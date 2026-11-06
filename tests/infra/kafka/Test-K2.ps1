[CmdletBinding()]
param([string]$RepositoryRoot)
$ErrorActionPreference = 'Stop'
if (-not $RepositoryRoot) { $RepositoryRoot = Join-Path $PSScriptRoot '../../..' }
$node = Get-Command node -ErrorAction SilentlyContinue
if ($node) { $executable = $node.Source }
elseif (Test-Path 'C:/Users/nc/tools/node/node.exe') { $executable = 'C:/Users/nc/tools/node/node.exe' }
else { throw 'Node.js is required for K2 executable static tests.' }
$previousRoot = $env:K2_QA_REPOSITORY_ROOT
try {
    $env:K2_QA_REPOSITORY_ROOT = [IO.Path]::GetFullPath($RepositoryRoot)
    & $executable --test (Join-Path $PSScriptRoot 'k2-static.test.mjs')
    $result = $LASTEXITCODE
} finally { $env:K2_QA_REPOSITORY_ROOT = $previousRoot }
exit $result
