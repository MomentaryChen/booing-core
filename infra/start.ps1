param(
  [ValidateSet("all", "frontend")]
  [string]$Mode = "all",
  [string]$ComposeFile = "",
  [string]$ProjectName = "booking-core",
  [switch]$Rebuild = $true,
  [switch]$Detach = $true,
  [switch]$ResetMySqlData = $false
)

$ErrorActionPreference = "Stop"

$scriptRoot = $PSScriptRoot
$defaultComposeFile = if ($Mode -eq "frontend") { "docker-compose.frontend.yml" } else { "docker-compose.yml" }
$resolvedComposeFile = if ([string]::IsNullOrWhiteSpace($ComposeFile)) { $defaultComposeFile } else { $ComposeFile }
$composePath = Join-Path $scriptRoot $resolvedComposeFile
$envPath = Join-Path $scriptRoot ".env"
$envExamplePath = Join-Path $scriptRoot ".env.example"

if ((-not (Test-Path $envPath)) -and (Test-Path $envExamplePath)) {
  Copy-Item -Path $envExamplePath -Destination $envPath
  Write-Host "No .env found. Created from .env.example" -ForegroundColor Yellow
}

if (-not (Test-Path $composePath)) {
  throw "docker compose file not found: $composePath"
}

Write-Host "== booking-core ==" -ForegroundColor Cyan
Write-Host "Mode: $Mode"
Write-Host "Compose: $composePath"
Write-Host "Project: $ProjectName"

if ($ResetMySqlData) {
  if ($Mode -eq "frontend") {
    Write-Host "Skip MySQL reset in frontend-only mode." -ForegroundColor Yellow
  } else {
    $downArgs = @("compose", "-p", $ProjectName, "-f", $composePath, "down")
    Write-Host "Running: docker $($downArgs -join ' ')"
    $downProc = Start-Process -FilePath "docker" -ArgumentList $downArgs -NoNewWindow -Wait -PassThru
    if ($downProc.ExitCode -ne 0) {
      throw "docker compose down failed with exit code $($downProc.ExitCode)"
    }

    $mysqlVolumeName = "$ProjectName`_mysql-data"
    $volumeRmArgs = @("volume", "rm", "-f", $mysqlVolumeName)
    Write-Host "Running: docker $($volumeRmArgs -join ' ')"
    $volumeRmProc = Start-Process -FilePath "docker" -ArgumentList $volumeRmArgs -NoNewWindow -Wait -PassThru
    if ($volumeRmProc.ExitCode -ne 0) {
      throw "docker volume rm failed with exit code $($volumeRmProc.ExitCode)"
    }

    Write-Host "MySQL data reset complete: $mysqlVolumeName" -ForegroundColor Yellow
  }
}

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

