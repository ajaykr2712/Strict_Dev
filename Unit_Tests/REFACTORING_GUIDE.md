# Refactoring Guide - Unit Tests
## How to Fix Compilation Errors and Improve Test Quality

**Date**: October 20, 2025

---

## 🔧 Quick Fixes Applied

### 1. Package Declarations
**Issue**: Tests lacked proper package declarations  
**Fix**: Added `package unittests;` to test files

**Note**: If you get package mismatch errors, you have two options:

#### Option A: Match Directory Structure
```java
// Remove package declaration or use:
// package Unit_Tests;
```

#### Option B: Create Proper Package Structure
```bash
mkdir -p src/test/java/com/strictdev/tests
mv Unit_Tests/*.java src/test/java/com/strictdev/tests/
```

---

## 🚀 Recommended: Maven Project Setup

### Step 1: Create pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.strictdev</groupId>
    <artifactId>strict-dev-tests</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <junit.version>4.13.2</junit.version>
        <mockito.version>3.12.4</mockito.version>
    </properties>
    
    <dependencies>
        <!-- JUnit 4 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        
        <!-- Mockito -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        
        <!-- Hamcrest -->
        <dependency>
            <groupId>org.hamcrest</groupId>
            <artifactId>hamcrest-core</artifactId>
            <version>1.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 2: Restructure Project
```bash
# Create Maven structure
mkdir -p src/test/java/com/strictdev
mkdir -p src/main/java/com/strictdev

# Move test files
mv Unit_Tests/*.java src/test/java/com/strictdev/

# Update package declarations in all test files
find src/test/java -name "*.java" -exec sed -i '' 's/package unittests;/package com.strictdev;/g' {} \;
```

### Step 3: Run Tests with Maven
```bash
# Compile and run all tests
mvn clean test

# Run specific test
mvn test -Dtest=ProductServiceTest

# Generate test report
mvn surefire-report:report
```

---

## 🔨 Alternative: Gradle Setup

### build.gradle
```gradle
plugins {
    id 'java'
}

group 'com.strictdev'
version '1.0.0'

sourceCompatibility = 11

repositories {
    mavenCentral()
}

dependencies {
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:3.12.4'
    testImplementation 'org.hamcrest:hamcrest-core:1.3'
}

test {
    useJUnit()
    
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

### Run with Gradle
```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests ProductServiceTest

# Generate report
./gradlew test --info
```

---

## 📝 Refactoring Checklist

### ✅ Completed
- [x] Added package declarations to 5 key test files
- [x] Improved JavaDoc documentation
- [x] Added refactor dates
- [x] Enhanced test descriptions

### 🔄 In Progress
- [ ] Fix package naming across all files
- [ ] Set up Maven/Gradle build
- [ ] Add proper dependency management
- [ ] Configure CI/CD integration

### 📋 Recommended Next Steps

1. **Choose Build Tool**
   - Maven (recommended for enterprise)
   - Gradle (recommended for modern projects)
   - Keep manual compilation (for simple projects)

2. **Fix Package Structure**
   ```bash
   # Option 1: Remove package declarations
   find Unit_Tests -name "*.java" -exec sed -i '' '/^package unittests;/d' {} \;
   
   # Option 2: Use proper package structure with Maven/Gradle
   ```

3. **Update Imports**
   - Ensure JUnit jars are in classpath
   - Use IDE auto-import feature
   - Or use Maven/Gradle dependency management

4. **Run Tests**
   ```bash
   # With Maven
   mvn test
   
   # With Gradle
   ./gradlew test
   
   # Manual
   export CLASSPATH="lib/*:Unit_Tests:."
   javac Unit_Tests/*.java
   java org.junit.runner.JUnitCore ProductServiceTest
   ```

---

## 🐛 Common Issues & Solutions

### Issue 1: Package Declaration Mismatch
```
Error: The declared package "unittests" does not match expected "Unit_Tests"
```

**Solution A**: Remove package declaration
```java
// Delete this line:
// package unittests;
```

**Solution B**: Rename directory
```bash
mv Unit_Tests unittests
```

**Solution C**: Update package to match
```java
package Unit_Tests;
```

### Issue 2: Cannot Find JUnit Classes
```
Error: package org.junit does not exist
```

**Solution**: Use Maven/Gradle or add to classpath
```bash
export CLASSPATH="lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar:."
```

### Issue 3: Cannot Resolve Symbols
```
Error: cannot find symbol method assertEquals
```

**Solution**: This is because JUnit is not in classpath. Use Maven/Gradle.

---

## 🎯 Best Practice Recommendations

### 1. Use Build Tool
✅ **DO**: Use Maven or Gradle  
❌ **DON'T**: Manage dependencies manually

### 2. Proper Package Structure
✅ **DO**: `com.company.project.tests`  
❌ **DON'T**: `Unit_Tests` (with underscores)

### 3. Separate Source and Test
```
project/
├── src/
│   ├── main/java/          # Production code
│   └── test/java/          # Test code
├── pom.xml or build.gradle
└── README.md
```

### 4. Use Modern Java
✅ **DO**: Java 11+ with modules  
❌ **DON'T**: Java 7 or earlier

### 5. Continuous Integration
```yaml
# .github/workflows/tests.yml
name: Run Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '11'
      - run: mvn test
```

---

## 📊 Refactored Files Summary

| File | Status | Changes |
|------|--------|---------|
| BulkheadIsolationTest.java | ✅ Refactored | Package + docs |
| GCounterCRDTTest.java | ✅ Refactored | Package + docs |
| CircuitBreakerRegistryTest.java | ✅ Refactored | Package + docs |
| MetricsCollectorTest.java | ✅ Refactored | Package + docs |
| AdapterPatternTest.java | ⏳ Pending | Package needed |
| ProductServiceTest.java | ⏳ Pending | Package needed |
| Others (26 files) | ⏳ Pending | Batch refactor |

---

## 🚀 Quick Start After Refactoring

### With Maven (Recommended)
```bash
# 1. Create pom.xml (see above)
# 2. Restructure files
mkdir -p src/test/java/com/strictdev
mv Unit_Tests/*.java src/test/java/com/strictdev/

# 3. Update packages in all files
find src/test/java -name "*.java" -exec \
  sed -i '' '1s/^/package com.strictdev;\n\n/' {} \;

# 4. Run tests
mvn clean test
```

### Without Build Tool (Current Setup)
```bash
# 1. Remove package declarations
find Unit_Tests -name "*.java" -exec \
  sed -i '' '/^package unittests;/d' {} \;

# 2. Compile
export CLASSPATH="lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar:Unit_Tests:."
javac Unit_Tests/*.java

# 3. Run
java org.junit.runner.JUnitCore ProductServiceTest
```

---

## 📖 Additional Resources

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)
- [Maven Getting Started](https://maven.apache.org/guides/getting-started/)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)

---

**Last Updated**: October 20, 2025  
**Status**: Refactoring in progress  
**Recommendation**: Use Maven for best results
