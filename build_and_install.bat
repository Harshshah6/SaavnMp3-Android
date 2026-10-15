@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Melotune - Build and Install via ADB

echo =====================================================================
echo              Melotune - Build and Install via ADB
echo =====================================================================
echo:

cd /d "%~dp0"

rem Resolve ADB
set "ADB_CMD=adb"
where adb >nul 2>&1
if %ERRORLEVEL% equ 0 goto adb_found

if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    goto adb_found
)
if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    set "ADB_CMD=%ANDROID_HOME%\platform-tools\adb.exe"
    goto adb_found
)
if exist "%USERPROFILE%\Desktop\Scrcpy\adb.exe" (
    set "ADB_CMD=%USERPROFILE%\Desktop\Scrcpy\adb.exe"
    goto adb_found
)

echo [ERROR] ADB was not found in PATH or standard Android SDK directories.
echo Please ensure Android Platform Tools / ADB is installed and added to PATH.
echo:
pause
exit /b 1

:adb_found
rem Check connected devices
echo [*] Checking connected ADB devices...
"%ADB_CMD%" devices
set "CONNECTED_DEVICE="

for /f "skip=1 tokens=1,2" %%A in ('"%ADB_CMD%" devices') do (
    if "%%B"=="device" (
        if not defined CONNECTED_DEVICE set "CONNECTED_DEVICE=%%A"
    )
)

if defined CONNECTED_DEVICE goto device_ready

echo:
echo [WARNING] No authorized device or emulator detected via ADB.
echo Please ensure USB Debugging is turned on and your device is authorized.
echo:
pause
exit /b 1

:device_ready
echo [+] Target device ready: !CONNECTED_DEVICE!
echo:

rem Parse arguments
set "BUILD_TYPE=debug"
set "SKIP_BUILD=0"
set "LAUNCH_APP=1"

:arg_loop
if "%~1"=="" goto after_args
if /i "%~1"=="--release" set "BUILD_TYPE=release"
if /i "%~1"=="--debug" set "BUILD_TYPE=debug"
if /i "%~1"=="--skip-build" set "SKIP_BUILD=1"
if /i "%~1"=="--no-launch" set "LAUNCH_APP=0"
shift
goto arg_loop

:after_args
if "%SKIP_BUILD%"=="1" goto skip_build
goto do_build

:skip_build
echo [*] Skipping build step (--skip-build requested).
goto locate_apk

:do_build
echo [*] Building %BUILD_TYPE% APK with gradlew.bat...
if /i "%BUILD_TYPE%"=="release" (
    call gradlew.bat assembleRelease
) else (
    call gradlew.bat assembleDebug
)

if %ERRORLEVEL% equ 0 goto locate_apk

echo:
echo [ERROR] Gradle build failed with exit code %ERRORLEVEL%.
echo:
pause
exit /b %ERRORLEVEL%

:locate_apk
echo:
echo [*] Locating output APK...
set "APK_PATH="
set "SEARCH_DIR=app\build\outputs\apk\!BUILD_TYPE!"

if not exist "!SEARCH_DIR!" (
    echo [ERROR] Directory does not exist: !SEARCH_DIR!
    pause
    exit /b 1
)

for %%F in ("!SEARCH_DIR!\*.apk") do (
    set "APK_PATH=%%~fF"
)

if defined APK_PATH goto install_apk

echo [ERROR] No APK file found in !SEARCH_DIR!
pause
exit /b 1

:install_apk
echo [+] Found APK: "!APK_PATH!"
echo:

echo [*] Installing to device !CONNECTED_DEVICE!...
"%ADB_CMD%" -s !CONNECTED_DEVICE! install -r -d -t "!APK_PATH!"

if %ERRORLEVEL% equ 0 goto install_success

echo:
echo [WARNING] Direct installation failed.
echo Attempting clean reinstall by removing existing package first...
"%ADB_CMD%" -s !CONNECTED_DEVICE! uninstall com.harsh.shah.saavnmp3
"%ADB_CMD%" -s !CONNECTED_DEVICE! install -r -d -t "!APK_PATH!"

if %ERRORLEVEL% equ 0 goto install_success

echo:
echo [ERROR] Installation failed.
pause
exit /b 1

:install_success
echo:
echo [+] APK successfully installed!

if "%LAUNCH_APP%"=="0" goto finished

echo:
echo [*] Launching Melotune...
"%ADB_CMD%" -s !CONNECTED_DEVICE! shell am start -n com.harsh.shah.saavnmp3/com.harsh.shah.saavnmp3.activities.MainActivity
echo [+] Melotune started.

:finished
echo:
echo =====================================================================
echo                        Success!
echo =====================================================================
echo:
pause
