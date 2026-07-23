param(
  [Parameter(Mandatory = $true)][string]$ProductSource,
  [Parameter(Mandatory = $true)][string]$SourcePackageSource,
  [Parameter(Mandatory = $true)][string]$IntroSource,
  [Parameter(Mandatory = $true)][string]$DestinationRoot,
  [string]$ProductFolderName = "01_product",
  [string]$SourceFolderName = "02_source",
  [string]$IntroFolderName = "03_intro"
)

$ErrorActionPreference = "Stop"

function Get-FullPath([string]$Path) {
  return [System.IO.Path]::GetFullPath($Path).TrimEnd('\')
}

function Test-SkipPath([string]$RelativePath, [string]$Kind) {
  $path = $RelativePath.Replace('/', '\').ToLowerInvariant()
  if ($Kind -eq "product") {
    return $path.StartsWith("data\") `
      -or $path.StartsWith("logs\") `
      -or $path.StartsWith("run\") `
      -or $path -eq "config\local.env" `
      -or $path -eq "config\mysql.ini" `
      -or $path.EndsWith(".sha256")
  }
  if ($Kind -eq "source") {
    $segments = @($path.Split('\'))
    $forbiddenSegments = @(
      ".git", ".venv", "venv", "node_modules", "__pycache__", ".pytest_cache",
      ".mypy_cache", ".rag_vector_store", ".npm", ".npm-cache", "target", "dist",
      "coverage", "logs", "run", "tmp", "temp"
    )
    if (@($segments | Where-Object { $forbiddenSegments -contains $_ }).Count -gt 0) {
      return $true
    }
    return $path.StartsWith("backend\data\") `
      -or $path.StartsWith("ai-service\data\") `
      -or $path.StartsWith("projects\") `
      -or $path.EndsWith(".pyc") `
      -or $path.EndsWith(".pyo") `
      -or $path.EndsWith(".log") `
      -or $path.EndsWith(".tmp") `
      -or $path.EndsWith(".temp") `
      -or $path.EndsWith("local.env") `
      -or $path.EndsWith(".env") `
      -or $path.EndsWith(".sha256")
  }
  return $RelativePath.EndsWith(".sha256", [System.StringComparison]::OrdinalIgnoreCase)
}

function Copy-TreeWithProgress(
  [string]$Source,
  [string]$Destination,
  [string]$Kind,
  [string]$Label
) {
  $sourceRoot = Get-FullPath $Source
  $destinationRoot = Get-FullPath $Destination
  if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) {
    throw "Source directory does not exist: $sourceRoot"
  }
  if (Test-Path -LiteralPath $destinationRoot) {
    $existing = @(Get-ChildItem -LiteralPath $destinationRoot -Force -ErrorAction SilentlyContinue)
    if ($existing.Count -gt 0) {
      throw "Destination must be new or empty: $destinationRoot"
    }
  }
  New-Item -ItemType Directory -Path $destinationRoot -Force | Out-Null

  $files = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Force | Where-Object {
    $relative = $_.FullName.Substring($sourceRoot.Length).TrimStart('\')
    -not (Test-SkipPath $relative $Kind)
  })
  $total = $files.Count
  Write-Host "[$Label] Copying $total files from $sourceRoot"

  for ($index = 0; $index -lt $total; $index++) {
    $file = $files[$index]
    $relative = $file.FullName.Substring($sourceRoot.Length).TrimStart('\')
    $target = Join-Path $destinationRoot $relative
    $parent = Split-Path -Parent $target
    if (-not (Test-Path -LiteralPath $parent)) {
      New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    $copied = $false
    for ($attempt = 1; $attempt -le 3 -and -not $copied; $attempt++) {
      try {
        Copy-Item -LiteralPath $file.FullName -Destination $target -Force -ErrorAction Stop
        $copied = $true
      } catch {
        if ($attempt -ge 3) {
          throw "Failed after two retries: $relative. $($_.Exception.Message)"
        }
        Start-Sleep -Milliseconds 500
      }
    }

    $current = $index + 1
    $percent = if ($total -eq 0) { 100 } else { [int](100 * $current / $total) }
    Write-Progress -Activity "Copying $Label" -Status "$current/$total $relative" -PercentComplete $percent
    if ($current -eq 1 -or $current -eq $total -or $current % 25 -eq 0) {
      Write-Host "[$Label] [$current/$total] $relative"
    }
  }
  Write-Progress -Activity "Copying $Label" -Completed
  Write-Host "[$Label] Complete."
}

$destination = Get-FullPath $DestinationRoot
if (Test-Path -LiteralPath $destination) {
  $existingRoot = @(Get-ChildItem -LiteralPath $destination -Force -ErrorAction SilentlyContinue)
  if ($existingRoot.Count -gt 0) {
    throw "Final submission root must be new or empty: $destination"
  }
}
New-Item -ItemType Directory -Path $destination -Force | Out-Null

$productDestination = Join-Path $destination $ProductFolderName
$sourceDestination = Join-Path $destination $SourceFolderName
$introDestination = Join-Path $destination $IntroFolderName

Copy-TreeWithProgress $ProductSource $productDestination "product" "product"
Copy-TreeWithProgress $SourcePackageSource $sourceDestination "source" "source"
Copy-TreeWithProgress $IntroSource $introDestination "intro" "intro"

foreach ($path in @("data\ai", "data\knowledge", "data\rag", "logs", "run")) {
  New-Item -ItemType Directory -Path (Join-Path $productDestination $path) -Force | Out-Null
}

Write-Host "Final submission copy completed at $destination"
