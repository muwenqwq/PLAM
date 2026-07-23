Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

function Normalize-ProcessPathEnvironment {
    $pathValue = $env:PATH
    Remove-Item -LiteralPath Env:Path -ErrorAction SilentlyContinue
    $env:PATH = $pathValue
}

Normalize-ProcessPathEnvironment

function Get-ReleaseRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}

function Mount-ReleaseDrive([string]$Root) {
    $subst = Join-Path $env:SystemRoot 'System32\subst.exe'
    foreach ($letter in @('Z', 'Y', 'X', 'W', 'V', 'U', 'T')) {
        $drive = "${letter}:"
        if (Test-Path -LiteralPath "${drive}\") {
            continue
        }
        & $subst $drive $Root | Out-Null
        if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath "${drive}\")) {
            return $drive
        }
    }
    throw 'Unable to create a temporary drive mapping for the release directory.'
}

function Dismount-ReleaseDrive([string]$Drive) {
    if (-not $Drive) {
        return
    }
    $subst = Join-Path $env:SystemRoot 'System32\subst.exe'
    & $subst $Drive /D | Out-Null
}

function Read-EnvFile([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) {
            continue
        }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) {
            continue
        }
        $values[$trimmed.Substring(0, $separator).Trim()] = $trimmed.Substring($separator + 1)
    }
    return $values
}

function Write-EnvFile([string]$Path, [hashtable]$Values) {
    $order = @(
        'MYSQL_PORT', 'MYSQL_DATABASE', 'MYSQL_USERNAME', 'MYSQL_PASSWORD',
        'MYSQL_ROOT_PASSWORD', 'BACKEND_PORT', 'AI_PORT', 'JWT_SECRET'
    )
    $lines = foreach ($name in $order) {
        "$name=$($Values[$name])"
    }
    [System.IO.File]::WriteAllLines($Path, $lines, [System.Text.UTF8Encoding]::new($false))
}

function New-RandomSecret([int]$Length = 40) {
    $value = ([Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N'))
    return $value.Substring(0, [Math]::Min($Length, $value.Length))
}

function Convert-ToConfigPath([string]$Path) {
    return $Path.Replace('\', '/')
}

function Test-TcpPort([string]$HostName, [int]$Port, [int]$TimeoutMilliseconds = 1000) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $result = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne($TimeoutMilliseconds, $false)) {
            return $false
        }
        $client.EndConnect($result)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-TcpPort([string]$HostName, [int]$Port, [int]$TimeoutSeconds = 45) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort $HostName $Port 700) {
            return $true
        }
        Start-Sleep -Milliseconds 600
    }
    return $false
}

function Wait-Http([string]$Url, [int]$TimeoutSeconds = 75) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return $true
            }
        } catch {
            Start-Sleep -Milliseconds 700
        }
    }
    return $false
}

function Write-MySqlConfig([string]$Root, [int]$Port) {
    $configDir = Join-Path $Root 'config'
    $dataDir = Join-Path $Root 'data\mysql'
    $secureDir = Join-Path $Root 'data\mysql-files'
    New-Item -ItemType Directory -Path $configDir, $dataDir, $secureDir -Force | Out-Null
    $lines = @(
        '[mysqld]',
        "basedir=$(Convert-ToConfigPath (Join-Path $Root 'runtime\mysql'))",
        "datadir=$(Convert-ToConfigPath $dataDir)",
        "secure-file-priv=$(Convert-ToConfigPath $secureDir)",
        "port=$Port",
        'bind-address=127.0.0.1',
        'mysqlx=0',
        'skip-log-bin',
        'character-set-server=utf8mb4',
        'collation-server=utf8mb4_0900_ai_ci',
        'default-time-zone=+08:00',
        'max_connections=80',
        '',
        '[client]',
        "port=$Port",
        'host=127.0.0.1',
        'default-character-set=utf8mb4'
    )
    $path = Join-Path $configDir 'mysql.ini'
    [System.IO.File]::WriteAllLines($path, $lines, [System.Text.UTF8Encoding]::new($false))
    return $path
}

function New-MySqlCredentialFile([string]$User, [string]$Password, [int]$Port) {
    $path = Join-Path $env:TEMP ("eduagent-mysql-{0}.cnf" -f [Guid]::NewGuid().ToString('N'))
    $lines = @('[client]', 'host=127.0.0.1', "port=$Port", "user=$User", "password=$Password", 'default-character-set=utf8mb4')
    [System.IO.File]::WriteAllLines($path, $lines, [System.Text.UTF8Encoding]::new($false))
    return $path
}
