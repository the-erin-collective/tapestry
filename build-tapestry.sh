#!/bin/bash

echo "Building Tapestry Platform..."
cd tapestry
./gradlew downloadMikel exportTypes
if [ $? -ne 0 ]; then
    echo "Tapestry build failed!"
    exit 1
fi
echo "Tapestry built successfully!"
cd ..
