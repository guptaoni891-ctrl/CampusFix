@echo off
setlocal
cd /d "%~dp0"
if not exist "out" mkdir "out"
rem Compile the main entry point and automatically compile all referenced classes.
rem Avoid Java @argument files with Windows backslashes (Java treats them as escapes).
javac -encoding UTF-8 --release 17 -sourcepath "src\main\java" -d "out" "src\main\java\com\campusfix\CampusFixApp.java"
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -cp "out" com.campusfix.CampusFixApp --file %*
if errorlevel 1 (
    echo CampusFix exited with an error.
    pause
    exit /b 1
)
