param(
  [string]$Database = "eduagent_studio"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker is not installed or not in PATH."
}

docker compose up -d mysql
Write-Host "Waiting for MySQL container..."
Start-Sleep -Seconds 12

$password = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { "eduagent_root_password" }
docker compose exec -T mysql mysql -uroot -p"$password" $Database -e "SELECT COUNT(*) AS user_count FROM sys_user;"
Write-Host "Database initialization checked. schema.sql and seed.sql are mounted under docker-entrypoint-initdb.d on first container creation."
