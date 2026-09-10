@echo off
echo ============================================================
echo   Citizen AI Backend Startup Script (Running from D: Drive)
echo ============================================================

REM Set Node.js path (Playwright's bundled Node)
set NODE="C:\Users\Urmila Dhumal\AppData\Local\ms-playwright-go\1.57.0\node.exe"

REM Check if MongoDB is running on port 27017
echo [1/3] Checking MongoDB...
netstat -an | findstr "27017" >nul 2>&1
if errorlevel 1 (
  echo MongoDB is not running. Starting MongoDB from D: drive...
  if not exist "D:\Citizen\data\db" mkdir "D:\Citizen\data\db"
  start "" /b "D:\mongodb-win32-x86_64-windows-8.3.2\bin\mongod.exe" --dbpath "D:\Citizen\data\db" --port 27017
  timeout /t 3 >nul
)

echo [OK] MongoDB is active on port 27017.

REM Go to backend directory
cd /d "d:\Citizen\citizen-ai-backend"

REM Seed admin accounts (only creates if missing)
echo [2/3] Checking database seed...
%NODE% scripts/seedAdmin.js

REM Start the server
echo [3/3] Starting Citizen AI Backend on port 8000...
%NODE% src/server.js
