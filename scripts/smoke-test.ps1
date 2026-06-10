param(
  [string]$BackendBaseUrl = "http://127.0.0.1:8080",
  [string]$AiBaseUrl = "http://127.0.0.1:8000",
  [string]$FrontendDir = "frontend",
  [string]$Username = "demo_student",
  [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Invoke-JsonPost {
  param(
    [string]$Url,
    [object]$Body,
    [hashtable]$Headers = @{}
  )
  $json = $Body | ConvertTo-Json -Depth 20
  Invoke-RestMethod -Method Post -Uri $Url -Headers $Headers -ContentType "application/json; charset=utf-8" -Body $json
}

function Assert-Success {
  param(
    [string]$Name,
    [object]$Response
  )
  if ($null -eq $Response) {
    throw "$Name returned empty response."
  }
  if ($Response.PSObject.Properties.Name -contains "success" -and -not $Response.success) {
    throw "$Name failed: $($Response | ConvertTo-Json -Depth 10)"
  }
  Write-Host "[PASS] $Name"
}

$aiHealth = Invoke-RestMethod -Uri "$AiBaseUrl/ai/health" -Method Get
Assert-Success "AI health" $aiHealth

$backendHealth = Invoke-RestMethod -Uri "$BackendBaseUrl/api/health" -Method Get
Assert-Success "Backend health" $backendHealth

$login = Invoke-JsonPost "$BackendBaseUrl/api/auth/login" @{ username = $Username; password = $Password }
Assert-Success "Login" $login
$token = $login.data.accessToken
if (-not $token) {
  throw "Login did not return data.accessToken."
}
$headers = @{ Authorization = "Bearer $token" }

$provider = Invoke-RestMethod -Uri "$BackendBaseUrl/api/model-providers/default" -Headers $headers -Method Get
Assert-Success "Default model provider" $provider
$providerId = $provider.data.id

$providerTest = Invoke-JsonPost "$BackendBaseUrl/api/model-providers/$providerId/test" @{ prompt = "Explain the mock model status in one sentence." } $headers
Assert-Success "Model provider test" $providerTest

$conversation = Invoke-JsonPost "$BackendBaseUrl/api/chat/conversations" @{ spaceId = 1; title = "Smoke Test Conversation"; intentType = "exam_review" } $headers
Assert-Success "Create conversation" $conversation
$conversationId = $conversation.data.id

$chat = Invoke-JsonPost "$BackendBaseUrl/api/chat/conversations/$conversationId/messages" @{ modelProviderId = $providerId; subject = "Database Systems"; message = "Explain second normal form." } $headers
Assert-Success "Chat message" $chat

$agent = Invoke-JsonPost "$BackendBaseUrl/api/agent-tasks" @{ spaceId = 1; providerId = $providerId; taskType = "resource_generation"; title = "Smoke Test Agent Task"; subject = "Database Systems"; resourceType = "plan"; inputParams = @{ days = 7 } } $headers
Assert-Success "Agent task" $agent

$resource = Invoke-JsonPost "$BackendBaseUrl/api/resources/generate" @{ spaceId = 1; modelProviderId = $providerId; title = "Smoke Test Review Resource"; subject = "Database Systems"; resourceType = "plan"; knowledgePoints = @("normalization", "index"); difficulty = "medium"; outputLength = "short"; useKnowledgeBase = $false } $headers
Assert-Success "Resource generation" $resource

$path = Invoke-JsonPost "$BackendBaseUrl/api/learning-paths/generate" @{ spaceId = 1; modelProviderId = $providerId; subject = "Database Systems"; goal = "Finish final exam review"; knowledgePoints = @("normalization", "SQL"); days = 7; preference = @{ outputStyle = "markdown" } } $headers
Assert-Success "Learning path generation" $path

$quiz = Invoke-JsonPost "$BackendBaseUrl/api/quizzes/generate" @{ spaceId = 1; modelProviderId = $providerId; subject = "Database Systems"; title = "Smoke Test Quiz"; knowledgePoints = @("normalization", "index"); questionCount = 3; difficulty = "medium" } $headers
Assert-Success "Quiz generation" $quiz

$report = Invoke-RestMethod -Uri "$BackendBaseUrl/api/reports/overview" -Headers $headers -Method Get
Assert-Success "Report overview" $report

Push-Location $FrontendDir
try {
  npm.cmd run build
  Write-Host "[PASS] Frontend build"
} finally {
  Pop-Location
}

Write-Host "Smoke test completed."
