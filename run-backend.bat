@echo off
title Citizen AI - Backend (Port 8000)
cd /d "D:\Citizen\citizen-ai-backend"
echo Starting Backend Server on port 8000...
where node >nul 2>nul
if %errorlevel% equ 0 (
    node src/server.js
) else (
    "C:\Users\Urmila Dhumal\AppData\Local\ms-playwright-go\1.57.0\node.exe" src/server.js
)
pause
