@echo off
setlocal
if not exist "%~dp0build\logs" mkdir "%~dp0build\logs"
set "LAB_LAUNCH_LOG=%~dp0build\logs\launcher-%RANDOM%-%RANDOM%.log"
echo Minecraft Shader Lab - starting...
echo Log: "%LAB_LAUNCH_LOG%"
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %* > "%LAB_LAUNCH_LOG%" 2>&1
set "LAB_EXIT_CODE=%ERRORLEVEL%"
type "%LAB_LAUNCH_LOG%"
if not "%LAB_EXIT_CODE%"=="0" (
    echo.
    echo Startup failed. See the log above.
    if "%~1"=="" pause
)
exit /b %LAB_EXIT_CODE%
