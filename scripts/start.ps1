param(
  [switch]$Build
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker is not installed or not in PATH. Install Docker Desktop, then rerun scripts/start.ps1."
}

$args = @("compose", "up", "-d")
if ($Build) {
  $args += "--build"
}

docker @args

Write-Host "EduAgent Studio is starting."
Write-Host "Frontend: http://127.0.0.1:5173"
Write-Host "Backend health: http://127.0.0.1:8080/api/health"
Write-Host "AI health: http://127.0.0.1:8000/ai/health"
