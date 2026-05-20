@echo off
echo ============================================================
echo      Compiling and Packaging Hotel Ohno...
echo ============================================================

if not exist bin mkdir bin

echo [+] Compiling source files...
javac -encoding UTF-8 -d bin -cp "lib/*;mssql-jdbc-12.10.0.jre11.jar;src" @sources.txt
if %ERRORLEVEL% neq 0 (
    echo [x] Error: Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo [V] Compilation successful!

:: Locate jar.exe
set "JAR_EXE=jar"
if exist "C:\Program Files\Java\jdk-17\bin\jar.exe" (
    set "JAR_EXE=C:\Program Files\Java\jdk-17\bin\jar.exe"
) else (
    for /d %%d in ("C:\Program Files\Java\jdk-*") do (
        if exist "%%d\bin\jar.exe" set "JAR_EXE=%%d\bin\jar.exe"
    )
)

echo [+] Creating Manifest file...
(
echo Manifest-Version: 1.0
echo Main-Class: main.Main
echo Class-Path: mssql-jdbc-12.10.0.jre11.jar lib/poi-5.2.3.jar lib/poi-ooxml-5.2.3.jar lib/poi-ooxml-lite-5.2.3.jar lib/xmlbeans-5.1.1.jar lib/commons-codec-1.15.jar lib/commons-collections4-4.4.jar lib/commons-compress-1.21.jar lib/commons-io-2.11.0.jar lib/curvesapi-1.07.jar lib/itextpdf-5.5.13.3.jar lib/log4j-api-2.18.0.jar lib/log4j-core-2.18.0.jar lib/SparseBitSet-1.2.jar
echo.
) > manifest.txt

echo [+] Packaging JAR file using: "%JAR_EXE%"
"%JAR_EXE%" cfm HotelOhno.jar manifest.txt -C bin .
if %ERRORLEVEL% neq 0 (
    echo [x] Error: JAR packaging failed!
    del manifest.txt
    pause
    exit /b %ERRORLEVEL%
)

del manifest.txt

echo ============================================================
echo [V] PACKAGING SUCCESSFUL!
echo Created: HotelOhno.jar
echo Note: When moving HotelOhno.jar to another folder/machine,
echo you must copy these items along with it:
echo - lib/ folder
echo - icon/, img/, resources/ folders
echo - mssql-jdbc-12.10.0.jre11.jar
echo ============================================================
pause
