#!/usr/bin/env python3
"""
Comprehensive Testing Framework for Distributed Systems
Includes unit tests, integration tests, load tests, and chaos engineering.
"""

import time
import random
import threading
import requests
from typing import Dict, List, Any, Callable, Optional
from dataclasses import dataclass
from concurrent.futures import ThreadPoolExecutor, as_completed
import statistics


@dataclass
class TestResult:
    test_name: str
    success: bool
    duration: float
    error_message: Optional[str] = None
    metrics: Dict[str, Any] = None


@dataclass
class LoadTestResult:
    total_requests: int
    successful_requests: int
    failed_requests: int
    average_response_time: float
    p95_response_time: float
    p99_response_time: float
    requests_per_second: float
    error_rate: float


class BaseTest:
    """Base class for all tests"""
    
    def __init__(self, name: str):
        self.name = name
        self.setup_complete = False
        self.teardown_complete = False
    
    def setup(self):
        """Setup before test execution"""
        self.setup_complete = True
    
    def teardown(self):
        """Cleanup after test execution"""
        self.teardown_complete = True
    
    def run(self) -> TestResult:
        """Run the test and return results"""
        start_time = time.time()
        
        try:
            self.setup()
            result = self.execute()
            duration = time.time() - start_time
            
            return TestResult(
                test_name=self.name,
                success=result,
                duration=duration
            )
        
        except Exception as e:
            duration = time.time() - start_time
            return TestResult(
                test_name=self.name,
                success=False,
                duration=duration,
                error_message=str(e)
            )
        
        finally:
            self.teardown()
    
    def execute(self) -> bool:
        """Override this method with test logic"""
        raise NotImplementedError


class UnitTest(BaseTest):
    """Unit test for individual components"""
    
    def __init__(self, name: str, test_func: Callable[[], bool]):
        super().__init__(name)
        self.test_func = test_func
    
    def execute(self) -> bool:
        return self.test_func()


class IntegrationTest(BaseTest):
    """Integration test for component interactions"""
    
    def __init__(self, name: str, components: List[str], test_func: Callable[[], bool]):
        super().__init__(name)
        self.components = components
        self.test_func = test_func
    
    def execute(self) -> bool:
        # Verify all components are available
        for component in self.components:
            if not self.check_component_health(component):
                raise Exception(f"Component {component} is not healthy")
        
        return self.test_func()
    
    def check_component_health(self, component: str) -> bool:
        """Override to implement component health checks"""
        return True


class LoadTest(BaseTest):
    """Load test for performance testing"""
    
    def __init__(self, name: str, target_url: str, concurrent_users: int, 
                 duration_seconds: int, request_func: Callable = None):
        super().__init__(name)
        self.target_url = target_url
        self.concurrent_users = concurrent_users
        self.duration_seconds = duration_seconds
        self.request_func = request_func or self.default_request
        self.results: List[float] = []
        self.errors: List[str] = []
    
    def default_request(self) -> tuple:
        """Default HTTP GET request"""
        try:
            start_time = time.time()
            response = requests.get(self.target_url, timeout=30)
            duration = time.time() - start_time
            return duration, response.status_code == 200, None
        except Exception as e:
            duration = time.time() - start_time
            return duration, False, str(e)
    
    def execute(self) -> bool:
        """Execute load test"""
        print(f"Starting load test: {self.concurrent_users} users for {self.duration_seconds}s")
        
        start_time = time.time()
        end_time = start_time + self.duration_seconds
        
        with ThreadPoolExecutor(max_workers=self.concurrent_users) as executor:
            futures = []
            
            # Submit initial batch of requests
            for _ in range(self.concurrent_users):
                future = executor.submit(self.worker_thread, end_time)
                futures.append(future)
            
            # Collect results
            for future in as_completed(futures):
                try:
                    future.result()
                except Exception as e:
                    self.errors.append(str(e))
        
        # Generate load test report
        self.generate_report()
        return len(self.errors) == 0
    
    def worker_thread(self, end_time: float):
        """Worker thread that continuously sends requests"""
        while time.time() < end_time:
            duration, success, error = self.request_func()
            
            self.results.append(duration)
            if not success:
                self.errors.append(error or "Request failed")
            
            # Small delay to prevent overwhelming
            time.sleep(0.01)
    
    def generate_report(self) -> LoadTestResult:
        """Generate load test report"""
        if not self.results:
            return LoadTestResult(0, 0, len(self.errors), 0, 0, 0, 0, 100.0)
        
        total_requests = len(self.results)
        failed_requests = len(self.errors)
        successful_requests = total_requests - failed_requests
        
        avg_response_time = statistics.mean(self.results)
        sorted_results = sorted(self.results)
        p95_response_time = sorted_results[int(0.95 * len(sorted_results))]
        p99_response_time = sorted_results[int(0.99 * len(sorted_results))]
        
        requests_per_second = total_requests / self.duration_seconds
        error_rate = (failed_requests / total_requests) * 100 if total_requests > 0 else 0
        
        report = LoadTestResult(
            total_requests=total_requests,
            successful_requests=successful_requests,
            failed_requests=failed_requests,
            average_response_time=avg_response_time,
            p95_response_time=p95_response_time,
            p99_response_time=p99_response_time,
            requests_per_second=requests_per_second,
            error_rate=error_rate
        )
        
        print(f"\nLoad Test Report for {self.name}:")
        print(f"Total Requests: {report.total_requests}")
        print(f"Successful: {report.successful_requests}")
        print(f"Failed: {report.failed_requests}")
        print(f"Average Response Time: {report.average_response_time:.3f}s")
        print(f"P95 Response Time: {report.p95_response_time:.3f}s")
        print(f"P99 Response Time: {report.p99_response_time:.3f}s")
        print(f"Requests/Second: {report.requests_per_second:.2f}")
        print(f"Error Rate: {report.error_rate:.2f}%")
        
        return report


class ChaosTest(BaseTest):
    """Chaos engineering test to validate system resilience"""
    
    def __init__(self, name: str, chaos_actions: List[Callable], 
                 validation_func: Callable[[], bool]):
        super().__init__(name)
        self.chaos_actions = chaos_actions
        self.validation_func = validation_func
        self.chaos_threads: List[threading.Thread] = []
    
    def execute(self) -> bool:
        """Execute chaos test"""
        print(f"Starting chaos test: {self.name}")
        
        try:
            # Start chaos actions
            for action in self.chaos_actions:
                thread = threading.Thread(target=action, daemon=True)
                thread.start()
                self.chaos_threads.append(thread)
            
            # Allow chaos to run for a while
            time.sleep(5)
            
            # Validate system still functions
            return self.validation_func()
        
        finally:
            # Stop chaos actions
            self.stop_chaos()
    
    def stop_chaos(self):
        """Stop all chaos actions"""
        for thread in self.chaos_threads:
            if thread.is_alive():
                # In a real implementation, you'd have proper cleanup
                pass


class DatabaseTest(BaseTest):
    """Database-specific testing"""
    
    def __init__(self, name: str, database_config: Dict[str, Any]):
        super().__init__(name)
        self.database_config = database_config
        self.connection = None
    
    def setup(self):
        """Setup database connection"""
        # In a real implementation, create database connection
        super().setup()
        print(f"Connected to database: {self.database_config.get('host', 'localhost')}")
    
    def teardown(self):
        """Close database connection"""
        if self.connection:
            # Close connection
            pass
        super().teardown()
    
    def execute(self) -> bool:
        """Execute database tests"""
        # Test connection
        if not self.test_connection():
            return False
        
        # Test CRUD operations
        if not self.test_crud_operations():
            return False
        
        # Test performance
        if not self.test_query_performance():
            return False
        
        return True
    
    def test_connection(self) -> bool:
        """Test database connectivity"""
        try:
            # Simulate connection test
            time.sleep(0.1)
            return True
        except Exception as e:
            print(f"Connection test failed: {e}")
            return False
    
    def test_crud_operations(self) -> bool:
        """Test basic CRUD operations"""
        try:
            # Simulate CRUD tests
            time.sleep(0.2)
            return True
        except Exception as e:
            print(f"CRUD test failed: {e}")
            return False
    
    def test_query_performance(self) -> bool:
        """Test query performance"""
        try:
            start_time = time.time()
            # Simulate complex query
            time.sleep(0.5)
            duration = time.time() - start_time
            
            if duration > 1.0:  # Threshold
                print(f"Query performance issue: {duration:.3f}s")
                return False
            
            return True
        except Exception as e:
            print(f"Performance test failed: {e}")
            return False


class APITest(BaseTest):
    """REST API testing"""
    
    def __init__(self, name: str, base_url: str, endpoints: List[Dict]):
        super().__init__(name)
        self.base_url = base_url
        self.endpoints = endpoints
        self.session = requests.Session()
    
    def execute(self) -> bool:
        """Execute API tests"""
        for endpoint in self.endpoints:
            if not self.test_endpoint(endpoint):
                return False
        return True
    
    def test_endpoint(self, endpoint: Dict) -> bool:
        """Test a specific API endpoint"""
        method = endpoint.get('method', 'GET')
        path = endpoint.get('path', '/')
        expected_status = endpoint.get('expected_status', 200)
        headers = endpoint.get('headers', {})
        data = endpoint.get('data')
        
        url = f"{self.base_url}{path}"
        
        try:
            if method == 'GET':
                response = self.session.get(url, headers=headers, timeout=10)
            elif method == 'POST':
                response = self.session.post(url, headers=headers, json=data, timeout=10)
            elif method == 'PUT':
                response = self.session.put(url, headers=headers, json=data, timeout=10)
            elif method == 'DELETE':
                response = self.session.delete(url, headers=headers, timeout=10)
            else:
                print(f"Unsupported method: {method}")
                return False
            
            if response.status_code != expected_status:
                print(f"API test failed: {method} {path} returned {response.status_code}, expected {expected_status}")
                return False
            
            # Validate response time
            if response.elapsed.total_seconds() > 2.0:
                print(f"API response too slow: {response.elapsed.total_seconds():.3f}s")
                return False
            
            return True
        
        except Exception as e:
            print(f"API test error: {e}")
            return False


class TestSuite:
    """Test suite manager"""
    
    def __init__(self, name: str):
        self.name = name
        self.tests: List[BaseTest] = []
        self.results: List[TestResult] = []
    
    def add_test(self, test: BaseTest):
        """Add a test to the suite"""
        self.tests.append(test)
    
    def run_all(self) -> Dict[str, Any]:
        """Run all tests in the suite"""
        print(f"Running test suite: {self.name}")
        print("=" * 50)
        
        start_time = time.time()
        
        for test in self.tests:
            print(f"Running {test.name}...")
            result = test.run()
            self.results.append(result)
            
            status = "✓ PASS" if result.success else "✗ FAIL"
            print(f"  {status} ({result.duration:.3f}s)")
            
            if not result.success and result.error_message:
                print(f"  Error: {result.error_message}")
        
        total_duration = time.time() - start_time
        
        # Generate summary
        summary = self.generate_summary(total_duration)
        self.print_summary(summary)
        
        return summary
    
    def generate_summary(self, total_duration: float) -> Dict[str, Any]:
        """Generate test suite summary"""
        total_tests = len(self.results)
        passed_tests = sum(1 for r in self.results if r.success)
        failed_tests = total_tests - passed_tests
        
        return {
            "suite_name": self.name,
            "total_tests": total_tests,
            "passed": passed_tests,
            "failed": failed_tests,
            "success_rate": (passed_tests / total_tests) * 100 if total_tests > 0 else 0,
            "total_duration": total_duration,
            "average_test_duration": total_duration / total_tests if total_tests > 0 else 0
        }
    
    def print_summary(self, summary: Dict[str, Any]):
        """Print test suite summary"""
        print("\n" + "=" * 50)
        print(f"Test Suite Summary: {summary['suite_name']}")
        print("=" * 50)
        print(f"Total Tests: {summary['total_tests']}")
        print(f"Passed: {summary['passed']}")
        print(f"Failed: {summary['failed']}")
        print(f"Success Rate: {summary['success_rate']:.1f}%")
        print(f"Total Duration: {summary['total_duration']:.3f}s")
        print(f"Average Test Duration: {summary['average_test_duration']:.3f}s")


def create_sample_tests() -> TestSuite:
    """Create a sample test suite"""
    suite = TestSuite("System Integration Test Suite")
    
    # Unit tests
    def test_math_operations():
        return 2 + 2 == 4 and 3 * 3 == 9
    
    def test_string_operations():
        return "hello".upper() == "HELLO" and "WORLD".lower() == "world"
    
    suite.add_test(UnitTest("Math Operations", test_math_operations))
    suite.add_test(UnitTest("String Operations", test_string_operations))
    
    # Integration test
    def test_service_integration():
        # Simulate service interaction
        time.sleep(0.1)
        return True
    
    suite.add_test(IntegrationTest(
        "Service Integration",
        ["user_service", "order_service"],
        test_service_integration
    ))
    
    # Database test
    suite.add_test(DatabaseTest(
        "Database Connectivity",
        {"host": "localhost", "port": 5432, "database": "testdb"}
    ))
    
    # API test
    api_endpoints = [
        {"method": "GET", "path": "/health", "expected_status": 200},
        {"method": "GET", "path": "/api/users", "expected_status": 200},
        {"method": "POST", "path": "/api/users", "expected_status": 201, 
         "data": {"name": "Test User", "email": "test@example.com"}}
    ]
    
    suite.add_test(APITest(
        "API Endpoints",
        "http://localhost:8080",
        api_endpoints
    ))
    
    return suite


def create_load_test() -> LoadTest:
    """Create a sample load test"""
    def custom_request():
        """Custom request function for load testing"""
        try:
            start_time = time.time()
            # Simulate API call
            time.sleep(random.uniform(0.05, 0.2))
            duration = time.time() - start_time
            
            # Simulate 95% success rate
            success = random.random() < 0.95
            error = None if success else "Simulated failure"
            
            return duration, success, error
        
        except Exception as e:
            duration = time.time() - start_time
            return duration, False, str(e)
    
    return LoadTest(
        "API Load Test",
        "http://localhost:8080/api/health",
        concurrent_users=10,
        duration_seconds=30,
        request_func=custom_request
    )


def create_chaos_test() -> ChaosTest:
    """Create a sample chaos test"""
    def simulate_network_partition():
        """Simulate network partition chaos"""
        print("Simulating network partition...")
        time.sleep(10)
    
    def simulate_high_cpu():
        """Simulate high CPU load chaos"""
        print("Simulating high CPU load...")
        # Simulate CPU load
        end_time = time.time() + 10
        while time.time() < end_time:
            sum(i * i for i in range(1000))
    
    def validate_system_health():
        """Validate that system still functions under chaos"""
        try:
            # Simulate health check
            time.sleep(0.5)
            return True  # Assume system is resilient
        except Exception:
            return False
    
    return ChaosTest(
        "System Resilience Test",
        [simulate_network_partition, simulate_high_cpu],
        validate_system_health
    )


def demo_testing_framework():
    """Demonstrate the testing framework"""
    print("Distributed Systems Testing Framework Demo")
    print("=" * 60)
    
    # Run unit and integration tests
    test_suite = create_sample_tests()
    suite_results = test_suite.run_all()
    
    print("\n" + "=" * 60)
    
    # Run load test
    load_test = create_load_test()
    print(f"Running load test: {load_test.name}")
    load_result = load_test.run()
    
    print("\n" + "=" * 60)
    
    # Run chaos test
    chaos_test = create_chaos_test()
    print(f"Running chaos test: {chaos_test.name}")
    chaos_result = chaos_test.run()
    
    print("\n" + "=" * 60)
    print("Testing Framework Demo Complete!")
    
    # Overall summary
    print("\nOverall Results:")
    print(f"Test Suite Success Rate: {suite_results['success_rate']:.1f}%")
    print(f"Load Test: {'PASS' if load_result.success else 'FAIL'}")
    print(f"Chaos Test: {'PASS' if chaos_result.success else 'FAIL'}")


if __name__ == "__main__":
    demo_testing_framework()
