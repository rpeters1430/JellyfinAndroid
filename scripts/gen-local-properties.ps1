#!/usr/bin/env pwsh
param()

$sdkDir = $env:ANDROID_SDK_ROOT
if (-not $sdkDir -or $sdkDir -eq "") {
  $sdkDir = $env:ANDROID_HOME
}

if (-not $sdkDir -or $sdkDir -eq "") {
  Write-Error "ERROR: ANDROID_SDK_ROOT or ANDROID_HOME is not set."
  exit 1
}

$localPropertiesPath = "local.properties"
$existingLines = @()

if (Test-Path $localPropertiesPath) {
  $existingLines = Get-Content $localPropertiesPath
}

# Keep existing values but replace sdk.dir with the latest path.
$filteredLines = $existingLines | Where-Object { $_ -notmatch '^sdk\.dir=' }
$newLines = @("sdk.dir=$sdkDir") + $filteredLines

$hasGoogleAiApiKey = $false
foreach ($line in $newLines) {
  if ($line -match '^GOOGLE_AI_API_KEY=') {
    $hasGoogleAiApiKey = $true
    break
  }
}

if (-not $hasGoogleAiApiKey) {
  $newLines += ""
  $newLines += "## Google AI API key for Gemini cloud fallback"
  $newLines += "## Get your key from: https://aistudio.google.com/apikey"
  $newLines += "GOOGLE_AI_API_KEY="
}

$newLines | Set-Content -Path $localPropertiesPath -Encoding UTF8
Write-Host "Updated local.properties with sdk.dir=$sdkDir (existing entries preserved)"
