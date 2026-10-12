@echo off
chcp 65001 >nul
title Homura Hotel - He thong Quan ly Khach san

:: Kiem tra neu chua build thu muc bin thi build
if not exist "bin\main\Main.class" (
    echo Dang bien dich ma nguon Homura Hotel...
    if not exist bin mkdir bin
    javac -encoding UTF-8 -d bin -cp "lib/*;src" @sources.txt
    if %ERRORLEVEL% neq 0 (
        echo [!] Loi bien dich! Vui long kiem tra lai.
        pause
        exit /b 1
    )
)

:: Chay ung dung
start javaw -cp "bin;lib/*" main.Main
exit
