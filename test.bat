@echo off
setlocal
cd /d "%~dp0"
if not exist "out" mkdir "out"
rem Compile tests and dependencies without using a Windows-path @argument file.
javac -encoding UTF-8 --release 17 -sourcepath "src\main\java;src\test\java" -d "out" "src\test\java\com\campusfix\SmokeTest.java"
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -ea -cp "out" com.campusfix.SmokeTest
pause
