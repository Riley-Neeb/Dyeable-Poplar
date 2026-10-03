@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title Dyeable Poplar build window - leave open while Claude works, close when done

rem This window builds and test-runs the mod. It only ever runs these Gradle tasks:
rem   build, runServer (a quick start-and-stop server check), runClient, runClientGameTest.
rem Claude asks for one by writing its name into request.txt; results go to build-log.txt.

set "JDK_DIR=%~dp0.jdk"
if not exist "%JDK_DIR%\bin\java.exe" call :getjdk
if not exist "%JDK_DIR%\bin\java.exe" (
  echo.
  echo Could not download Java 25. See the messages above.
  pause
  exit /b 1
)
set "JAVA_HOME=%JDK_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist request.txt (echo build> request.txt)
echo Ready. Waiting for build requests. Output goes to build-log.txt
:loop
if exist request.txt (
  set "REQ="
  set /p REQ=<request.txt
  del request.txt
  set "TASKS="
  if /i "!REQ!"=="build" set "TASKS=build"
  if /i "!REQ!"=="server" set "TASKS=build runServer"
  if /i "!REQ!"=="client" set "TASKS=build runClient"
  if /i "!REQ!"=="clienttest" set "TASKS=build runClientGameTest"
  if defined TASKS (
    echo [!time!] Running: !TASKS!
    > build-status.txt echo running !REQ!
    call gradlew.bat !TASKS! --console=plain --stacktrace > build-log.txt 2>&1
    set "RC=!ERRORLEVEL!"
    > build-status.txt echo done !REQ! exit=!RC!
    echo [!time!] Finished: !REQ!, exit code !RC!
  ) else (
    echo [!time!] Ignored unknown request: !REQ!
  )
)
timeout /t 2 /nobreak >nul
goto loop

:getjdk
echo Downloading Java 25 for this project, one time only, about 200 MB...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; $zip=Join-Path $env:TEMP 'dyeable-poplar-jdk25.zip'; Invoke-WebRequest -UseBasicParsing -Uri 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $zip; $tmp=Join-Path $env:TEMP 'dyeable-poplar-jdk25'; if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }; Expand-Archive -Path $zip -DestinationPath $tmp; $inner=Get-ChildItem $tmp | Select-Object -First 1; Move-Item $inner.FullName $env:JDK_DIR; Remove-Item $zip"
exit /b 0
