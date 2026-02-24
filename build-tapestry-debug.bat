@echo off
echo Building Tapestry Platform (DEBUG VERSION)...

call gradlew.bat downloadMikel exportTypes
if %ERRORLEVEL% NEQ 0 (
    echo Tapestry build failed!
    pause
    exit /b 1
)

echo Renaming JAR with debug suffix...
rename "build\libs\tapestry-0.0.1.jar" "tapestry-0.0.1-debug.jar"
if %ERRORLEVEL% NEQ 0 (
    echo Failed to rename JAR!
    pause
    exit /b 1
)

echo.
echo ✅ Tapestry DEBUG build completed!
echo 📦 Debug JAR: tapestry-0.0.1-debug.jar
echo 🔍 Enhanced logging enabled for dependent mods
echo.
pause
