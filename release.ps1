param(
    [switch]$Major
)

$ErrorActionPreference = "Stop"

# Load environment variables from .env file if it exists
$envPath = Join-Path $PSScriptRoot ".env"
if (Test-Path $envPath) {
    Get-Content $envPath | Where-Object { $_ -match '=' -and -not $_.StartsWith('#') } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        $name = $name.Trim()
        $value = $value.Trim()
        if (-not [string]::IsNullOrEmpty($name)) {
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

Write-Host "Starting Version Bump and Build Release Automation..." -ForegroundColor Cyan

# 1. File Paths
$gradlePath = Join-Path $PSScriptRoot "app\build.gradle.kts"
$ciPath = Join-Path $PSScriptRoot ".gitlab-ci.yml"
$gitlabMdPath = Join-Path $PSScriptRoot "GITLAB.md"
$releasesDir = Join-Path $PSScriptRoot "releases"
$apkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
$zipPath = Join-Path $PSScriptRoot "releases\app-debug.zip"

# 2. Read and Parse build.gradle.kts
if (-not (Test-Path $gradlePath)) {
    Write-Error "Could not find build.gradle.kts at $gradlePath"
}

$gradleContent = Get-Content $gradlePath -Raw

# Extract versionCode
if ($gradleContent -match 'versionCode\s*=\s*(\d+)') {
    $oldVersionCode = [int]$Matches[1]
    $newVersionCode = $oldVersionCode + 1
} else {
    Write-Error "Could not find versionCode in build.gradle.kts"
}

# Extract versionName
if ($gradleContent -match 'versionName\s*=\s*"([^"]+)"') {
    $oldVersionName = $Matches[1]
} else {
    Write-Error "Could not find versionName in build.gradle.kts"
}

# Parse old version name to calculate new version name (e.g. "1.3" -> Major: 1, Minor: 3)
$versionParts = $oldVersionName.Split('.')
if ($versionParts.Length -lt 2) {
    Write-Error "versionName format is not valid (expected X.Y or X.Y.Z, got $oldVersionName)"
}

$majorVer = [int]$versionParts[0]
$minorVer = [int]$versionParts[1]

if ($Major) {
    $newMajorVer = $majorVer + 1
    $newVersionName = "$newMajorVer.0"
} else {
    $newMinorVer = $minorVer + 1
    $newVersionName = "$majorVer.$newMinorVer"
}

Write-Host "Bumping version from v$oldVersionName (Code $oldVersionCode) to v$newVersionName (Code $newVersionCode)..." -ForegroundColor Yellow

# Replace values in the build configuration string
$newGradleContent = $gradleContent -replace "versionCode\s*=\s*$oldVersionCode", "versionCode = $newVersionCode"
$newGradleContent = $newGradleContent -replace ('versionName\s*=\s*"' + [regex]::Escape($oldVersionName) + '"'), ('versionName = "' + $newVersionName + '"')

# Backup existing config and write new one
Copy-Item $gradlePath "$gradlePath.bak" -Force
[System.IO.File]::WriteAllText($gradlePath, $newGradleContent)

# 3. Update .gitlab-ci.yml (if exists)
$ciBackup = $null
if (Test-Path $ciPath) {
    $ciContent = Get-Content $ciPath -Raw
    $ciBackup = $ciContent
    $newCiContent = $ciContent -replace "/invoice-generator/$oldVersionName/InvoiceGenerator_v${oldVersionName}_debug.apk", "/invoice-generator/$newVersionName/InvoiceGenerator_v${newVersionName}_debug.apk"
    [System.IO.File]::WriteAllText($ciPath, $newCiContent)
    Write-Host "Synchronized .gitlab-ci.yml to version v$newVersionName" -ForegroundColor Green
}

# 4. Update GITLAB.md (if exists)
$mdBackup = $null
if (Test-Path $gitlabMdPath) {
    $mdContent = Get-Content $gitlabMdPath -Raw
    $mdBackup = $mdContent
    $newMdContent = $mdContent -replace "/invoice-generator/$oldVersionName/InvoiceGenerator_v${oldVersionName}_debug.apk", "/invoice-generator/$newVersionName/InvoiceGenerator_v${newVersionName}_debug.apk"
    [System.IO.File]::WriteAllText($gitlabMdPath, $newMdContent)
    Write-Host "Synchronized GITLAB.md to version v$newVersionName" -ForegroundColor Green
}

# 5. Compile App using Gradle
Write-Host "Running Gradle build (assembleDebug)..." -ForegroundColor Yellow
$buildResult = Start-Process powershell -ArgumentList '-NoProfile -Command ".\gradlew.bat assembleDebug"' -WorkingDirectory $PSScriptRoot -PassThru -NoNewWindow -Wait

if ($buildResult.ExitCode -ne 0) {
    Write-Host "Build failed! Reverting configurations to their previous state..." -ForegroundColor Red
    Move-Item "$gradlePath.bak" $gradlePath -Force
    if ($ciBackup -ne $null) {
        [System.IO.File]::WriteAllText($ciPath, $ciBackup)
    }
    if ($mdBackup -ne $null) {
        [System.IO.File]::WriteAllText($gitlabMdPath, $mdBackup)
    }
    Exit 1
}

# Clean backup file on success
if (Test-Path "$gradlePath.bak") {
    Remove-Item "$gradlePath.bak" -Force
}

# 6. Compress/Zip and copy the raw APK into releases
Write-Host "Packaging build artifacts..." -ForegroundColor Yellow
if (-not (Test-Path $releasesDir)) {
    New-Item -ItemType Directory -Path $releasesDir -Force | Out-Null
}

if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

# Clean old APKs from releases folder
Get-ChildItem $releasesDir -Filter "InvoiceGenerator_v*_debug.apk" | Remove-Item -Force

if (-not (Test-Path $apkPath)) {
    Write-Error "Could not find compiled APK at $apkPath"
}

# Copy raw APK to releases folder
$targetApkPath = Join-Path $releasesDir "InvoiceGenerator_v${newVersionName}_debug.apk"
Copy-Item $apkPath $targetApkPath -Force
Write-Host "Copied raw APK to: releases/InvoiceGenerator_v${newVersionName}_debug.apk" -ForegroundColor Green

# Compress APK to zip
Compress-Archive -Path $apkPath -DestinationPath $zipPath

Write-Host ('Successfully released v' + $newVersionName + ' (Code ' + $newVersionCode + ') at ''releases/app-debug.zip''!') -ForegroundColor Green

# 7. Optional: Upload to GitLab Package Registry (Packages section)
$gitlabToken = $env:GITLAB_TOKEN
$gitlabProjectId = $env:GITLAB_PROJECT_ID

if (-not [string]::IsNullOrEmpty($gitlabToken) -and -not [string]::IsNullOrEmpty($gitlabProjectId)) {
    Write-Host "Uploading APK to GitLab Package Registry (Packages section)..." -ForegroundColor Yellow
    $uri = "https://gitlab.com/api/v4/projects/$gitlabProjectId/packages/generic/invoice-generator/$newVersionName/InvoiceGenerator_v${newVersionName}_debug.apk"
    $headers = @{
        "PRIVATE-TOKEN" = $gitlabToken
    }
    try {
        $fileBytes = [System.IO.File]::ReadAllBytes($apkPath)
        # Suppress output progress to speed up and prevent logs cluttering
        $uploadResult = Invoke-RestMethod -Uri $uri -Headers $headers -Method Put -Body $fileBytes -ContentType "application/vnd.android.package-archive"
        Write-Host "Successfully uploaded to GitLab Package Registry Packages section!" -ForegroundColor Green
    } catch {
        Write-Warning "Failed to upload to GitLab Package Registry: $_"
    }
} else {
    Write-Host "GitLab credentials not found (GITLAB_TOKEN & GITLAB_PROJECT_ID). Skipping Packages upload." -ForegroundColor DarkGray
}
