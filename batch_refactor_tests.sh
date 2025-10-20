#!/bin/bash

# Batch Refactoring Script for Unit Tests
# Adds package declarations to all Java test files in Unit_Tests directory
# Usage: ./batch_refactor_tests.sh

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Unit Tests Batch Refactoring Script${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Navigate to the Unit_Tests directory
cd "/Users/aponduga/Desktop/Personal/Strict_Dev/Unit_Tests" || exit 1

# Counter for files processed
total_files=0
updated_files=0
skipped_files=0

# Process each Java file
for file in *.java; do
    # Skip if not a file
    if [[ ! -f "$file" ]]; then
        continue
    fi
    
    ((total_files++))
    
    echo -e "${YELLOW}Processing:${NC} $file"
    
    # Check if file already has a package declaration
    if grep -q "^package " "$file"; then
        echo -e "  ${GREEN}✓${NC} Already has package declaration - skipping"
        ((skipped_files++))
    else
        # Create temp file with package declaration
        {
            echo "package unittests;"
            echo ""
            cat "$file"
        } > "$file.tmp"
        
        # Replace original file
        mv "$file.tmp" "$file"
        
        echo -e "  ${GREEN}✓${NC} Added package declaration"
        ((updated_files++))
    fi
done

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Refactoring Summary${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Total files processed: ${total_files}"
echo -e "Files updated:         ${GREEN}${updated_files}${NC}"
echo -e "Files skipped:         ${YELLOW}${skipped_files}${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review the changes with: git diff Unit_Tests/"
echo "2. Compile tests with: mvn clean test-compile"
echo "3. Run tests with: mvn test"
echo ""
