param(
  [string]$BackendBaseUrl = "http://127.0.0.1:8080",
  [string]$Username = "demo_student",
  [string]$Password = "123456",
  [switch]$UseDefaultProvider,
  [switch]$ResourceOnly,
  [switch]$SkipResources,
  [string]$StartAt = ""
)

$ErrorActionPreference = "Stop"
$script:Passed = 0

function Invoke-JsonPost {
  param(
    [string]$Url,
    [object]$Body,
    [hashtable]$Headers = @{}
  )

  Invoke-RestMethod -Method Post -Uri $Url -Headers $Headers `
    -ContentType "application/json; charset=utf-8" `
    -Body ($Body | ConvertTo-Json -Depth 30)
}

function Assert-True {
  param(
    [string]$Name,
    [bool]$Condition
  )

  if (-not $Condition) {
    throw "$Name assertion failed."
  }
  $script:Passed++
  Write-Host "[PASS] $Name"
}

$login = Invoke-JsonPost "$BackendBaseUrl/api/auth/login" @{
  username = $Username
  password = $Password
}
Assert-True "Student login" ($login.success -and $login.data.accessToken)
$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }

if ($UseDefaultProvider) {
  $providerResponse = Invoke-RestMethod -Method Get -Uri "$BackendBaseUrl/api/model-providers/default" -Headers $headers
  $provider = $providerResponse.data
  Assert-True "Default provider available" ($null -ne $provider)
} else {
  $providers = Invoke-RestMethod -Method Get -Uri "$BackendBaseUrl/api/model-providers?pageNum=1&pageSize=50" -Headers $headers
  $provider = @($providers.data.records | Where-Object { $_.providerType -eq "mock" })[0]
  Assert-True "Local deterministic provider available" ($null -ne $provider)
}

$spaceId = $null
try {
  $space = Invoke-JsonPost "$BackendBaseUrl/api/learning-spaces" @{
    spaceName = "Experience regression $(Get-Date -Format 'yyyyMMdd-HHmmss')"
    subject = "Database Systems"
    description = "Temporary space for resource, agent and learning path regression checks."
    learningGoal = "Master B+ tree indexes, transaction isolation and query optimization."
    foundationLevel = "intermediate"
    weakPoints = @("Index selection", "Transaction isolation")
    weeklyAvailableHours = 8
    availableTimeSlots = @("Weekday evenings")
    outputStyle = ""
  } $headers
  Assert-True "Create isolated QA space" ($space.success -and $space.data.id)
  $spaceId = [long]$space.data.id

  $resourceTypes = @(
    "lecture_note",
    "summary",
    "knowledge_graph",
    "quiz_set",
    "review_outline",
    "mistake_review",
    "plan",
    "case_task"
  )
  if ($StartAt) {
    $startIndex = [array]::IndexOf($resourceTypes, $StartAt)
    Assert-True "Requested start type exists" ($startIndex -ge 0)
    $resourceTypes = @($resourceTypes[$startIndex..($resourceTypes.Count - 1)])
  }

  $contentFingerprints = @{}
  $generationModes = @{}
  if ($SkipResources) {
    $resourceTypes = @()
  }

  foreach ($resourceType in $resourceTypes) {
    $response = Invoke-JsonPost "$BackendBaseUrl/api/resources/generate" @{
      spaceId = $spaceId
      modelProviderId = [long]$provider.id
      title = "Database Systems $resourceType acceptance"
      subject = "Database Systems"
      resourceType = $resourceType
      knowledgePoints = @("B+ tree index", "transaction isolation", "query optimization")
      difficulty = "medium"
      outputLength = $(if ($UseDefaultProvider) { "short" } else { "medium" })
      useKnowledgeBase = $false
    } $headers

    Assert-True "$resourceType generation succeeds" ($response.success -and $response.data.resource.id)
    $resource = $response.data.resource
    $content = [string]$resource.contentMarkdown
    $checks = $resource.contentJson.quality_checks
    $actualResourceType = if ($resource.resourceType) {
      $resource.resourceType
    } elseif ($resource.contentJson.resource_type) {
      $resource.contentJson.resource_type
    } else {
      $resource.contentJson.resource_contract.resource_type
    }
    Assert-True "$resourceType uses requested contract" ($actualResourceType -eq $resourceType)
    Write-Host "[CHECK] $resourceType chars=$($checks.character_count) min=$($checks.minimum_character_count) missing=$([string]::Join(',', @($checks.missing_sections)))"
    Assert-True "$resourceType passes type quality checks" ($checks.valid -eq $true -and @($checks.missing_sections).Count -eq 0)
    Assert-True "$resourceType has required length" ($content.Length -ge [int]$checks.minimum_character_count)
    $contentFingerprints[$resourceType] = $content.GetHashCode()
    $generationModes[$resourceType] = if ($resource.contentJson.real_provider -eq $true) {
      "model"
    } elseif ($resource.contentJson.provider_fallback -eq $true) {
      "typed-fallback"
    } else {
      "deterministic"
    }
    Write-Host "[MODE] $resourceType -> $($generationModes[$resourceType])"
  }
  if ($resourceTypes.Count -gt 0) {
    Assert-True "All resource types produce distinct bodies" (@($contentFingerprints.Values | Select-Object -Unique).Count -eq $resourceTypes.Count)
  }

  if (-not $ResourceOnly) {
    $agent = Invoke-JsonPost "$BackendBaseUrl/api/agent-tasks" @{
      spaceId = $spaceId
      providerId = [long]$provider.id
      taskType = "resource_generation"
      title = "Database index case task"
      subject = "Database Systems"
      resourceType = "case_task"
      inputParams = @{
        knowledge_points = @("B+ tree index", "transaction isolation", "query optimization")
        difficulty = "medium"
        output_length = "medium"
        use_knowledge_base = $false
      }
    } $headers
    Assert-True "Multi-Agent execution succeeds" ($agent.success -and $agent.data.task.executionStatus -eq "succeeded")
    $steps = @($agent.data.steps)
    Assert-True "Multi-Agent has four distinct stages" ($steps.Count -ge 4 -and @($steps.agentName | Select-Object -Unique).Count -ge 4)
    foreach ($step in $steps) {
      Assert-True "Agent step $($step.agentName) has input" ($null -ne $step.inputJson -and $step.inputJson.ToString() -ne "{}")
      Assert-True "Agent step $($step.agentName) has output" ($null -ne $step.resultJson -and $step.resultJson.ToString() -ne "{}")
      Assert-True "Agent step $($step.agentName) has readable summary" (-not [string]::IsNullOrWhiteSpace($step.outputSummary))
    }
    Assert-True "Agent resource follows requested contract" ($agent.data.resources[0].contentJson.quality_checks.valid -eq $true)

    $path = Invoke-JsonPost "$BackendBaseUrl/api/learning-paths/generate" @{
      spaceId = $spaceId
      modelProviderId = [long]$provider.id
      subject = "Database Systems"
      goal = "Finish an executable final review in five days."
      knowledgePoints = @("B+ tree index", "transaction isolation", "query optimization")
      days = 5
      preference = @{ dailyMinutes = 75 }
    } $headers
    Assert-True "Learning path generation succeeds" ($path.success -and $path.data.id)
    $items = @($path.data.items)
    Assert-True "Learning path covers all five days" ($items.Count -eq 5)
    Assert-True "Learning path descriptions are distinct" (@($items.description | Select-Object -Unique).Count -eq $items.Count)
    foreach ($item in $items) {
      Assert-True "Path item $($item.itemOrder) is detailed" ($item.description.Length -ge 80 -and @($item.description -split "`n").Count -ge 3)
      Assert-True "Path item $($item.itemOrder) is scheduled" ($null -ne $item.dueDate -and $item.estimatedMinutes -ge 10)
    }
  }

  Write-Host "[SUMMARY] $script:Passed assertions passed."
  foreach ($resourceType in $resourceTypes) {
    Write-Host "[SUMMARY] $resourceType mode: $($generationModes[$resourceType])"
  }
}
finally {
  if ($spaceId) {
    Invoke-RestMethod -Method Delete -Uri "$BackendBaseUrl/api/learning-spaces/$spaceId" -Headers $headers | Out-Null
    Write-Host "[CLEANUP] Removed temporary learning space $spaceId and all generated data."
  }
}
