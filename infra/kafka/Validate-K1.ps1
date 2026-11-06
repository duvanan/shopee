param(
    [string]$RepositoryRoot = (Join-Path $PSScriptRoot '../..')
)

# Read-only static validation. No daemon, image pull, container, storage or topic writes.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$RepositoryRoot = [IO.Path]::GetFullPath($RepositoryRoot)

function Assert-K1([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "K1 validation failed: $Message" }
}

function Read-Properties([string]$Path) {
    $result = @{}
    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        $value = $line.Trim()
        if ($value -eq '' -or $value.StartsWith('#')) { continue }
        Assert-K1 ($value -match '^([^=\s]+)=(.*)$') "unsupported property syntax in $Path"
        $key = $Matches[1]
        Assert-K1 (-not $result.ContainsKey($key)) "duplicate property $key in $Path"
        $result[$key] = $Matches[2]
    }
    return $result
}

function Require-Property($Properties, [string]$Key, [string]$Expected) {
    Assert-K1 ($Properties.ContainsKey($Key) -and $Properties[$Key] -ceq $Expected) "$Key must be $Expected"
}

$composePath = Join-Path $RepositoryRoot 'docker-compose.kafka.yml'
$rendered = & docker compose -f $composePath --profile kafka-foundation config --format json
Assert-K1 ($LASTEXITCODE -eq 0) 'docker compose config must succeed'
$compose = ($rendered -join "`n") | ConvertFrom-Json
$services = @($compose.services.PSObject.Properties)
Assert-K1 ($services.Count -eq 3) 'exactly three Kafka services required'
Assert-K1 ((@($services.Name | Sort-Object) -join ',') -ceq 'kafka-1,kafka-2,kafka-3') 'unexpected service'
Assert-K1 (@($compose.volumes.PSObject.Properties).Count -eq 3) 'three independent data volumes required'
Assert-K1 ($compose.networks.'kafka-local'.internal -eq $true) 'Kafka network must be internal'

$gatePath = Join-Path $RepositoryRoot 'infra/kafka/k1-block-start.sh'
$gateBytes = [IO.File]::ReadAllBytes($gatePath)
Assert-K1 (-not ($gateBytes -contains 13)) 'gate script must use LF, not CRLF'
$gateLines = @([IO.File]::ReadAllLines($gatePath) | Where-Object { $_.Trim() -ne '' -and -not $_.Trim().StartsWith('#') })
Assert-K1 ($gateLines.Count -eq 2) 'gate must contain only a fixed diagnostic and unconditional exit'
Assert-K1 ($gateLines[0] -ceq "printf '%s\n' 'K1_ACTIVATION_BLOCKED: complete reviewed K2 TLS/mTLS and ACL provisioning before starting Kafka.' >&2") 'gate diagnostic must be literal, without argument/environment evaluation'
Assert-K1 ($gateLines[1] -ceq 'exit 78') 'gate must unconditionally exit 78'

$image = 'apache/kafka:4.1.2@sha256:5cc2a2fd93fa2687b44015eee04fb2c3edd9e526bd64bf8bec5ff1e268772e0e'
$voters = '1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093'
$usedVolumes = @()
foreach ($node in 1..3) {
    $service = $compose.services."kafka-$node"
    Assert-K1 ($service.image -ceq $image) 'official Kafka image version/digest must stay pinned'
    Assert-K1 ((@($service.entrypoint) -join ',') -ceq '/bin/sh,/mnt/kafka-foundation/k1-block-start.sh') 'entrypoint must invoke only the K1 gate'
    Assert-K1 (@($service.command).Count -eq 0) 'no broker command may be configured'
    Assert-K1 ($service.restart -ceq 'no') 'blocked service must not restart'
    Assert-K1 ((@($service.profiles) -join ',') -ceq 'kafka-foundation') 'service must remain opt-in'
    Assert-K1 ($service.user -ceq '1000:1000') 'non-root identity required'
    Assert-K1 ($service.mem_limit -eq 2147483648) 'resource hypothesis must remain 2GiB per broker'
    Assert-K1 ([double]$service.cpus -eq 1) 'resource hypothesis must remain one CPU per broker'
    Assert-K1 ($service.environment.KAFKA_HEAP_OPTS -ceq '-Xms1g -Xmx1g') 'heap hypothesis must remain 1GiB'
    Assert-K1 (@($service.environment.PSObject.Properties).Count -eq 1) 'no env activation/credential/config overrides in K1'
    Assert-K1 ($service.hostname -ceq "kafka-$node") 'node hostname must match voters'
    Assert-K1 (@($service.ports).Count -eq 1) 'only the external client port may be published'
    $port = @($service.ports)[0]
    Assert-K1 ($port.host_ip -ceq '127.0.0.1' -and $port.target -eq 19092 -and [string]$port.published -ceq "${node}9092") 'external port must bind node-specific loopback only'
    Assert-K1 ((@($service.networks.PSObject.Properties.Name) -join ',') -ceq 'kafka-local') 'only scoped Kafka network allowed'
    Assert-K1 (@($service.volumes).Count -eq 3) 'only data, server config and gate mounts expected'
    $data = @($service.volumes | Where-Object { $_.target -ceq '/var/lib/kafka/data' })
    Assert-K1 ($data.Count -eq 1 -and $data[0].type -ceq 'volume' -and $data[0].source -ceq "kafka_${node}_data") 'node must own its distinct volume'
    $usedVolumes += $data[0].source
    foreach ($mountSpec in @(
        @{ Target='/mnt/kafka-foundation/server.properties'; Relative="infra/kafka/config/kafka-$node.properties" },
        @{ Target='/mnt/kafka-foundation/k1-block-start.sh'; Relative='infra/kafka/k1-block-start.sh' }
    )) {
        $mounts = @($service.volumes | Where-Object { $_.target -ceq $mountSpec.Target })
        Assert-K1 ($mounts.Count -eq 1) "missing mounted $($mountSpec.Relative)"
        $mount = $mounts[0]
        # Compose omits explicit false in JSON; missing is false in its normalized bind model.
        $createHostPath = $mount.bind.PSObject.Properties['create_host_path']
        Assert-K1 ($mount.type -ceq 'bind' -and $mount.read_only -eq $true -and ($null -eq $createHostPath -or $createHostPath.Value -eq $false)) 'bind mounts must be existing files, read-only'
        Assert-K1 ([IO.Path]::GetFullPath($mount.source) -ceq [IO.Path]::GetFullPath((Join-Path $RepositoryRoot $mountSpec.Relative))) 'mounted source must match reviewed file'
        Assert-K1 (Test-Path -LiteralPath $mount.source -PathType Leaf) 'bind source must exist'
    }
    $properties = Read-Properties (Join-Path $RepositoryRoot "infra/kafka/config/kafka-$node.properties")
    $expected = @{
        'node.id'=[string]$node; 'process.roles'='broker,controller'; 'controller.quorum.voters'=$voters
        'controller.listener.names'='CONTROLLER'
        'listeners'='INTERNAL://0.0.0.0:9092,EXTERNAL://0.0.0.0:19092,CONTROLLER://0.0.0.0:9093'
        'advertised.listeners'="INTERNAL://kafka-${node}:9092,EXTERNAL://localhost:${node}9092"
        'listener.security.protocol.map'='INTERNAL:SSL,EXTERNAL:SSL,CONTROLLER:SSL'
        'inter.broker.listener.name'='INTERNAL'; 'ssl.client.auth'='required'
        'listener.name.internal.ssl.client.auth'='required'; 'listener.name.external.ssl.client.auth'='required'
        'listener.name.controller.ssl.client.auth'='required'; 'ssl.endpoint.identification.algorithm'='https'
        'ssl.enabled.protocols'='TLSv1.3,TLSv1.2'
        'authorizer.class.name'='org.apache.kafka.metadata.authorizer.StandardAuthorizer'
        'allow.everyone.if.no.acl.found'='false'; 'auto.create.topics.enable'='false'
        'unclean.leader.election.enable'='false'; 'default.replication.factor'='3'; 'min.insync.replicas'='2'
        'num.partitions'='6'; 'offsets.topic.replication.factor'='3'; 'transaction.state.log.replication.factor'='3'
        'transaction.state.log.min.isr'='2'; 'log.dirs'='/var/lib/kafka/data/logs'
        'metadata.log.dir'='/var/lib/kafka/data/metadata'
    }
    foreach ($key in $expected.Keys) { Require-Property $properties $key $expected[$key] }
    Assert-K1 ($properties.Count -eq $expected.Count) 'unreviewed broker properties (credentials/ZooKeeper/dynamic quorum/ACL overrides) forbidden'
}
Assert-K1 (@($usedVolumes | Select-Object -Unique).Count -eq 3) 'volumes cannot be shared by nodes'

$topics = Get-Content -LiteralPath (Join-Path $RepositoryRoot 'infra/kafka/topics.json') -Raw | ConvertFrom-Json
Assert-K1 ($topics.schemaVersion -eq 1 -and $topics.activation -ceq 'blocked-until-K2') 'topic manifest must remain non-executing K1 declaration'
Assert-K1 ($topics.retentionApproval -ceq 'pending-KN-D03') 'retention must remain explicitly pending'
Assert-K1 (@($topics.topics).Count -eq 1) 'only auth topic belongs to K1'
$topic = $topics.topics[0]
Assert-K1 ($topic.name -ceq 'notification.auth.requested.v1' -and $topic.partitions -eq 6 -and $topic.replicationFactor -eq 3) 'auth topic must declare 6 partitions and RF3'
Assert-K1 ($topic.config.'min.insync.replicas' -eq 2) 'auth topic must require minISR2'
Assert-K1 (@($topic.config.PSObject.Properties).Count -eq 1 -and $null -eq $topic.retentionMs) 'retention/cleanup configuration is not approved by K1'

foreach ($kind in @('host','container')) {
    $producer = Read-Properties (Join-Path $RepositoryRoot "infra/kafka/config/producer-$kind.properties")
    Require-Property $producer 'acks' 'all'
    Require-Property $producer 'enable.idempotence' 'true'
    Require-Property $producer 'retries' '2147483647'
    Require-Property $producer 'max.in.flight.requests.per.connection' '5'
    Require-Property $producer 'security.protocol' 'SSL'
    Require-Property $producer 'ssl.endpoint.identification.algorithm' 'https'
    $bootstrap = if ($kind -eq 'host') { 'localhost:19092,localhost:29092,localhost:39092' } else { 'kafka-1:9092,kafka-2:9092,kafka-3:9092' }
    Require-Property $producer 'bootstrap.servers' $bootstrap
    Assert-K1 ($producer.Count -eq 7) 'producer secrets or unreviewed overrides must not be committed'
}

Write-Output 'PASS K1-AC01..05: rendered topology, volumes, SSL/default-deny intent, topic/producer declarations, unconditional activation gate. Static evidence only; cluster not started.'
