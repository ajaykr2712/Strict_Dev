"""
System Design Concept Demonstration: Exponential Growth and Performance Testing
Purpose: Demonstrate computational complexity and performance implications in system design
Author: System Design Daily
Date: 2025-07-24

This script demonstrates:
1. Exponential computational growth
2. Performance measurement techniques
3. Resource utilization monitoring
4. System design implications of algorithmic complexity
"""

import time
import psutil
import threading
from typing import List, Dict, Any
import statistics


class PerformanceMonitor:
    """Monitor system performance during operations"""
    
    def __init__(self):
        self.metrics: List[Dict[str, Any]] = []
        self.monitoring = False
        
    def start_monitoring(self, interval: float = 0.1):
        """Start monitoring system metrics"""
        self.monitoring = True
        self.monitor_thread = threading.Thread(target=self._monitor_loop, args=(interval,))
        self.monitor_thread.daemon = True
        self.monitor_thread.start()
        
    def stop_monitoring(self):
        """Stop monitoring and return collected metrics"""
        self.monitoring = False
        if hasattr(self, 'monitor_thread'):
            self.monitor_thread.join(timeout=1)
        return self.metrics
        
    def _monitor_loop(self, interval: float):
        """Internal monitoring loop"""
        while self.monitoring:
            try:
                cpu_percent = psutil.cpu_percent(interval=None)
                memory = psutil.virtual_memory()
                
                self.metrics.append({
                    'timestamp': time.time(),
                    'cpu_percent': cpu_percent,
                    'memory_percent': memory.percent,
                    'memory_used_mb': memory.used / (1024 * 1024)
                })
            except Exception as e:
                print(f"Monitoring error: {e}")
            
            time.sleep(interval)


def exponential_computation_demo(base: int = 45, exponent: int = 66, iterations: int = 10):
    """
    Demonstrate exponential computational complexity
    
    In system design, this represents:
    - Algorithmic complexity impact on performance
    - Resource consumption patterns
    - Scaling challenges with exponential operations
    """
    print("🔢 Exponential Computation Demo")
    print(f"Computing {base}^{exponent} for {iterations} iterations\n")
    
    monitor = PerformanceMonitor()
    monitor.start_monitoring()
    
    execution_times = []
    
    for i in range(1, iterations + 1):
        start_time = time.perf_counter()
        
        # Perform the exponential calculation
        result = base ** exponent
        
        end_time = time.perf_counter()
        execution_time = end_time - start_time
        execution_times.append(execution_time)
        
        print(f"Iteration {i:2d}: {execution_time:.6f} seconds (Result: {len(str(result))} digits)")
        
        # Small delay to see monitoring effects
        time.sleep(0.01)
    
    metrics = monitor.stop_monitoring()
    
    # Performance Analysis
    print("\n📊 Performance Analysis:")
    print(f"Average execution time: {statistics.mean(execution_times):.6f} seconds")
    print(f"Min execution time: {min(execution_times):.6f} seconds")
    print(f"Max execution time: {max(execution_times):.6f} seconds")
    print(f"Standard deviation: {statistics.stdev(execution_times):.6f} seconds")
    
    if metrics:
        cpu_usage = [m['cpu_percent'] for m in metrics if m['cpu_percent'] > 0]
        memory_usage = [m['memory_percent'] for m in metrics]
        
        if cpu_usage:
            print(f"Average CPU usage: {statistics.mean(cpu_usage):.2f}%")
            print(f"Peak CPU usage: {max(cpu_usage):.2f}%")
        
        print(f"Average memory usage: {statistics.mean(memory_usage):.2f}%")
        print(f"Peak memory usage: {max(memory_usage):.2f}%")


def system_design_lessons():
    """
    Explain system design lessons from this demonstration
    """
    print("\n🏗️ System Design Lessons:")
    print("1. **Algorithmic Complexity**: O(log n) operations scale better than O(n^2) or O(2^n)")
    print("2. **Resource Monitoring**: Essential for understanding system behavior under load")
    print("3. **Performance Baselines**: Establish metrics for comparison and optimization")
    print("4. **Scalability Planning**: Understand how operations scale with input size")
    print("5. **Resource Constraints**: CPU and memory usage impact system capacity")
    print("6. **Optimization Opportunities**: Profile before optimizing bottlenecks")


def load_testing_simulation(concurrent_operations: int = 5):
    """
    Simulate concurrent load testing scenario
    
    Demonstrates:
    - Concurrent processing patterns
    - Resource contention
    - Thread safety considerations
    """
    print("\n🔄 Concurrent Load Testing Simulation")
    print(f"Running {concurrent_operations} concurrent exponential operations\n")
    
    monitor = PerformanceMonitor()
    monitor.start_monitoring()
    
    def worker_task(worker_id: int, base: int, exponent: int):
        """Worker task for concurrent execution"""
        start_time = time.perf_counter()
        result = base ** (exponent // 2)  # Reduced exponent for demo
        end_time = time.perf_counter()
        
        print(f"Worker {worker_id}: Completed in {end_time - start_time:.4f} seconds")
        return result
    
    # Start concurrent workers
    threads = []
    start_time = time.perf_counter()
    
    for i in range(concurrent_operations):
        thread = threading.Thread(target=worker_task, args=(i + 1, 45, 30))
        threads.append(thread)
        thread.start()
    
    # Wait for all workers to complete
    for thread in threads:
        thread.join()
    
    total_time = time.perf_counter() - start_time
    metrics = monitor.stop_monitoring()
    
    print("\n📈 Concurrent Execution Results:")
    print(f"Total execution time: {total_time:.4f} seconds")
    print(f"Operations per second: {concurrent_operations / total_time:.2f}")
    
    if metrics:
        cpu_usage = [m['cpu_percent'] for m in metrics if m['cpu_percent'] > 0]
        if cpu_usage:
            print(f"Peak CPU usage during concurrency: {max(cpu_usage):.2f}%")


def main():
    """
    Main function demonstrating system design concepts through practical examples
    """
    print("=" * 70)
    print("🏗️  SYSTEM DESIGN CONCEPT DEMONSTRATION")
    print("=" * 70)
    
    try:
        # Single-threaded performance demo
        exponential_computation_demo(base=45, exponent=66, iterations=5)
        
        # System design lessons
        system_design_lessons()
        
        # Concurrent load testing
        load_testing_simulation(concurrent_operations=3)
        
        print("\n✅ Demo completed successfully!")
        print("\n💡 Key Takeaways:")
        print("   - Monitor performance to understand system behavior")
        print("   - Consider algorithmic complexity in system design")
        print("   - Test concurrent scenarios to identify bottlenecks")
        print("   - Use profiling data to make informed optimization decisions")
        
    except Exception as e:
        print(f"❌ Error during demonstration: {e}")
    
    print("=" * 70)


if __name__ == "__main__":
    main()
        
