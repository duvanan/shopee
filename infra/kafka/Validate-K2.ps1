param([string]$RepositoryRoot = (Join-Path $PSScriptRoot '../..'))
$ErrorActionPreference = 'Stop'
$node = Get-Command node -ErrorAction SilentlyContinue
if ($node) { $executable = $node.Source }
elseif (Test-Path -LiteralPath 'C:/Users/nc/tools/node/node.exe') { $executable = 'C:/Users/nc/tools/node/node.exe' }
else { throw 'Node.js 20+ is required for K2 executable static validation' }
& $executable (Join-Path $PSScriptRoot 'k2.mjs') validate --repository-root $RepositoryRoot
exit $LASTEXITCODE
