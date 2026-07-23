. (Join-Path $PSScriptRoot 'common.ps1')

$root = Get-ReleaseRoot
$statePath = Join-Path $root 'run\processes.json'
$configPath = Join-Path $root 'config\local.env'
$state = $null

if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -LiteralPath $statePath -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($processId in @($state.backend, $state.ai)) {
        if ($processId -and (Get-Process -Id $processId -ErrorAction SilentlyContinue)) {
            Stop-Process -Id $processId -Force
        }
    }
}

if (Test-Path -LiteralPath $configPath) {
    $config = Read-EnvFile $configPath
    $credentialFile = New-MySqlCredentialFile 'root' $config['MYSQL_ROOT_PASSWORD'] ([int]$config['MYSQL_PORT'])
    try {
        $mysqlAdmin = Join-Path $root 'runtime\mysql\bin\mysqladmin.exe'
        & $mysqlAdmin "--defaults-extra-file=$credentialFile" shutdown 2>$null
    } catch {
        if (Test-Path -LiteralPath $statePath) {
            $state = Get-Content -LiteralPath $statePath -Raw -Encoding UTF8 | ConvertFrom-Json
            if ($state.mysql -and (Get-Process -Id $state.mysql -ErrorAction SilentlyContinue)) {
                Stop-Process -Id $state.mysql -Force
            }
        }
    } finally {
        Remove-Item -LiteralPath $credentialFile -Force -ErrorAction SilentlyContinue
    }
}

Remove-Item -LiteralPath $statePath -Force -ErrorAction SilentlyContinue
if ($state -and $state.releaseDrive) {
    Dismount-ReleaseDrive $state.releaseDrive
}
Write-Host 'EduAgent Studio stopped. Learning data remains in the data directory.' -ForegroundColor Green
