[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$SecretsDir, [switch]$IncludeFaultTests)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$node = Get-Command node -ErrorAction SilentlyContinue
$executable = if ($node) { $node.Source } else { 'C:/Users/nc/tools/node/node.exe' }
$cli = Join-Path $root 'infra/kafka/k2.mjs'
$ErrorActionPreference = 'Continue'
$probe = & docker info --format '{{.ServerVersion}}' 2>&1
$probeExit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($probeExit -ne 0 -or ($probe -join "`n") -match 'error|cannot|not running' -or -not ($probe -join '').Trim()) {
    Write-Host 'NOT RUN K2 runtime: Docker daemon unavailable.'
    exit 2
}
function Invoke-K2([string]$Command) {
    & $executable $cli $Command --secrets-dir $SecretsDir
    if ($LASTEXITCODE -ne 0) { throw "K2 runtime command failed: $Command" }
}
$oldSecrets = $env:KAFKA_K2_SECRETS_DIR
try {
    $env:KAFKA_K2_SECRETS_DIR = $SecretsDir
    Invoke-K2 'ready'
    Invoke-K2 'negative-tls'
    Invoke-K2 'negative-acls'
    Invoke-K2 'restart-check'
    if ($IncludeFaultTests) {
        $compose = Join-Path $root 'docker-compose.kafka.yml'
        try {
            & docker compose -f $compose --profile kafka-foundation stop kafka-3
            if ($LASTEXITCODE -ne 0) { throw 'Could not stop node3 for fault test' }
            # Full ready requires all3 endpoints. Here authenticate directly to the
            # surviving node and require a controller leader from the pinned CLI.
            $adminDir = Join-Path $SecretsDir 'clients/bootstrap-admin'
            $image = 'apache/kafka:4.1.2@sha256:5cc2a2fd93fa2687b44015eee04fb2c3edd9e526bd64bf8bec5ff1e268772e0e'
            $quorum = & docker run --rm --network shopee-kafka-local_kafka-local --user 1000:1000 --cap-drop ALL --security-opt no-new-privileges:true --mount "type=bind,source=$adminDir,target=/run/client,readonly" --entrypoint /opt/kafka/bin/kafka-metadata-quorum.sh $image --bootstrap-server kafka-1:9092 --command-config /run/client/container.properties describe --status
            if ($LASTEXITCODE -ne 0 -or ($quorum -join "`n") -notmatch 'LeaderId:\s+[12]\b') { throw 'Single-node outage has no authenticated surviving leader' }
            & docker compose -f $compose --profile kafka-foundation stop kafka-2
            if ($LASTEXITCODE -ne 0) { throw 'Could not stop node2 for majority-loss test' }
            # This checks rejection of incomplete topology only. It does not prove
            # majority-loss write/commit behavior; K4/#29 retain durability tests.
            & $executable $cli ready --secrets-dir $SecretsDir
            if ($LASTEXITCODE -eq 0) { throw 'Incomplete-topology readiness unexpectedly succeeded' }
            Write-Host 'PASS readiness rejects incomplete topology; majority-loss commit/durability acceptance NOT TESTED.'
        } finally {
            & docker compose -f $compose --profile kafka-foundation start kafka-2 kafka-3
            if ($LASTEXITCODE -ne 0) { throw 'Fault-test recovery failed; inspect node2/3 without deleting volumes' }
        }
        Invoke-K2 'ready'
    }
    Write-Host 'PASS K2 runtime checks; no topics created or records produced.'
} finally { $env:KAFKA_K2_SECRETS_DIR = $oldSecrets }
