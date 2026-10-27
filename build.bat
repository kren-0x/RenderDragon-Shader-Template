@echo off
setlocal
cd /d "%~dp0"

where /q python || (
    echo [ERROR] Python not found. Please install Python 3.9+ and add it to PATH.
    pause
    exit /b 1
)

python tools\build.py %*
if errorlevel 1 (
    if "%~1"=="" pause
    exit /b %errorlevel%
)
endlocal
