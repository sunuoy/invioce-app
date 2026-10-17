@echo off
title Starting Android Emulator (emulator-5554)
echo ===================================================
echo     Starting Android Emulator on Port 5554
echo             Device ID: emulator-5554
echo ===================================================

set "EMULATOR_EXE=%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe"

if not exist "%EMULATOR_EXE%" (
    if defined ANDROID_HOME (
        set "EMULATOR_EXE=%ANDROID_HOME%\emulator\emulator.exe"
    )
)

if not exist "%EMULATOR_EXE%" (
    echo [ERROR] Android Emulator executable not found!
    echo Looked in: %LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe
    pause
    exit /b 1
)

:: Choose AVD (Defaults to Pixel_9, fallback to medium_phone)
set "AVD_NAME=Pixel_9"
if "%~1" neq "" set "AVD_NAME=%~1"

echo Launching AVD: %AVD_NAME% on port 5554...
start "" "%EMULATOR_EXE%" -avd %AVD_NAME% -port 5554

echo Emulator started! Waiting for ADB connection...
ping 127.0.0.1 -n 3 >nul 2>&1
exit /b 0
