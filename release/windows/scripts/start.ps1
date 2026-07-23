param([switch]$NoBrowser)

. (Join-Path $PSScriptRoot 'common.ps1')

$root = Get-ReleaseRoot
$configPath = Join-Path $root 'config\local.env'
$dataMarker = Join-Path $root 'data\mysql\mysql'
if (-not (Test-Path -LiteralPath $configPath) -or -not (Test-Path -LiteralPath $dataMarker)) {
    & (Join-Path $PSScriptRoot 'install.ps1')
}

$config = Read-EnvFile $configPath
$mysqlPort = [int]$config['MYSQL_PORT']
$aiPort = [int]$config['AI_PORT']
$backendPort = [int]$config['BACKEND_PORT']
$backendHealth = "http://127.0.0.1:$backendPort/api/health"

try {
    $existing = Invoke-WebRequest -UseBasicParsing -Uri $backendHealth -TimeoutSec 2
    if ($existing.StatusCode -eq 200) {
        Write-Host 'EduAgent Studio is already running.' -ForegroundColor Green
        if (-not $NoBrowser) {
            Start-Process "http://127.0.0.1:$backendPort"
        }
        exit 0
    }
} catch {
    if (Test-TcpPort '127.0.0.1' $backendPort 500) {
        throw "Port $backendPort is already in use."
    }
}

foreach ($port in @($aiPort)) {
    if (Test-TcpPort '127.0.0.1' $port 500) {
        throw "Port $port is already in use."
    }
}

$logDir = Join-Path $root 'logs'
$runDir = Join-Path $root 'run'
$dataDir = Join-Path $root 'data'
New-Item -ItemType Directory -Path $logDir, $runDir, $dataDir -Force | Out-Null

$mysqld = Join-Path $root 'runtime\mysql\bin\mysqld.exe'
$releaseDrive = Mount-ReleaseDrive $root
$mappedRoot = "${releaseDrive}\"
$mappedMysqld = Join-Path $mappedRoot 'runtime\mysql\bin\mysqld.exe'
$mysqlConfig = Write-MySqlConfig $mappedRoot $mysqlPort
$mysqlProcess = $null
if (-not (Test-TcpPort '127.0.0.1' $mysqlPort 500)) {
    $mysqlProcess = Start-Process -FilePath $mappedMysqld -ArgumentList "--defaults-file=$mysqlConfig", '--console' -WorkingDirectory $mappedRoot -RedirectStandardOutput (Join-Path $logDir 'mysql.out.log') -RedirectStandardError (Join-Path $logDir 'mysql.err.log') -WindowStyle Hidden -PassThru
    if (-not (Wait-TcpPort '127.0.0.1' $mysqlPort 60)) {
        Stop-Process -Id $mysqlProcess.Id -Force -ErrorAction SilentlyContinue
        Dismount-ReleaseDrive $releaseDrive
        throw 'Bundled MySQL failed to start. See logs\mysql.err.log.'
    }
}

$env:AI_SERVICE_NAME = 'eduagent-ai-service'
$env:AI_SERVICE_VERSION = '0.1.0'
$env:AI_SERVICE_MOCK_MODE = 'true'
$env:EDUAGENT_DATA_DIR = Join-Path $dataDir 'ai'
$env:RAG_VECTOR_DIR = Join-Path $dataDir 'rag'
$env:RAG_VECTOR_BACKEND = 'mysql'
$env:CHROMA_PERSIST_DIR = Join-Path $dataDir 'rag\chroma'

$aiExe = Join-Path $root 'app\ai\EduAgentAI\EduAgentAI.exe'
$javaExe = Join-Path $root 'runtime\jre\bin\java.exe'
$backendJar = Join-Path $root 'app\eduagent-studio.jar'
foreach ($required in @($aiExe, $javaExe, $backendJar)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required release file is missing: $required"
    }
}

$aiProcess = Start-Process -FilePath $aiExe -ArgumentList '--host', '127.0.0.1', '--port', "$aiPort" -WorkingDirectory $dataDir -RedirectStandardOutput (Join-Path $logDir 'ai.out.log') -RedirectStandardError (Join-Path $logDir 'ai.err.log') -WindowStyle Hidden -PassThru
if (-not (Wait-Http "http://127.0.0.1:$aiPort/ai/health" 60)) {
    Stop-Process -Id $aiProcess.Id -Force -ErrorAction SilentlyContinue
    if ($mysqlProcess) { Stop-Process -Id $mysqlProcess.Id -Force -ErrorAction SilentlyContinue }
    Dismount-ReleaseDrive $releaseDrive
    throw 'AI service failed to start. See logs\ai.err.log.'
}

$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:MYSQL_URL = "jdbc:mysql://127.0.0.1:$mysqlPort/$($config['MYSQL_DATABASE'])?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:MYSQL_USERNAME = $config['MYSQL_USERNAME']
$env:MYSQL_PASSWORD = $config['MYSQL_PASSWORD']
$env:JWT_SECRET = $config['JWT_SECRET']
$env:AI_SERVICE_BASE_URL = "http://127.0.0.1:$aiPort"
$env:SERVER_PORT = "$backendPort"
$env:EDUAGENT_KNOWLEDGE_STORAGE_DIR = Join-Path $dataDir 'knowledge'

$javaArgs = @('-Dfile.encoding=UTF-8', '-Duser.timezone=Asia/Shanghai', '-jar', "`"$backendJar`"")
$backendProcess = Start-Process -FilePath $javaExe -ArgumentList $javaArgs -WorkingDirectory $dataDir -RedirectStandardOutput (Join-Path $logDir 'backend.out.log') -RedirectStandardError (Join-Path $logDir 'backend.err.log') -WindowStyle Hidden -PassThru
if (-not (Wait-Http $backendHealth 90)) {
    Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $aiProcess.Id -Force -ErrorAction SilentlyContinue
    if ($mysqlProcess) { Stop-Process -Id $mysqlProcess.Id -Force -ErrorAction SilentlyContinue }
    Dismount-ReleaseDrive $releaseDrive
    throw 'Backend failed to start. See logs\backend.err.log and logs\backend.out.log.'
}

$state = @{
    mysql = $(if ($mysqlProcess) { $mysqlProcess.Id } else { $null })
    ai = $aiProcess.Id
    backend = $backendProcess.Id
    releaseDrive = $releaseDrive
    startedAt = (Get-Date).ToString('s')
}
$state | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDir 'processes.json') -Encoding UTF8

Write-Host ''
Write-Host "EduAgent Studio started: http://127.0.0.1:$backendPort" -ForegroundColor Green
Write-Host 'Demo account: demo_student / 123456'
if (-not $NoBrowser) {
    Start-Process "http://127.0.0.1:$backendPort"
}
