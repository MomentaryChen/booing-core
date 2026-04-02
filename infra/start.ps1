param(
  [string]$ComposeFile = "docker-compose.yml",
  [string]$ProjectName = "booking-core",
  [switch]$Rebuild = $true,
  [switch]$Detach = $true
)

$ErrorActionPreference = "Stop"

$scriptRoot = $PSScriptRoot
$composePath = Join-Path $scriptRoot $ComposeFile

if (-not (Test-Path $composePath)) {
  throw "docker compose file not found: $composePath"
}

Write-Host "== booking-core ==" -ForegroundColor Cyan
Write-Host "Compose: $composePath"
Write-Host "Project: $ProjectName"

$composeArgs = @("-p", $ProjectName, "-f", $composePath, "up")

if ($Rebuild) {
  $composeArgs += "--build"
}

if ($Detach) {
  $composeArgs += "-d"
}

Write-Host "Running: docker compose $($composeArgs -join ' ')"
$dockerArgs = @("compose") + $composeArgs
$proc = Start-Process -FilePath "docker" -ArgumentList $dockerArgs -NoNewWindow -Wait -PassThru

if ($proc.ExitCode -ne 0) {
  throw "docker compose failed with exit code $($proc.ExitCode)"
}

Write-Host "Done. Use: docker compose -p `"$ProjectName`" -f `"$composePath`" ps" -ForegroundColor Green

