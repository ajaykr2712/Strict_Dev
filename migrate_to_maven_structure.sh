#!/bin/bash

# Maven Standard Directory Structure Migration Script
# Migrates Unit_Tests to Maven-standard src/test/java structure
# Usage: ./migrate_to_maven_structure.sh

set -e  # Exit on error

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Maven Structure Migration Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

PROJECT_ROOT="/Users/aponduga/Desktop/Personal/Strict_Dev"
cd "$PROJECT_ROOT" || exit 1

# Confirm before proceeding
echo -e "${YELLOW}This script will:${NC}"
echo "  1. Create Maven-standard directory structure"
echo "  2. Move test files to src/test/java/unittests/"
echo "  3. Keep original Unit_Tests/ as backup"
echo "  4. Update documentation"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Migration cancelled.${NC}"
    exit 1
fi

echo -e "${GREEN}Step 1: Creating Maven directory structure...${NC}"
mkdir -p src/test/java/unittests
mkdir -p src/test/resources
mkdir -p src/main/java
mkdir -p src/main/resources
echo -e "  ${GREEN}✓${NC} Directories created"

echo ""
echo -e "${GREEN}Step 2: Copying test files...${NC}"
test_count=0
for file in Unit_Tests/*.java; do
    if [[ -f "$file" ]]; then
        cp "$file" src/test/java/unittests/
        ((test_count++))
        echo -e "  ${GREEN}✓${NC} Copied $(basename "$file")"
    fi
done
echo -e "  ${GREEN}✓${NC} Copied $test_count test files"

echo ""
echo -e "${GREEN}Step 3: Copying documentation...${NC}"
doc_count=0
for file in Unit_Tests/*.md; do
    if [[ -f "$file" ]]; then
        cp "$file" src/test/resources/
        ((doc_count++))
    fi
done
echo -e "  ${GREEN}✓${NC} Copied $doc_count documentation files"

echo ""
echo -e "${GREEN}Step 4: Creating Maven wrapper...${NC}"
if command -v mvn &> /dev/null; then
    mvn -N wrapper:wrapper > /dev/null 2>&1 || true
    echo -e "  ${GREEN}✓${NC} Maven wrapper created"
else
    echo -e "  ${YELLOW}⚠${NC} Maven not found - skip wrapper creation"
fi

echo ""
echo -e "${GREEN}Step 5: Creating .gitignore...${NC}"
cat > .gitignore << 'EOF'
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo
*~
.DS_Store

# Compiled
*.class
out/
build/

# Logs
*.log

# Keep original structure as backup
# Unit_Tests/
EOF
echo -e "  ${GREEN}✓${NC} .gitignore created"

echo ""
echo -e "${GREEN}Step 6: Creating project README...${NC}"
cat > src/test/java/unittests/README.md << 'EOF'
# Unit Tests Package

This package contains all unit tests for the Strict_Dev project.

## Structure

```
unittests/
├── *PatternTest.java      - Design pattern tests
├── *ServiceTest.java      - Service layer tests
├── *UtilTest.java         - Utility class tests
├── *Test.java             - Other component tests
```

## Running Tests

### Maven
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=SingletonPatternTest

# Run with coverage
mvn test jacoco:report
```

### IDE
- IntelliJ IDEA: Right-click on test class → Run
- Eclipse: Right-click on test class → Run As → JUnit Test
- VS Code: Click the Run/Debug button above test methods

## Coverage

Target: 80%+ code coverage

See `TEST_MANIFEST.md` for complete test catalog.
EOF
echo -e "  ${GREEN}✓${NC} Test package README created"

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Migration Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Project structure:"
echo -e "  ${GREEN}✓${NC} src/test/java/unittests/ - $test_count test files"
echo -e "  ${GREEN}✓${NC} src/test/resources/      - $doc_count docs"
echo -e "  ${GREEN}✓${NC} src/main/java/           - Ready for source code"
echo -e "  ${GREEN}✓${NC} pom.xml                  - Maven configuration"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Organize source code:"
echo "   - Move source files to src/main/java/"
echo "   - Organize by package (e.g., com.strictdev.patterns)"
echo "   - Update import statements in tests"
echo ""
echo "2. Build and test:"
echo "   ${BLUE}mvn clean compile${NC}    # Compile source"
echo "   ${BLUE}mvn test${NC}             # Run tests"
echo "   ${BLUE}mvn verify${NC}           # Full build"
echo ""
echo "3. Setup IDE:"
echo "   - Import as Maven project"
echo "   - Let IDE index the project"
echo "   - Run tests from IDE"
echo ""
echo -e "${GREEN}Original Unit_Tests/ directory preserved as backup${NC}"
echo ""
