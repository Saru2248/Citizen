@echo off
title Citizen AI - Web Admin (Port 3000)
cd /d "D:\Citizen\Citizen web admin"
echo Starting Web Admin Frontend on port 3000...
where node >nul 2>nul
if %errorlevel% equ 0 (
    node node_modules\vite\bin\vite.js --port 3000
) else (
    "C:\Users\Urmila Dhumal\AppData\Local\ms-playwright-go\1.57.0\node.exe" node_modules\vite\bin\vite.js --port 3000
)
pause
