@echo off
echo Building Tapestry Platform...
cd tapestry
call gradlew.bat downloadMikel build
if %ERRORLEVEL% NEQ 0 (
    echo Tapestry build failed!
    pause
    exit /b 1
)
echo Tapestry built successfully!
cd ..
