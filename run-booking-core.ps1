param(
  [switch]$Rebuild = $true,
  [switch]$Detach = $true
)

$ErrorActionPreference = "Stop"

$infraScript = Join-Path $PSScriptRoot "infra\\run-docker-compose.ps1"
if (-not (Test-Path $infraScript)) {
  throw "Missing infra script: $infraScript"
}

& $infraScript -Rebuild:$Rebuild -Detach:$Detach

