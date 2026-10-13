@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Homura Hotel - He thong Quan ly Khach san
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build.ps1"
if errorlevel 1 (
    echo [!] Build failed. Check the error above.
    pause
    exit /b 1
)
start "Homura Hotel" javaw -cp "bin;lib/*" main.Main
exit /b 0
