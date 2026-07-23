param([switch]$Force)

. (Join-Path $PSScriptRoot 'common.ps1')

$root = Get-ReleaseRoot
$configPath = Join-Path $root 'config\local.env'
$examplePath = Join-Path $root 'config\app.env.example'
$mysqld = Join-Path $root 'runtime\mysql\bin\mysqld.exe'
$mysql = Join-Path $root 'runtime\mysql\bin\mysql.exe'
$mysqlAdmin = Join-Path $root 'runtime\mysql\bin\mysqladmin.exe'
$dataDir = Join-Path $root 'data\mysql'

foreach ($required in @($mysqld, $mysql, $mysqlAdmin, $examplePath)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required release file is missing: $required"
    }
}

if ((Test-Path -LiteralPath (Join-Path $dataDir 'mysql')) -and (Test-Path -LiteralPath $configPath) -and -not $Force) {
    Write-Host 'The bundled database is already initialized.' -ForegroundColor Green
    exit 0
}

if ($Force -and (Test-Path -LiteralPath $dataDir)) {
    $resolvedRoot = [System.IO.Path]::GetFullPath($root).TrimEnd('\')
    $resolvedData = [System.IO.Path]::GetFullPath($dataDir)
    if (-not $resolvedData.StartsWith($resolvedRoot + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing to remove a data path outside the release directory.'
    }
    Remove-Item -LiteralPath $dataDir -Recurse -Force
}

$template = Read-EnvFile $examplePath
$template['MYSQL_PASSWORD'] = New-RandomSecret 32
$template['MYSQL_ROOT_PASSWORD'] = New-RandomSecret 36
$template['JWT_SECRET'] = New-RandomSecret 60
$port = [int]$template['MYSQL_PORT']

if (Test-TcpPort '127.0.0.1' $port 700) {
    throw "Port $port is already in use. Change MYSQL_PORT in config\app.env.example and retry."
}

$releaseDrive = Mount-ReleaseDrive $root
$mysqlProcess = $null
try {
    $mappedRoot = "${releaseDrive}\"
    $mappedMysqld = Join-Path $mappedRoot 'runtime\mysql\bin\mysqld.exe'
    $mappedMysql = Join-Path $mappedRoot 'runtime\mysql\bin\mysql.exe'
    $mappedMysqlAdmin = Join-Path $mappedRoot 'runtime\mysql\bin\mysqladmin.exe'
    $mysqlConfig = Write-MySqlConfig $mappedRoot $port
    Write-Host 'Initializing the bundled MySQL data directory...'
    & $mappedMysqld "--defaults-file=$mysqlConfig" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) {
        throw 'Bundled MySQL initialization failed.'
    }

    $logDir = Join-Path $root 'logs'
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    $mysqlProcess = Start-Process -FilePath $mappedMysqld -ArgumentList "--defaults-file=$mysqlConfig", '--console' -WorkingDirectory $mappedRoot -RedirectStandardOutput (Join-Path $logDir 'mysql-install.out.log') -RedirectStandardError (Join-Path $logDir 'mysql-install.err.log') -WindowStyle Hidden -PassThru

    if (-not (Wait-TcpPort '127.0.0.1' $port 60)) {
        Stop-Process -Id $mysqlProcess.Id -Force -ErrorAction SilentlyContinue
        throw 'Bundled MySQL failed to start. See logs\mysql-install.err.log.'
    }

    $schema = Get-Content -LiteralPath (Join-Path $root 'database\schema.sql') -Raw -Encoding UTF8
    $seed = Get-Content -LiteralPath (Join-Path $root 'database\seed.sql') -Raw -Encoding UTF8
    $schema | & $mappedMysql --protocol=TCP --host=127.0.0.1 "--port=$port" --user=root --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) {
        throw 'Database schema import failed.'
    }
    $seed | & $mappedMysql --protocol=TCP --host=127.0.0.1 "--port=$port" --user=root --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) {
        throw 'Demo data import failed.'
    }

    $appUser = $template['MYSQL_USERNAME']
    $appPassword = $template['MYSQL_PASSWORD']
    $rootPassword = $template['MYSQL_ROOT_PASSWORD']
    $database = $template['MYSQL_DATABASE']
    $securitySql = @"
CREATE USER IF NOT EXISTS '$appUser'@'127.0.0.1' IDENTIFIED BY '$appPassword';
ALTER USER '$appUser'@'127.0.0.1' IDENTIFIED BY '$appPassword';
GRANT ALL PRIVILEGES ON $database.* TO '$appUser'@'127.0.0.1';
ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';
FLUSH PRIVILEGES;
"@
    $securitySql | & $mappedMysql --protocol=TCP --host=127.0.0.1 "--port=$port" --user=root
    if ($LASTEXITCODE -ne 0) {
        throw 'Database account configuration failed.'
    }

    Write-EnvFile $configPath $template
    $credentialFile = New-MySqlCredentialFile 'root' $rootPassword $port
    try {
        & $mappedMysqlAdmin "--defaults-extra-file=$credentialFile" shutdown
    } finally {
        Remove-Item -LiteralPath $credentialFile -Force -ErrorAction SilentlyContinue
    }
    $mysqlProcess.WaitForExit(15000) | Out-Null
} finally {
    if ($mysqlProcess -and -not $mysqlProcess.HasExited) {
        Stop-Process -Id $mysqlProcess.Id -Force -ErrorAction SilentlyContinue
    }
    Dismount-ReleaseDrive $releaseDrive
}

Write-Host ''
Write-Host 'Installation and initialization completed.' -ForegroundColor Green
Write-Host 'Demo account: demo_student / 123456'
Write-Host 'Next: double-click the start BAT file in the release root.'
