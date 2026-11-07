@echo off
rem Wrapper so the QA helper can be run from cmd.exe or by double-clicking.
rem Usage: tools\device-qa.cmd [-Apk "C:\path\to.apk"] [-Adb "C:\path\to\adb.exe"]
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0device-qa.ps1" %*
set "EXITCODE=%ERRORLEVEL%"
if not "%EXITCODE%"=="0" (
    echo.
    echo device-qa failed with exit code %EXITCODE%.
    pause
)
exit /b %EXITCODE%
