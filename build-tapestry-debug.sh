#!/bin/bash

echo "Building Tapestry Platform (DEBUG VERSION)..."

./gradlew downloadMikel exportTypes
if [ $? -ne 0 ]; then
    echo "Tapestry build failed!"
    exit 1
fi

echo "Renaming JAR with debug suffix..."
mv -f "build/libs/tapestry-0.0.1.jar" "build/libs/tapestry-0.0.1-debug.jar"
if [ $? -ne 0 ]; then
    echo "Failed to rename JAR!"
    exit 1
fi

echo ""
echo "✅ Tapestry DEBUG build completed!"
echo "📦 Debug JAR: tapestry-0.0.1-debug.jar"
echo "🔍 Enhanced logging enabled for dependent mods"
echo ""
