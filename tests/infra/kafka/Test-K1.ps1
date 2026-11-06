[CmdletBinding()]
param(
    [string]$RepositoryRoot,
    [string]$ShellPath
)

# Independent K1 QA: no broker, container, image pull, topic or storage command is run.
$ErrorActionPreference = 'Stop'
if (-not $RepositoryRoot) { $RepositoryRoot = Join-Path $PSScriptRoot '../../..' }
$RepositoryRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$script:passed = 0
$script:failed = 0
$script:compose = $null
$script:fixtureRoot = $null

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Test-Case([string]$Id, [scriptblock]$Body) {
    try {
        & $Body
        $script:passed++
        Write-Host "PASS $Id"
    } catch {
        $script:failed++
        Write-Host "FAIL ${Id}: $($_.Exception.Message)"
    }
}

function Read-Properties([string]$Path) {
    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*(?:#|!|$)') { continue }
        if ($line -notmatch '^\s*([^=\s]+)\s*=\s*(.*?)\s*$') { throw "Unsupported property line in $Path" }
        $key = $Matches[1]
        if ($properties.ContainsKey($key)) { throw "Duplicate property $key in $Path" }
        $properties[$key] = $Matches[2]
    }
    return $properties
}

function New-Fixture {
    $destination = Join-Path $script:fixtureRoot ([Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path (Join-Path $destination 'infra') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $RepositoryRoot 'docker-compose.kafka.yml') -Destination $destination
    Copy-Item -LiteralPath (Join-Path $RepositoryRoot 'infra/kafka') -Destination (Join-Path $destination 'infra/kafka') -Recurse
    return $destination
}

function Replace-FixtureText([string]$Root, [string]$RelativePath, [string]$Pattern, [string]$Replacement) {
    $path = Join-Path $Root $RelativePath
    $original = [IO.File]::ReadAllText($path)
    $updated = [regex]::Replace($original, $Pattern, $Replacement)
    Assert-True ($updated -cne $original) "Mutation did not change $RelativePath"
    [IO.File]::WriteAllText($path, $updated, (New-Object Text.UTF8Encoding($false)))
}

function Invoke-Validator([string]$Root) {
    $validator = Join-Path $RepositoryRoot 'infra/kafka/Validate-K1.ps1'
    $executable = (Get-Process -Id $PID).Path
    $priorErrorAction = $ErrorActionPreference
    try {
        # A rejected mutation intentionally writes to stderr in Windows PowerShell.
        $ErrorActionPreference = 'Continue'
        $validatorOutput = & $executable -NoProfile -ExecutionPolicy Bypass -File $validator -RepositoryRoot $Root 2>&1
        $validatorExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $priorErrorAction }
    return @{ ExitCode = $validatorExit; Output = ($validatorOutput -join "`n") }
}

function Test-RejectedMutation([string]$Id, [scriptblock]$Mutation, [string]$ExpectedReason) {
    Test-Case $Id {
        $fixture = New-Fixture
        & $Mutation $fixture
        $result = Invoke-Validator $fixture
        Assert-True ($result.ExitCode -ne 0) 'Unsafe mutated foundation was accepted by Validate-K1.ps1'
        Assert-True ($result.Output -match [regex]::Escape($ExpectedReason)) "Mutation failed for an unexpected reason: $($result.Output)"
    }
}

Test-Case 'K1-AC05.compose-schema' {
    $composePath = Join-Path $RepositoryRoot 'docker-compose.kafka.yml'
    $rendered = & docker compose -f $composePath --profile kafka-foundation config --format json
    Assert-True ($LASTEXITCODE -eq 0) 'docker compose config failed'
    $script:compose = ($rendered -join "`n") | ConvertFrom-Json
    Assert-True ($null -ne $script:compose.services) 'Rendered Compose has no services'
}

Test-Case 'K1-AC01.three-static-combined-nodes' {
    Assert-True ($null -ne $script:compose) 'Compose schema prerequisite failed'
    $names = @($script:compose.services.PSObject.Properties.Name | Sort-Object)
    Assert-True (($names -join ',') -eq 'kafka-1,kafka-2,kafka-3') 'Expected exactly kafka-1, kafka-2, kafka-3'
    foreach ($id in 1..3) {
        $p = Read-Properties (Join-Path $RepositoryRoot "infra/kafka/config/kafka-$id.properties")
        Assert-True ($p['node.id'] -eq "$id") "Wrong node.id for kafka-$id"
        Assert-True ($p['process.roles'] -eq 'broker,controller') "Wrong combined roles for kafka-$id"
        Assert-True ($p['controller.quorum.voters'] -eq '1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093') 'Static voter topology mismatch'
        Assert-True (-not $p.ContainsKey('controller.quorum.bootstrap.servers')) 'Dynamic/static quorum modes mixed'
        Assert-True (-not $p.ContainsKey('zookeeper.connect')) 'ZooKeeper configured'
    }
}

Test-Case 'K1-AC02.separate-persistent-volumes-and-pinned-image' {
    Assert-True ($null -ne $script:compose) 'Compose schema prerequisite failed'
    $volumes = @()
    foreach ($id in 1..3) {
        $service = $script:compose.services."kafka-$id"
        Assert-True ($service.image -match '^apache/kafka:4\.1\.\d+@sha256:[a-f0-9]{64}$') 'Kafka image must pin patch and immutable digest'
        $data = @($service.volumes | Where-Object type -eq 'volume')
        Assert-True ($data.Count -eq 1) "Expected one data volume for kafka-$id"
        $volumes += $data[0].source
        $p = Read-Properties (Join-Path $RepositoryRoot "infra/kafka/config/kafka-$id.properties")
        Assert-True ($p['log.dirs'].StartsWith($data[0].target)) 'Broker log directory lies outside its persistent volume'
        Assert-True ($p['metadata.log.dir'].StartsWith($data[0].target)) 'Metadata directory lies outside its persistent volume'
    }
    Assert-True (@($volumes | Sort-Object -Unique).Count -eq 3) 'Broker data volume reused'
}

Test-Case 'K1-AC01.host-container-metadata-and-private-controller' {
    Assert-True ($null -ne $script:compose) 'Compose schema prerequisite failed'
    foreach ($id in 1..3) {
        $externalPort = 19092 + (($id - 1) * 10000)
        $service = $script:compose.services."kafka-$id"
        $ports = @($service.ports)
        Assert-True ($ports.Count -eq 1) 'Only host broker listener may be published'
        Assert-True ($ports[0].host_ip -eq '127.0.0.1') 'Host listener exposed beyond loopback'
        Assert-True ([int]$ports[0].published -eq $externalPort) 'Wrong per-node external host port'
        Assert-True ([int]$ports[0].target -eq 19092) 'Controller/internal listener published'
        $p = Read-Properties (Join-Path $RepositoryRoot "infra/kafka/config/kafka-$id.properties")
        Assert-True ($p['advertised.listeners'] -eq "INTERNAL://kafka-${id}:9092,EXTERNAL://localhost:$externalPort") 'Metadata paths inconsistent with Docker and host clients'
    }
}

Test-Case 'K1-AC04.ssl-mtls-default-deny-foundation' {
    foreach ($id in 1..3) {
        $p = Read-Properties (Join-Path $RepositoryRoot "infra/kafka/config/kafka-$id.properties")
        Assert-True ($p['listener.security.protocol.map'] -eq 'INTERNAL:SSL,EXTERNAL:SSL,CONTROLLER:SSL') 'Listener protocol must be SSL for every endpoint'
        Assert-True ($p['ssl.client.auth'] -eq 'required') 'Client certificate authentication is not required'
        Assert-True ($p['ssl.endpoint.identification.algorithm'] -eq 'https') 'Hostname verification disabled'
        Assert-True ($p['authorizer.class.name'] -eq 'org.apache.kafka.metadata.authorizer.StandardAuthorizer') 'KRaft authorizer missing'
        Assert-True ($p['allow.everyone.if.no.acl.found'] -eq 'false') 'Default allow enabled'
        Assert-True (-not $p.ContainsKey('super.users')) 'K1 must not provision superuser identities'
        Assert-True ($p['auto.create.topics.enable'] -eq 'false') 'Implicit topic provisioning enabled'
        Assert-True ($p['unclean.leader.election.enable'] -eq 'false') 'Unsafe leader election enabled'
        Assert-True ($p['default.replication.factor'] -eq '3' -and $p['min.insync.replicas'] -eq '2') 'Unsafe replication defaults'
    }
}

Test-Case 'K1-AC03.topic-declaration-and-producer-acks' {
    $manifest = Get-Content -LiteralPath (Join-Path $RepositoryRoot 'infra/kafka/topics.json') -Raw | ConvertFrom-Json
    Assert-True ($manifest.activation -eq 'blocked-until-K2') 'Topic provisioning not blocked'
    Assert-True ($manifest.retentionApproval -eq 'pending-KN-D03') 'Unapproved retention policy enabled'
    Assert-True (@($manifest.topics).Count -eq 1) 'Unexpected future-domain topics declared'
    $topic = $manifest.topics[0]
    Assert-True ($topic.name -eq 'notification.auth.requested.v1') 'Unexpected auth topic'
    Assert-True ($topic.partitions -eq 6 -and $topic.replicationFactor -eq 3 -and $topic.config.'min.insync.replicas' -eq 2) 'Auth topic durability declaration differs from K1 approval'
    Assert-True ($null -eq $topic.retentionMs) 'Retention must remain pending'
    foreach ($network in @('host', 'container')) {
        $p = Read-Properties (Join-Path $RepositoryRoot "infra/kafka/config/producer-$network.properties")
        Assert-True ($p['acks'] -eq 'all' -and $p['enable.idempotence'] -eq 'true') 'Producer durability/idempotence weakened'
        Assert-True ($p['security.protocol'] -eq 'SSL' -and $p['ssl.endpoint.identification.algorithm'] -eq 'https') 'Producer TLS or hostname verification weakened'
    }
}

Test-Case 'K1-AC04.effective-entrypoint-cannot-start-kafka' {
    Assert-True ($null -ne $script:compose) 'Compose schema prerequisite failed'
    foreach ($id in 1..3) {
        $service = $script:compose.services."kafka-$id"
        Assert-True (($service.entrypoint -join ' ') -eq '/bin/sh /mnt/kafka-foundation/k1-block-start.sh') 'Service entrypoint does not exclusively execute K1 blocker'
        Assert-True (@($service.command).Count -eq 0) 'Unexpected startup command'
        Assert-True ($service.restart -eq 'no') 'K1 blocker configured for restart loop'
        $gateMount = @($service.volumes | Where-Object { $_.target -eq '/mnt/kafka-foundation/k1-block-start.sh' })
        Assert-True ($gateMount.Count -eq 1 -and $gateMount[0].read_only -eq $true) 'Gate script not mounted read-only'
    }
}

Test-Case 'K1-AC05.validator-accepts-baseline' {
    $result = Invoke-Validator $RepositoryRoot
    Assert-True ($result.ExitCode -eq 0) "Baseline validator failed: $($result.Output)"
}

$temporaryBase = [IO.Path]::GetTempPath()
$script:fixtureRoot = Join-Path $temporaryBase ('shopee-k1-qa-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $script:fixtureRoot | Out-Null
try {
    Test-Case 'K1-AC05.validator-accepts-copied-fixture' {
        $result = Invoke-Validator (New-Fixture)
        Assert-True ($result.ExitCode -eq 0) "Unmodified temporary fixture failed: $($result.Output)"
    }
    Test-RejectedMutation 'K1-AC05.reject-duplicate-node-id' { param($r) Replace-FixtureText $r 'infra/kafka/config/kafka-2.properties' '(?m)^node\.id=2[ \t]*\r?$' 'node.id=1' } 'node.id must be 2'
    Test-RejectedMutation 'K1-AC05.reject-quorum-drift' { param($r) Replace-FixtureText $r 'infra/kafka/config/kafka-1.properties' '3@kafka-3:9093' '3@unknown:9093' } 'controller.quorum.voters must be'
    Test-RejectedMutation 'K1-AC05.reject-plaintext-listener' { param($r) Replace-FixtureText $r 'infra/kafka/config/kafka-1.properties' 'INTERNAL:SSL' 'INTERNAL:PLAINTEXT' } 'listener.security.protocol.map must be'
    Test-RejectedMutation 'K1-AC05.reject-optional-client-auth' { param($r) Replace-FixtureText $r 'infra/kafka/config/kafka-1.properties' '(?m)^ssl\.client\.auth=required[ \t]*\r?$' 'ssl.client.auth=requested' } 'ssl.client.auth must be required'
    Test-RejectedMutation 'K1-AC05.reject-disabled-hostname-verification' { param($r) Replace-FixtureText $r 'infra/kafka/config/producer-host.properties' '(?m)^ssl\.endpoint\.identification\.algorithm=https[ \t]*\r?$' 'ssl.endpoint.identification.algorithm=' } 'ssl.endpoint.identification.algorithm must be https'
    Test-RejectedMutation 'K1-AC05.reject-default-allow' { param($r) Replace-FixtureText $r 'infra/kafka/config/kafka-1.properties' '(?m)^allow\.everyone\.if\.no\.acl\.found=false[ \t]*\r?$' 'allow.everyone.if.no.acl.found=true' } 'allow.everyone.if.no.acl.found must be false'
    Test-RejectedMutation 'K1-AC05.reject-weak-producer-acks' { param($r) Replace-FixtureText $r 'infra/kafka/config/producer-host.properties' '(?m)^acks=all[ \t]*\r?$' 'acks=1' } 'acks must be all'
    Test-RejectedMutation 'K1-AC05.reject-topic-replica-loss' { param($r) Replace-FixtureText $r 'infra/kafka/topics.json' '("replicationFactor"\s*:\s*)3' '${1}1' } 'auth topic must declare 6 partitions and RF3'
    Test-RejectedMutation 'K1-AC05.reject-topic-partition-drift' { param($r) Replace-FixtureText $r 'infra/kafka/topics.json' '("partitions"\s*:\s*)6' '${1}1' } 'auth topic must declare 6 partitions and RF3'
    Test-RejectedMutation 'K1-AC05.reject-weakened-topic-min-isr' { param($r) Replace-FixtureText $r 'infra/kafka/topics.json' '("min.insync.replicas"\s*:\s*)2' '${1}1' } 'auth topic must require minISR2'
    Test-RejectedMutation 'K1-AC05.reject-shared-data-volume' { param($r) Replace-FixtureText $r 'docker-compose.kafka.yml' 'source: kafka_2_data' 'source: kafka_1_data' } 'three independent data volumes required'
    Test-RejectedMutation 'K1-AC05.reject-activation-unlock' { param($r) Replace-FixtureText $r 'infra/kafka/k1-block-start.sh' '(?m)^exit 78[ \t]*\r?$' 'exit 0' } 'gate must unconditionally exit 78'
    Test-RejectedMutation 'K1-AC05.reject-gate-bypass-command' { param($r) Replace-FixtureText $r 'docker-compose.kafka.yml' '/mnt/kafka-foundation/k1-block-start.sh' '/opt/kafka/bin/kafka-server-start.sh' } 'entrypoint must invoke only the K1 gate'
    Test-RejectedMutation 'K1-AC05.reject-host-public-bind' { param($r) Replace-FixtureText $r 'docker-compose.kafka.yml' '127\.0\.0\.1' '0.0.0.0' } 'external port must bind node-specific loopback only'
    Test-RejectedMutation 'K1-AC05.reject-controller-host-port' { param($r) Replace-FixtureText $r 'docker-compose.kafka.yml' '19092:19092' '19092:9093' } 'external port must bind node-specific loopback only'
} finally {
    # Delete only this generated temporary fixture tree, after resolving/checking its boundary.
    $resolvedFixture = (Resolve-Path -LiteralPath $script:fixtureRoot).Path
    $resolvedTemporaryBase = [IO.Path]::GetFullPath($temporaryBase).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    Assert-True ($resolvedFixture.StartsWith($resolvedTemporaryBase, [StringComparison]::OrdinalIgnoreCase)) 'Refusing fixture cleanup outside temporary directory'
    Assert-True ((Split-Path -Leaf $resolvedFixture) -match '^shopee-k1-qa-[a-f0-9]{32}$') 'Refusing cleanup of unexpected fixture path'
    Remove-Item -LiteralPath $resolvedFixture -Recurse -Force
}

if (-not $ShellPath) {
    $gitShell = 'C:/Program Files/Git/bin/bash.exe'
    if (Test-Path -LiteralPath $gitShell) { $ShellPath = $gitShell }
    else {
        $shellCommand = Get-Command sh -ErrorAction SilentlyContinue
        if ($shellCommand) { $ShellPath = $shellCommand.Source }
    }
}
Test-Case 'K1-AC04.gate-rejects-environment-and-command-arguments' {
    Assert-True ([bool]$ShellPath) 'POSIX shell missing; provide -ShellPath to execute the blocker test'
    $priorUnlock = $env:KAFKA_K1_ACTIVATION
    $priorCommand = $env:KAFKA_COMMAND
    try {
        $env:KAFKA_K1_ACTIVATION = 'enabled'
        $env:KAFKA_COMMAND = '/opt/kafka/bin/kafka-server-start.sh'
        $gatePath = (Join-Path $RepositoryRoot 'infra/kafka/k1-block-start.sh').Replace('\', '/')
        $priorErrorAction = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $output = & $ShellPath $gatePath '/opt/kafka/bin/kafka-server-start.sh' '--override' 'listeners=PLAINTEXT://:9092' 2>&1
            $gateExit = $LASTEXITCODE
        } finally { $ErrorActionPreference = $priorErrorAction }
        Assert-True ($gateExit -eq 78) 'Blocker did not exit 78 when supplied hostile environment/arguments'
        Assert-True (($output -join "`n") -match 'K2') 'Blocker did not explain K2 activation dependency'
    } finally {
        $env:KAFKA_K1_ACTIVATION = $priorUnlock
        $env:KAFKA_COMMAND = $priorCommand
    }
}

Write-Host "K1 independent QA: $script:passed passed; $script:failed failed. No Kafka cluster was started."
if ($script:failed -gt 0) { exit 1 }
exit 0
