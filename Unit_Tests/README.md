# Unit Tests for Strict_Dev Java Codebase

> **🔧 REFACTORING UPDATE (Oct 20, 2025)**: Maven/Gradle setup now available!  
> See **REFACTORING_GUIDE.md** for modern build tool setup and dependency management.

## Quick Start Options

### Option 1: Maven (Recommended) ⭐
```bash
# Use the provided pom.xml in project root
cd /Users/aponduga/Desktop/Personal/Strict_Dev
mvn clean test
```

### Option 2: Manual Compilation
```bash
export CLASSPATH="lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar:Unit_Tests:."
javac Unit_Tests/ProductServiceTest.java
java org.junit.runner.JUnitCore ProductServiceTest
```

---

/**
 * README for Unit Tests
 * 
 * This directory contains comprehensive unit tests for the Strict_Dev codebase.
 * The tests are organized to cover critical components across different layers and patterns.
 * 
 * ## Test Structure
 * 
 * ### Service Layer Tests
 * - ProductServiceTest.java - Tests product management operations
 * - OrderServiceTest.java - Tests order processing workflows
 * - PaymentServiceTest.java - Tests payment processing and validation
 * 
 * ### Data Structure Tests
 * - LRUCacheTest.java - Tests cache operations and eviction policies
 * - VectorClockTest.java - Tests distributed system clock operations
 * 
 * ### Design Pattern Tests
 * - SingletonPatternTest.java - Tests singleton instance management
 * - BuilderPatternTest.java - Tests builder pattern implementation
 * - FactoryPatternTest.java - Tests factory object creation
 * - ObserverPatternTest.java - Tests event notification system
 * - AdapterPatternTest.java - Tests legacy system adaptation
 * 
 * ### Security Pattern Tests
 * - JWTRefreshTokenRotationTest.java - Tests JWT token rotation and security
 * 
 * ### API Pattern Tests
 * - IdempotencyKeyMiddlewareTest.java - Tests idempotent request handling
 * 
 * ### Mapper Tests
 * - ProductMapperTest.java - Tests DTO mapping logic
 * 
 * ## Running Tests
 * 
 * ### Prerequisites
 * - JUnit 4.13.2
 * - Mockito 3.x or higher
 * - Java 8 or higher
 * 
 * ### Command Line
 * ```bash
 * # Compile all tests
 * javac -cp ".:lib/*:src/main/java/*" Unit_Tests/*.java
 * 
 * # Run specific test
 * java -cp ".:lib/*:src/main/java/*" org.junit.runner.JUnitCore ProductServiceTest
 * 
 * # Run all tests
 * java -cp ".:lib/*:src/main/java/*" org.junit.runner.JUnitCore Unit_Tests/*Test
 * ```
 * 
 * ### Using Maven
 * ```bash
 * mvn test
 * ```
 * 
 * ### Using Gradle
 * ```bash
 * gradle test
 * ```
 * 
 * ## Test Coverage
 * 
 * These tests provide coverage for:
 * - Business logic validation
 * - Edge case handling
 * - Exception scenarios
 * - Concurrent operations
 * - Data validation
 * - Security mechanisms
 * - Design pattern implementations
 * 
 * ## Best Practices Followed
 * 
 * 1. **AAA Pattern**: Arrange, Act, Assert structure
 * 2. **Independent Tests**: Each test can run independently
 * 3. **Clear Naming**: Test names describe what is being tested
 * 4. **Mocking**: External dependencies are mocked
 * 5. **Coverage**: Multiple scenarios per component
 * 6. **Assertions**: Meaningful assertion messages
 * 
 * ## Contributing
 * 
 * When adding new tests:
 * 1. Follow the existing naming convention: `<ClassName>Test.java`
 * 2. Use descriptive test method names: `test<Method>_<Scenario>_<ExpectedResult>`
 * 3. Include javadoc comments describing test purpose
 * 4. Ensure tests are independent and repeatable
 * 5. Mock external dependencies
 * 6. Test both success and failure scenarios
 * 
 * ## Test Categories
 * 
 * - **Unit Tests**: Test individual components in isolation
 * - **Integration Tests**: Test component interactions (future)
 * - **Performance Tests**: Test performance characteristics (future)
 * 
 * ## Known Issues
 * 
 * - Tests require proper classpath setup for compilation
 * - Some tests may require mock data initialization
 * - Ensure test database/environment is properly configured
 */
