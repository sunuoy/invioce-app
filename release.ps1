param(
    [switch]$Major
)

$ErrorActionPreference = "Stop"

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

# 6. Compress/Zip the new APK and replace the old one
Write-Host "Packaging build artifacts..." -ForegroundColor Yellow
if (-not (Test-Path $releasesDir)) {
    New-Item -ItemType Directory -Path $releasesDir -Force | Out-Null
}

if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

if (-not (Test-Path $apkPath)) {
    Write-Error "Could not find compiled APK at $apkPath"
}

Compress-Archive -Path $apkPath -DestinationPath $zipPath

Write-Host "Successfully released v$newVersionName (Code $newVersionCode) at 'releases/app-debug.zip'!" -ForegroundColor Green
