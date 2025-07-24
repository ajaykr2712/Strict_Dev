"""
Database Connection Pool Simulator - System Design Concept Demonstration
Purpose: Simulate database connection pooling behavior and performance optimization
Author: System Design Daily
Date: 2025-07-24

This script demonstrates:
1. Connection pool management and lifecycle
2. Connection reuse vs. creation overhead
3. Pool sizing optimization
4. Connection timeout and retry mechanisms
5. Performance comparison with and without pooling
"""

import time
import threading
import random
import queue
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, field
from enum import Enum
import statistics
import contextlib


class ConnectionState(Enum):
    IDLE = "idle"
    ACTIVE = "active"
    CLOSED = "closed"
    ERROR = "error"


@dataclass
class DatabaseConnection:
    """Simulates a database connection"""
    id: str
    created_at: float = field(default_factory=time.time)
    last_used: float = field(default_factory=time.time)
    state: ConnectionState = ConnectionState.IDLE
    query_count: int = 0
    total_time: float = 0.0
    
    def connect(self) -> float:
        """Simulate connection establishment (expensive operation)"""
        connection_time = random.uniform(0.1, 0.3)  # 100-300ms
        time.sleep(connection_time)
        self.state = ConnectionState.ACTIVE
        return connection_time
    
    def execute_query(self, query: str) -> Dict[str, Any]:
        """Simulate query execution"""
        if self.state != ConnectionState.ACTIVE:
            raise Exception(f"Connection {self.id} is not active")
        
        # Simulate query execution time
        query_time = random.uniform(0.01, 0.05)  # 10-50ms
        time.sleep(query_time)
        
        self.query_count += 1
        self.total_time += query_time
        self.last_used = time.time()
        
        return {
            'result': f"Query executed: {query[:50]}...",
            'execution_time': query_time,
            'rows_affected': random.randint(0, 100)
        }
    
    def close(self):
        """Close the connection"""
        self.state = ConnectionState.CLOSED
    
    def is_expired(self, max_idle_time: float = 300) -> bool:
        """Check if connection has been idle too long"""
        return (time.time() - self.last_used) > max_idle_time
    
    def get_age(self) -> float:
        """Get connection age in seconds"""
        return time.time() - self.created_at


class ConnectionPool:
    """Database connection pool implementation"""
    
    def __init__(self, 
                 min_connections: int = 2,
                 max_connections: int = 10,
                 connection_timeout: float = 30.0,
                 max_idle_time: float = 300.0):
        self.min_connections = min_connections
        self.max_connections = max_connections
        self.connection_timeout = connection_timeout
        self.max_idle_time = max_idle_time
        
        # Pool state
        self.idle_connections: queue.Queue = queue.Queue()
        self.active_connections: Dict[str, DatabaseConnection] = {}
        self.total_created = 0
        self.total_closed = 0
        self.connection_requests = 0
        self.wait_times: List[float] = []
        
        # Thread safety
        self.lock = threading.RLock()
        
        # Initialize minimum connections
        self._initialize_pool()
    
    def _initialize_pool(self):
        """Create initial connections in the pool"""
        for i in range(self.min_connections):
            conn = self._create_connection()
            self.idle_connections.put(conn)
    
    def _create_connection(self) -> DatabaseConnection:
        """Create a new database connection"""
        conn_id = f"conn_{self.total_created + 1}"
        connection = DatabaseConnection(id=conn_id)
        
        # Simulate connection establishment
        connection.connect()
        
        with self.lock:
            self.total_created += 1
        
        return connection
    
    def get_connection(self) -> Optional[DatabaseConnection]:
        """Get a connection from the pool"""
        start_time = time.time()
        self.connection_requests += 1
        
        try:
            # Try to get an idle connection first
            try:
                connection = self.idle_connections.get(timeout=0.1)
                wait_time = time.time() - start_time
                self.wait_times.append(wait_time)
                
                with self.lock:
                    self.active_connections[connection.id] = connection
                
                return connection
            
            except queue.Empty:
                # No idle connections available
                with self.lock:
                    if len(self.active_connections) < self.max_connections:
                        # Create new connection
                        connection = self._create_connection()
                        self.active_connections[connection.id] = connection
                        wait_time = time.time() - start_time
                        self.wait_times.append(wait_time)
                        return connection
                    else:
                        # Pool is at maximum capacity, wait for a connection
                        print("⚠️  Pool at max capacity, waiting for connection...")
                        
                        # Wait with timeout
                        end_time = start_time + self.connection_timeout
                        while time.time() < end_time:
                            try:
                                connection = self.idle_connections.get(timeout=0.1)
                                wait_time = time.time() - start_time
                                self.wait_times.append(wait_time)
                                
                                with self.lock:
                                    self.active_connections[connection.id] = connection
                                
                                return connection
                            except queue.Empty:
                                continue
                        
                        # Timeout occurred
                        raise Exception("Connection timeout: No connections available")
        
        except Exception as e:
            print(f"❌ Failed to get connection: {e}")
            return None
    
    def return_connection(self, connection: DatabaseConnection):
        """Return a connection to the pool"""
        if not connection:
            return
        
        with self.lock:
            # Remove from active connections
            if connection.id in self.active_connections:
                del self.active_connections[connection.id]
            
            # Check if connection should be kept or closed
            if connection.is_expired(self.max_idle_time) or connection.state == ConnectionState.ERROR:
                connection.close()
                self.total_closed += 1
            else:
                # Return to idle pool
                connection.state = ConnectionState.IDLE
                self.idle_connections.put(connection)
    
    @contextlib.contextmanager
    def get_connection_context(self):
        """Context manager for automatic connection management"""
        connection = self.get_connection()
        try:
            yield connection
        finally:
            self.return_connection(connection)
    
    def cleanup_expired_connections(self):
        """Clean up expired idle connections"""
        expired_connections = []
        
        # Check idle connections
        temp_queue = queue.Queue()
        while not self.idle_connections.empty():
            try:
                conn = self.idle_connections.get_nowait()
                if conn.is_expired(self.max_idle_time):
                    expired_connections.append(conn)
                else:
                    temp_queue.put(conn)
            except queue.Empty:
                break
        
        # Put back non-expired connections
        while not temp_queue.empty():
            self.idle_connections.put(temp_queue.get())
        
        # Close expired connections
        for conn in expired_connections:
            conn.close()
            self.total_closed += 1
        
        return len(expired_connections)
    
    def get_statistics(self) -> Dict[str, Any]:
        """Get connection pool statistics"""
        with self.lock:
            idle_count = self.idle_connections.qsize()
            active_count = len(self.active_connections)
            
            return {
                'pool_config': {
                    'min_connections': self.min_connections,
                    'max_connections': self.max_connections,
                    'connection_timeout': self.connection_timeout,
                    'max_idle_time': self.max_idle_time
                },
                'current_state': {
                    'idle_connections': idle_count,
                    'active_connections': active_count,
                    'total_connections': idle_count + active_count
                },
                'lifetime_stats': {
                    'total_created': self.total_created,
                    'total_closed': self.total_closed,
                    'connection_requests': self.connection_requests,
                    'avg_wait_time': statistics.mean(self.wait_times) if self.wait_times else 0,
                    'max_wait_time': max(self.wait_times) if self.wait_times else 0
                }
            }
    
    def close_all(self):
        """Close all connections in the pool"""
        # Close idle connections
        while not self.idle_connections.empty():
            try:
                conn = self.idle_connections.get_nowait()
                conn.close()
                self.total_closed += 1
            except queue.Empty:
                break
        
        # Close active connections
        with self.lock:
            for conn in self.active_connections.values():
                conn.close()
                self.total_closed += 1
            self.active_connections.clear()


def simulate_without_pooling(num_requests: int = 50):
    """Simulate database operations without connection pooling"""
    print("🚫 Simulation WITHOUT Connection Pooling")
    print("-" * 40)
    
    start_time = time.time()
    total_connection_time = 0
    total_query_time = 0
    
    for i in range(num_requests):
        # Create new connection for each request (expensive!)
        conn = DatabaseConnection(id=f"direct_conn_{i}")
        connection_time = conn.connect()
        total_connection_time += connection_time
        
        # Execute query
        query = f"SELECT * FROM users WHERE id = {i}"
        result = conn.execute_query(query)
        total_query_time += result['execution_time']
        
        # Close connection
        conn.close()
    
    total_time = time.time() - start_time
    
    print("📊 Results:")
    print(f"  Total Requests: {num_requests}")
    print(f"  Total Time: {total_time:.3f} seconds")
    print(f"  Connection Time: {total_connection_time:.3f} seconds")
    print(f"  Query Time: {total_query_time:.3f} seconds")
    print(f"  Overhead: {((total_connection_time / total_time) * 100):.1f}%")
    print(f"  Requests/second: {num_requests / total_time:.2f}")
    
    return {
        'total_time': total_time,
        'connection_time': total_connection_time,
        'query_time': total_query_time,
        'requests_per_second': num_requests / total_time,
        'overhead_percentage': (total_connection_time / total_time) * 100
    }


def simulate_with_pooling(num_requests: int = 50, pool_size: int = 5):
    """Simulate database operations with connection pooling"""
    print(f"✅ Simulation WITH Connection Pooling (Pool Size: {pool_size})")
    print("-" * 40)
    
    # Create connection pool
    pool = ConnectionPool(min_connections=2, max_connections=pool_size)
    
    start_time = time.time()
    total_query_time = 0
    successful_requests = 0
    
    def worker_task(request_id: int):
        nonlocal total_query_time, successful_requests
        
        try:
            with pool.get_connection_context() as conn:
                if conn:
                    query = f"SELECT * FROM users WHERE id = {request_id}"
                    result = conn.execute_query(query)
                    total_query_time += result['execution_time']
                    successful_requests += 1
        except Exception as e:
            print(f"❌ Request {request_id} failed: {e}")
    
    # Simulate concurrent requests
    threads = []
    for i in range(num_requests):
        thread = threading.Thread(target=worker_task, args=(i,))
        threads.append(thread)
        thread.start()
    
    # Wait for all requests to complete
    for thread in threads:
        thread.join()
    
    total_time = time.time() - start_time
    stats = pool.get_statistics()
    
    print("📊 Results:")
    print(f"  Total Requests: {num_requests}")
    print(f"  Successful Requests: {successful_requests}")
    print(f"  Total Time: {total_time:.3f} seconds")
    print(f"  Query Time: {total_query_time:.3f} seconds")
    print(f"  Requests/second: {successful_requests / total_time:.2f}")
    print(f"  Connections Created: {stats['lifetime_stats']['total_created']}")
    print(f"  Avg Wait Time: {stats['lifetime_stats']['avg_wait_time']:.4f} seconds")
    
    # Cleanup
    pool.close_all()
    
    return {
        'total_time': total_time,
        'query_time': total_query_time,
        'requests_per_second': successful_requests / total_time,
        'connections_created': stats['lifetime_stats']['total_created'],
        'successful_requests': successful_requests
    }


def compare_pool_sizes():
    """Compare performance with different pool sizes"""
    print("\n🔍 Pool Size Optimization Analysis")
    print("=" * 50)
    
    num_requests = 30
    pool_sizes = [2, 5, 10, 15]
    results = []
    
    for pool_size in pool_sizes:
        print(f"\n🧪 Testing Pool Size: {pool_size}")
        result = simulate_with_pooling(num_requests, pool_size)
        result['pool_size'] = pool_size
        results.append(result)
        time.sleep(1)  # Brief pause between tests
    
    # Analyze results
    print("\n📈 Pool Size Comparison:")
    print("Pool Size | Req/Sec | Connections | Total Time")
    print("-" * 45)
    
    for result in results:
        print(f"{result['pool_size']:8d} | {result['requests_per_second']:7.2f} | "
              f"{result['connections_created']:11d} | {result['total_time']:9.3f}s")
    
    # Find optimal pool size
    best_result = max(results, key=lambda x: x['requests_per_second'])
    print(f"\n🏆 Optimal Pool Size: {best_result['pool_size']} "
          f"({best_result['requests_per_second']:.2f} req/sec)")


def main():
    """Main function to run connection pool demonstrations"""
    print("🏗️ Database Connection Pool System Design Demonstration")
    print("=" * 60)
    
    num_requests = 25
    
    try:
        # Test without pooling
        no_pool_result = simulate_without_pooling(num_requests)
        
        print("\n" + "=" * 60)
        
        # Test with pooling
        pool_result = simulate_with_pooling(num_requests, pool_size=5)
        
        # Performance comparison
        print("\n🚀 Performance Improvement:")
        improvement = ((pool_result['requests_per_second'] - no_pool_result['requests_per_second']) 
                      / no_pool_result['requests_per_second'] * 100)
        print(f"  Throughput Improvement: {improvement:.1f}%")
        
        overhead_reduction = no_pool_result['overhead_percentage']
        print(f"  Connection Overhead Eliminated: {overhead_reduction:.1f}%")
        
        # Pool size optimization
        compare_pool_sizes()
        
        print("\n✅ Connection Pool Simulation Completed!")
        print("\n💡 Key System Design Insights:")
        print("   - Connection pooling dramatically improves performance")
        print("   - Pool size should be tuned based on workload patterns")
        print("   - Connection reuse eliminates expensive setup overhead")
        print("   - Proper timeout and cleanup prevent resource leaks")
        print("   - Monitor pool metrics for optimal configuration")
        
    except Exception as e:
        print(f"❌ Error during simulation: {e}")


if __name__ == "__main__":
    main()
