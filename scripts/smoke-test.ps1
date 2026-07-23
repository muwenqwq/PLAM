param(
  [string]$BackendBaseUrl = "http://127.0.0.1:8080",
  [string]$AiBaseUrl = "http://127.0.0.1:8000",
  [string]$FrontendDir = "frontend",
  [string]$BackendDataDir = "",
  [string]$Username = "demo_student",
  [string]$Password = "123456",
  [long]$ProviderId = 0,
  [switch]$SkipFrontendBuild
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
  $json = $Body | ConvertTo-Json -Depth 30
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

function Assert-True {
  param(
    [string]$Name,
    [bool]$Condition
  )
  if (-not $Condition) {
    throw "$Name assertion failed."
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

$providerUrl = if ($ProviderId -gt 0) {
  "$BackendBaseUrl/api/model-providers/$ProviderId"
} else {
  "$BackendBaseUrl/api/model-providers/default"
}
$provider = Invoke-RestMethod -Uri $providerUrl -Headers $headers -Method Get
Assert-Success "Selected model provider" $provider
$providerId = [long]$provider.data.id

$providerTest = Invoke-JsonPost "$BackendBaseUrl/api/model-providers/$providerId/test" @{
  prompt = "Explain the configured model status in one sentence."
} $headers
Assert-Success "Model provider test" $providerTest

$activeRole = Invoke-RestMethod -Uri "$BackendBaseUrl/api/companion-roles/active" -Headers $headers -Method Get
Assert-Success "Active companion role" $activeRole
if (-not $activeRole.data) {
  $activeRole = Invoke-JsonPost "$BackendBaseUrl/api/companion-roles" @{
    roleName = "Study Companion"
    roleIdentity = "Learning companion"
    speakingStyle = "Clear, patient and encouraging"
    companionGoal = "Break learning tasks into manageable steps"
    defaultRole = $true
  } $headers
  Assert-Success "Create companion role" $activeRole
}
$roleId = $activeRole.data.id

$spaceId = $null
try {
  $space = Invoke-JsonPost "$BackendBaseUrl/api/learning-spaces" @{
    spaceName = "Space isolation smoke $(Get-Date -Format 'yyyyMMdd-HHmmss')"
    subject = "Database Systems"
    description = "Verify profiles, resources, quizzes, reports and cascade cleanup"
    learningGoal = "Master database indexes and normalization"
    foundationLevel = "beginner"
    weakPoints = @("Database indexes", "Second normal form")
    weeklyAvailableHours = 6
    availableTimeSlots = @("Weekday evenings", "Weekend mornings")
    outputStyle = "Structured and example first"
  } $headers
  Assert-Success "Create isolated learning space" $space
  $spaceId = [long]$space.data.id

  $profile = Invoke-RestMethod -Uri "$BackendBaseUrl/api/profiles/space/$spaceId" -Headers $headers -Method Get
  Assert-Success "Load space learner profile" $profile
  Assert-True "Initial learner profile" ($profile.data.learningGoal -eq "Master database indexes and normalization" -and $profile.data.foundationLevel -eq "beginner")

  $profile = Invoke-RestMethod -Uri "$BackendBaseUrl/api/profiles/space/$spaceId" -Headers $headers -Method Put -ContentType "application/json; charset=utf-8" -Body (@{
    profileNarrative = "I am preparing for a database final. I understand SQL basics but need concrete examples for indexes and normalization."
    learningGoal = "Master database indexes and normalization"
    subjectDirection = "Database Systems"
    foundationLevel = "beginner"
    weakPoints = @("Database indexes", "Second normal form")
    interestTags = @("Worked examples", "Visual summaries")
    weeklyAvailableHours = 6
    availableTime = @("Weekday evenings", "Weekend mornings")
    outputStyle = "Structured and example first"
  } | ConvertTo-Json -Depth 10)
  Assert-Success "Save natural-language learner profile" $profile
  Assert-True "Learner profile exposes adaptive summary" (
    $profile.data.profileNarrative -match "database final" -and
    -not [string]::IsNullOrWhiteSpace($profile.data.adaptiveSummary)
  )

  $conversation = Invoke-JsonPost "$BackendBaseUrl/api/chat/conversations" @{
    spaceId = $spaceId
    title = "Database review chat"
    intentType = "exam_review"
    roleId = $roleId
    rolePlayEnabled = $true
  } $headers
  Assert-Success "Create space conversation" $conversation
  $conversationId = $conversation.data.id

  $chat = Invoke-JsonPost "$BackendBaseUrl/api/chat/conversations/$conversationId/messages" @{
    modelProviderId = $providerId
    subject = "Database Systems"
    message = "Please explain second normal form step by step with a concrete example."
  } $headers
  Assert-Success "Profile-aware chat" $chat
  Assert-True "Profile-aware chat returns a substantive answer" (
    -not [string]::IsNullOrWhiteSpace([string]$chat.data.assistantMessage.contentMd) -and
    ([string]$chat.data.assistantMessage.contentMd).Length -ge 80
  )

  $profileAfterChat = Invoke-RestMethod -Uri "$BackendBaseUrl/api/profiles/space/$spaceId" -Headers $headers -Method Get
  Assert-Success "Reload learner profile after chat" $profileAfterChat
  Write-Host ("[INFO] Chat profile source={0}; summarySanitized={1}; narrativeSanitized={2}; goalPreserved={3}; stylePreserved={4}; activityTimestamp={5}" -f `
    $profileAfterChat.data.lastActivitySource,
    ($profileAfterChat.data.lastActivitySummary -notmatch "Please explain second normal form"),
    ($profileAfterChat.data.profileNarrative -notmatch "Please explain second normal form"),
    ($profileAfterChat.data.learningGoal -eq "Master database indexes and normalization"),
    (-not [string]::IsNullOrWhiteSpace([string]$profileAfterChat.data.outputStyle)),
    ($null -ne $profileAfterChat.data.lastActivityAt))
  Assert-True "Chat activity updates profile without copying the raw utterance" (
    $profileAfterChat.data.lastActivitySource -eq "chat" -and
    $profileAfterChat.data.lastActivitySummary -notmatch "Please explain second normal form" -and
    $profileAfterChat.data.profileNarrative -notmatch "Please explain second normal form" -and
    $profileAfterChat.data.learningGoal -eq "Master database indexes and normalization" -and
    -not [string]::IsNullOrWhiteSpace([string]$profileAfterChat.data.outputStyle) -and
    $null -ne $profileAfterChat.data.lastActivityAt
  )

  $streamBody = @{
    modelProviderId = $providerId
    subject = "Database Systems"
    message = "Explain database indexes with a short example."
  } | ConvertTo-Json -Depth 10
  $stream = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BackendBaseUrl/api/chat/conversations/$conversationId/messages/stream" -Headers $headers -ContentType "application/json; charset=utf-8" -Body $streamBody
  Assert-True "Streaming role-play chat" ($stream.Content -match '"type":"delta"' -and $stream.Content -match '"type":"done"')

  $agent = Invoke-JsonPost "$BackendBaseUrl/api/agent-tasks" @{
    spaceId = $spaceId
    providerId = $providerId
    taskType = "resource_generation"
    title = "Database review package"
    subject = "Database Systems"
    resourceType = "plan"
    inputParams = @{ days = 7 }
  } $headers
  Assert-Success "Space Agent task" $agent

  $resource = Invoke-JsonPost "$BackendBaseUrl/api/resources/generate" @{
    spaceId = $spaceId
    modelProviderId = $providerId
    title = "Database index review resource"
    subject = "Database Systems"
    resourceType = "plan"
    knowledgePoints = @("Normalization", "Indexes")
    difficulty = "medium"
    outputLength = "short"
    useKnowledgeBase = $false
    rolePlayEnabled = $true
    companionRoleId = $roleId
  } $headers
  Assert-Success "Space resource generation" $resource
  Assert-True "Generated resource uses learner profile" (
    [bool]$resource.data.resource.contentJson.profile_adapted -and
    @($resource.data.resource.contentJson.profile_fields) -contains "learning_goal"
  )
  $resourceId = $resource.data.resource.id

  $resourcePage = Invoke-RestMethod -Uri "$BackendBaseUrl/api/resources?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
  Assert-Success "Space resource list" $resourcePage
  Assert-True "Generated resource belongs to selected space" (@($resourcePage.data.records | Where-Object { $_.id -eq $resourceId }).Count -eq 1)

  $uploadSource = Join-Path $env:TEMP "plam-knowledge-$spaceId.md"
  Set-Content -LiteralPath $uploadSource -Encoding utf8 -Value "# Database review`nA B+ tree index reduces disk I/O by keeping the tree shallow."
  $uploadRaw = & curl.exe -sS -X POST "$BackendBaseUrl/api/knowledge/files/upload?spaceId=$spaceId" `
    -H "Authorization: Bearer $token" `
    -F "file=@$uploadSource;type=text/markdown"
  $uploadedFile = $uploadRaw | ConvertFrom-Json
  Assert-Success "Upload and index space knowledge file" $uploadedFile
  $knowledgeFileId = [long]$uploadedFile.data.id
  $knowledgeDataDir = if ([string]::IsNullOrWhiteSpace($BackendDataDir)) {
    Join-Path $root "backend/data/knowledge"
  } else {
    Join-Path ([System.IO.Path]::GetFullPath($BackendDataDir)) "knowledge"
  }
  $storedFile = Join-Path $knowledgeDataDir $uploadedFile.data.storagePath
  Assert-True "Uploaded source exists on disk" (Test-Path -LiteralPath $storedFile)

  $path = Invoke-JsonPost "$BackendBaseUrl/api/learning-paths/generate" @{
    spaceId = $spaceId
    modelProviderId = $providerId
    subject = "Database Systems"
    goal = "Finish the database final review"
    knowledgePoints = @("Normalization", "SQL", "Indexes")
    days = 7
    preference = @{ outputStyle = "markdown" }
  } $headers
  Assert-Success "Space learning path generation" $path

  $adjustedPath = Invoke-RestMethod -Method Post -Uri "$BackendBaseUrl/api/learning-paths/$($path.data.id)/adjust" -Headers $headers
  Assert-Success "Adjust learning path with persisted knowledge points" $adjustedPath
  Assert-True "Adjusted path exposes adjustment reason and summary" (
    -not [string]::IsNullOrWhiteSpace([string]$adjustedPath.data.planJson.adjust_reason) -and
    -not [string]::IsNullOrWhiteSpace([string]$adjustedPath.data.planJson.adjust_summary)
  )

  $quiz = Invoke-JsonPost "$BackendBaseUrl/api/quizzes/generate" @{
    spaceId = $spaceId
    modelProviderId = $providerId
    subject = "Database Systems"
    title = "Database stage quiz"
    knowledgePoints = @("Normalization", "Indexes")
    questionCount = 3
    questionType = "single_choice"
    difficulty = "medium"
  } $headers
  Assert-Success "Space quiz generation" $quiz
  $questions = @($quiz.data.questions)
  Assert-True "Quiz contains questions" ($questions.Count -gt 0)
  foreach ($question in $questions) {
    Assert-True "Quiz question $($question.questionOrder) is four-option single choice" (
      $question.questionType -eq "single_choice" -and
      @($question.options).Count -eq 4 -and
      $question.answerText -match '^[A-D]$'
    )
    Assert-True "Quiz question $($question.questionOrder) explains every option" (
      @($question.optionExplanations.PSObject.Properties.Name).Count -eq 4 -and
      @($question.optionExplanations.PSObject.Properties.Name | Where-Object { $_ -match '^[A-D]$' }).Count -eq 4
    )
  }

  Assert-True "Quiz is also saved as a learning-space resource" ($null -ne $quiz.data.resourceId)
  $quizResourceId = [long]$quiz.data.resourceId
  $resourcePageWithQuiz = Invoke-RestMethod -Uri "$BackendBaseUrl/api/resources?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
  Assert-Success "Reload resources after quiz generation" $resourcePageWithQuiz
  $quizResources = @($resourcePageWithQuiz.data.records | Where-Object { $_.id -eq $quizResourceId -and $_.resourceType -eq "quiz_set" })
  Assert-True "Quiz resource belongs to selected space and links back to quiz" (
    $quizResources.Count -eq 1 -and
    [long]$quizResources[0].contentJson.quizId -eq [long]$quiz.data.id
  )

  $answers = @($questions | ForEach-Object {
    $correctAnswer = [string]$_.answerText
    $submittedAnswer = $correctAnswer
    if ([int]$_.questionOrder -eq 1) {
      $submittedAnswer = @("A", "B", "C", "D") | Where-Object { $_ -ne $correctAnswer } | Select-Object -First 1
    }
    @{ questionId = $_.id; answerText = $submittedAnswer }
  })
  $quizResult = Invoke-JsonPost "$BackendBaseUrl/api/quizzes/$($quiz.data.id)/submit" @{
    answers = $answers
    rolePlayEnabled = $true
    companionRoleId = $roleId
  } $headers
  Assert-Success "Submit multiple-choice quiz" $quizResult
  Assert-True "Quiz returns per-question feedback" (@($quizResult.data.questionFeedbacks).Count -eq $questions.Count)
  foreach ($feedback in @($quizResult.data.questionFeedbacks)) {
    Assert-True "Submitted question $($feedback.questionOrder) keeps per-option feedback" (
      @($feedback.optionExplanations.PSObject.Properties.Name).Count -eq 4
    )
  }

  $wrongFeedbacks = @($quizResult.data.questionFeedbacks | Where-Object { -not $_.correct })
  Assert-True "Quiz submission contains a deterministic real mistake" ($wrongFeedbacks.Count -ge 1)
  $firstWrong = $wrongFeedbacks[0]

  $profileAfterQuiz = Invoke-RestMethod -Uri "$BackendBaseUrl/api/profiles/space/$spaceId" -Headers $headers -Method Get
  Assert-Success "Reload learner profile after quiz" $profileAfterQuiz
  Assert-True "Assessment evidence updates inferred learning situation" (
    $profileAfterQuiz.data.lastActivitySource -eq "assessment" -and
    $profileAfterQuiz.data.lastActivitySummary -notmatch [regex]::Escape([string]$firstWrong.answerText)
  )

  $mistakeResource = Invoke-JsonPost "$BackendBaseUrl/api/resources/generate" @{
    spaceId = $spaceId
    modelProviderId = $providerId
    title = "Database mistake review"
    subject = "Database Systems"
    resourceType = "mistake_review"
    knowledgePoints = @("Normalization", "Indexes")
    difficulty = "medium"
    outputLength = "short"
    useKnowledgeBase = $false
  } $headers
  Assert-Success "Generate mistake review from selected-space quiz answers" $mistakeResource
  $mistakeMarkdown = [string]$mistakeResource.data.resource.contentMarkdown
  Assert-True "Mistake review metadata confirms real assessment evidence" (
    [bool]$mistakeResource.data.resource.contentJson.has_actual_mistakes -and
    [int]$mistakeResource.data.resource.contentJson.card_count -ge 1
  )
  Assert-True "Mistake review preserves the real question and answer comparison" (
    $mistakeMarkdown -match [regex]::Escape([string]$firstWrong.stem) -and
    $mistakeMarkdown -match [regex]::Escape([string]$firstWrong.answerText) -and
    $mistakeMarkdown -match [regex]::Escape([string]$firstWrong.correctAnswer)
  )

  $overview = Invoke-RestMethod -Uri "$BackendBaseUrl/api/reports/overview?spaceId=$spaceId" -Headers $headers -Method Get
  Assert-Success "Space report overview" $overview
  Assert-True "Overview uses space data" ($overview.data.resourceCount -ge 1 -and $overview.data.quizCount -ge 1 -and $overview.data.pathCount -ge 1)

  $report = Invoke-JsonPost "$BackendBaseUrl/api/reports/generate" @{
    spaceId = $spaceId
    modelProviderId = $providerId
    reportType = "space_weekly"
    title = "Database weekly report"
    rolePlayEnabled = $true
    companionRoleId = $roleId
  } $headers
  Assert-Success "Generate populated learning report" $report
  $reportJson = $report.data.reportJson | ConvertTo-Json -Depth 20
  Assert-True "Report contains strengths, weak points and next actions" (
    $reportJson -match 'strengths' -and
    $reportJson -match 'weak_points' -and
    $reportJson -match 'next_actions'
  )
  Assert-True "Report contains space learning evidence" (
    $reportJson -match 'activity_summary' -and
    $reportJson -match 'resource_breakdown' -and
    $reportJson -match 'path_progress' -and
    $reportJson -match 'quiz_performance' -and
    $reportJson -match 'knowledge_library'
  )

  $spaceReports = Invoke-RestMethod -Uri "$BackendBaseUrl/api/reports/space/$spaceId" -Headers $headers -Method Get
  Assert-Success "List reports by learning space" $spaceReports
  Assert-True "Generated report belongs to selected space" (@($spaceReports.data | Where-Object { $_.id -eq $report.data.id }).Count -eq 1)

  $deletedReport = Invoke-RestMethod -Uri "$BackendBaseUrl/api/reports/$($report.data.id)" -Headers $headers -Method Delete
  Assert-Success "Delete generated report" $deletedReport
  $spaceReportsAfterDelete = Invoke-RestMethod -Uri "$BackendBaseUrl/api/reports/space/$spaceId" -Headers $headers -Method Get
  Assert-True "Deleted report no longer appears" (@($spaceReportsAfterDelete.data | Where-Object { $_.id -eq $report.data.id }).Count -eq 0)

  $deletedQuiz = Invoke-RestMethod -Uri "$BackendBaseUrl/api/quizzes/$($quiz.data.id)" -Headers $headers -Method Delete
  Assert-Success "Delete generated quiz" $deletedQuiz
  $quizPageAfterDelete = Invoke-RestMethod -Uri "$BackendBaseUrl/api/quizzes?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
  Assert-True "Deleted quiz no longer appears" (@($quizPageAfterDelete.data.records | Where-Object { $_.id -eq $quiz.data.id }).Count -eq 0)
  $resourcesAfterQuizDelete = Invoke-RestMethod -Uri "$BackendBaseUrl/api/resources?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
  Assert-True "Deleting quiz also removes its linked resource" (@($resourcesAfterQuizDelete.data.records | Where-Object { $_.id -eq $quizResourceId }).Count -eq 0)

  $deletedKnowledge = Invoke-RestMethod -Uri "$BackendBaseUrl/api/knowledge/files/$knowledgeFileId" -Headers $headers -Method Delete
  Assert-Success "Delete uploaded knowledge file" $deletedKnowledge
  Assert-True "Deleting knowledge file removes disk source" (-not (Test-Path -LiteralPath $storedFile))

  $deletedResource = Invoke-RestMethod -Uri "$BackendBaseUrl/api/resources/$resourceId" -Headers $headers -Method Delete
  Assert-Success "Delete generated resource" $deletedResource
  $resourcePageAfterDelete = Invoke-RestMethod -Uri "$BackendBaseUrl/api/resources?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
  Assert-True "Deleted generated resource no longer appears" (@($resourcePageAfterDelete.data.records | Where-Object { $_.id -eq $resourceId }).Count -eq 0)
}
finally {
  if ($uploadSource -and (Test-Path -LiteralPath $uploadSource)) {
    Remove-Item -LiteralPath $uploadSource -Force
  }
  if ($spaceId) {
    $deleted = Invoke-RestMethod -Uri "$BackendBaseUrl/api/learning-spaces/$spaceId" -Headers $headers -Method Delete
    Assert-Success "Delete isolated learning space" $deleted

    $remainingResources = Invoke-RestMethod -Uri "$BackendBaseUrl/api/resources?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
    $remainingQuizzes = Invoke-RestMethod -Uri "$BackendBaseUrl/api/quizzes?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
    $remainingPaths = Invoke-RestMethod -Uri "$BackendBaseUrl/api/learning-paths?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
    $remainingAgents = Invoke-RestMethod -Uri "$BackendBaseUrl/api/agent-tasks?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
    $remainingChats = Invoke-RestMethod -Uri "$BackendBaseUrl/api/chat/conversations?spaceId=$spaceId&pageNum=1&pageSize=100" -Headers $headers -Method Get
    Assert-True "Cascade cleanup removes space-owned records" (
      $remainingResources.data.total -eq 0 -and
      $remainingQuizzes.data.total -eq 0 -and
      $remainingPaths.data.total -eq 0 -and
      $remainingAgents.data.total -eq 0 -and
      $remainingChats.data.total -eq 0
    )
  }
}

if (-not $SkipFrontendBuild) {
  Push-Location $FrontendDir
  try {
    npm.cmd run build
    Write-Host "[PASS] Frontend build"
  }
  finally {
    Pop-Location
  }
}

Write-Host "Smoke test completed."
