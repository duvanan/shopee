param([string]$RepositoryRoot = (Join-Path $PSScriptRoot '../..'))
# Compatibility entry point: K1 security assertions now enforce runnable K2 invariants.
& (Join-Path $PSScriptRoot 'Validate-K2.ps1') -RepositoryRoot $RepositoryRoot
exit $LASTEXITCODE
