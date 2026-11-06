[CmdletBinding()]
param([string]$RepositoryRoot, [string]$ShellPath)
if (-not $RepositoryRoot) { $RepositoryRoot = Join-Path $PSScriptRoot '../../..' }
# Compatibility entry point: K2 replaces the exit-78 gate with authenticated startup.
# The K2 suite retains topology, persistence, TLS, ACL and retention policy checks.
& (Join-Path $PSScriptRoot 'Test-K2.ps1') -RepositoryRoot $RepositoryRoot
exit $LASTEXITCODE
