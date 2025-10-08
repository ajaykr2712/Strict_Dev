#!/bin/bash

# Amazon Distributed Cache System Demo
# This script compiles and runs the Distributed Cache implementation

echo "===================================="
echo "Amazon Distributed Cache System Demo"
echo "===================================="
echo ""

# Set the directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Clean previous builds
echo "Cleaning previous builds..."
rm -f *.class

# Compile
echo "Compiling Distributed Cache System..."
javac DistributedCacheSystemDemo.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "Running demo..."
    echo "----------------------------------------"
    java DistributedCacheSystemDemo
    echo "----------------------------------------"
    echo ""
    echo "Demo completed successfully!"
else
    echo "Compilation failed!"
    exit 1
fi

# Clean up class files
echo ""
echo "Cleaning up..."
rm -f *.class

echo "Done!"
