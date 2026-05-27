@echo off
echo === Compiling CleanupP107 ===
javac -encoding UTF-8 -cp "bin;lib/*;mssql-jdbc-12.10.0.jre11.jar" -d scratch scratch\CleanupP107.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compile failed!
    pause
    exit /b 1
)
echo === Running CleanupP107 ===
java -cp "bin;lib/*;mssql-jdbc-12.10.0.jre11.jar;scratch" CleanupP107
pause
