@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set "JAVA_CMD=java"
set "JAVAC_CMD=javac"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
)

"%JAVAC_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo A JDK 17 or newer is required. Set JAVA_HOME or add javac to PATH.
    exit /b 1
)

if not exist "out" mkdir "out"
> "out\main-sources.txt" (
    for /r "src\main\java" %%F in (*.java) do (
        set "SOURCE_FILE=%%F"
        echo "!SOURCE_FILE:\=/!"
    )
)
"%JAVAC_CMD%" -encoding UTF-8 --release 17 -d "out" @"out\main-sources.txt"
if errorlevel 1 (
    echo Compilation failed. Confirm JAVA_HOME points to JDK 17 or newer.
    exit /b 1
)
"%JAVA_CMD%" -cp "out" com.campusfix.CampusFixApp --file %*
if errorlevel 1 (
    echo CampusFix exited with an error.
    exit /b 1
)
exit /b 0
