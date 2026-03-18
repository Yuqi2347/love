param(
  [string]$EnvFile = ".env",
  [switch]$SkipRun
)

$ErrorActionPreference = "Stop"

function Load-EnvFile {
  param([string]$Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    throw "Env file not found: $Path"
  }

  $count = 0
  Get-Content -LiteralPath $Path | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith("#")) { return }

    $parts = $line -split "=", 2
    if ($parts.Count -lt 2) { return }

    $name = $parts[0].Trim()
    $value = $parts[1]
    if ([string]::IsNullOrWhiteSpace($name)) { return }

    [Environment]::SetEnvironmentVariable($name, $value, "Process")
    $count++
  }

  return $count
}

function Resolve-EnvPath {
  param([string]$Preferred)

  if (Test-Path -LiteralPath $Preferred) {
    return $Preferred
  }

  if ($Preferred -eq ".env" -and (Test-Path -LiteralPath ".env.example")) {
    Write-Host "No .env found, fallback to .env.example (recommended: copy .env.example to .env and fill real secrets)." -ForegroundColor Yellow
    return ".env.example"
  }

  throw "Env file not found: $Preferred"
}

function Start-Backend {
  if (Get-Command mvn -ErrorAction SilentlyContinue) {
    mvn spring-boot:run
    return
  }

  if (Test-Path -LiteralPath ".\mvnw.cmd") {
    .\mvnw.cmd spring-boot:run
    return
  }

  throw "Neither mvn nor mvnw.cmd found. Install Maven or add Maven Wrapper."
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -LiteralPath $scriptDir

$resolvedEnvFile = Resolve-EnvPath -Preferred $EnvFile
$loaded = Load-EnvFile -Path $resolvedEnvFile
Write-Host "Loaded $loaded environment variables from $resolvedEnvFile" -ForegroundColor Green

if ($SkipRun) {
  Write-Host "Skip backend run (-SkipRun)." -ForegroundColor Cyan
  exit 0
}

Start-Backend