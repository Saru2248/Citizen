@echo off
echo ============================================================
echo   Citizen AI Web Admin Frontend Startup Script
echo ============================================================

set NODE="C:\Users\Urmila Dhumal\AppData\Local\ms-playwright-go\1.57.0\node.exe"
set YARN="%APPDATA%\npm\node_modules\yarn\bin\yarn.js"

cd /d "d:\Citizen\Citizen web admin"

echo Starting Vite Dev Server on http://localhost:3000 ...
%NODE% %YARN% dev
