#!/bin/bash

# Meta GraphQL API Gateway Demo
# This script compiles and runs the GraphQL Gateway implementation

echo "===================================="
echo "Meta GraphQL API Gateway Demo"
echo "===================================="
echo ""

# Set the directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Clean previous builds
echo "Cleaning previous builds..."
rm -f *.class

# Compile
echo "Compiling GraphQL Gateway..."
javac GraphQLGatewayDemo.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "Running demo..."
    echo "----------------------------------------"
    java GraphQLGatewayDemo
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
