@echo off
title Free Port 8888 Helper
cd /d "%~dp0"

echo Checking if port 8888 is in use...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8888 ^| findstr LISTENING') do (
    echo Found process %%a using port 8888. Closing it now...
    taskkill /F /PID %%a
    echo Port 8888 has been successfully freed!
    goto done
)

echo Port 8888 is already free! Nothing to close.

:done
echo.
pause
