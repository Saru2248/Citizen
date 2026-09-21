@echo off
REM ==============================================================================
REM CitizenAI - Developer Port 8000 Firewall Helper Script
REM
REM PURPOSE:
REM Allows physical Android devices on the same Wi-Fi network to connect
REM to the local Node.js backend on port 8000.
REM
REM USAGE:
REM Right-click this file and select "Run as administrator".
REM
REM ACTIONS PERFORMED:
REM 1. Adds an inbound firewall rule named "Citizen AI Backend Port 8000" for TCP port 8000.
REM 2. Checks current Wi-Fi network profile (Private vs Public).
REM ==============================================================================

echo.
echo ==============================================================================
echo   CitizenAI - Local LAN Firewall Configuration Tool
echo ==============================================================================
echo.

REM Verify Administrative Privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] This script requires Administrator privileges.
    echo Please right-click setup_firewall.bat and select "Run as administrator".
    echo.
    pause
    exit /b 1
)

echo [1/2] Adding inbound firewall rule for TCP port 8000...
netsh advfirewall firewall delete rule name="Citizen AI Backend Port 8000" >nul 2>&1
netsh advfirewall firewall add rule name="Citizen AI Backend Port 8000" dir=in action=allow protocol=TCP localport=8000 profile=any >nul 2>&1

if %errorLevel% equ 0 (
    echo   [SUCCESS] Firewall rule created: Inbound TCP 8000 allowed on all profiles.
) else (
    echo   [WARNING] Could not create firewall rule. Check Windows Defender settings.
)

echo.
echo [2/2] Checking Network Profile status...
powershell -NoProfile -Command "Get-NetConnectionProfile | Select-Object Name, InterfaceAlias, NetworkCategory"

echo.
echo ==============================================================================
echo   Firewall setup completed!
echo   Physical devices on the same Wi-Fi network can now reach port 8000.
echo ==============================================================================
echo.
pause
