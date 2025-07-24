"""
Load Balancer Simulation - System Design Concept Demonstration
Purpose: Simulate different load balancing algorithms and their behavior
Author: System Design Daily
Date: 2025-07-24

This script demonstrates:
1. Different load balancing algorithms (Round Robin, Least Connections, Weighted)
2. Server health monitoring and failover
3. Performance metrics collection
4. Load distribution patterns
"""

import random
import time
import threading
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, field
from enum import Enum
import statistics


class ServerStatus(Enum):
    HEALTHY = "healthy"
    UNHEALTHY = "unhealthy"
    MAINTENANCE = "maintenance"


@dataclass
class Server:
    """Represents a backend server"""
    id: str
    weight: int = 1
    status: ServerStatus = ServerStatus.HEALTHY
    active_connections: int = 0
    total_requests: int = 0
    response_times: List[float] = field(default_factory=list)
    
    def process_request(self) -> float:
        """Simulate request processing and return response time"""
        if self.status != ServerStatus.HEALTHY:
            raise Exception(f"Server {self.id} is {self.status.value}")
        
        # Simulate processing time (varies by server load)
        base_time = random.uniform(0.1, 0.3)
        load_factor = 1 + (self.active_connections * 0.1)
        processing_time = base_time * load_factor
        
        self.active_connections += 1
        time.sleep(processing_time)
        self.active_connections -= 1
        self.total_requests += 1
        self.response_times.append(processing_time)
        
        return processing_time
    
    def get_average_response_time(self) -> float:
        """Get average response time for this server"""
        if not self.response_times:
            return 0.0
        return statistics.mean(self.response_times[-100:])  # Last 100 requests
    
    def get_health_score(self) -> float:
        """Calculate server health score based on performance"""
        if self.status != ServerStatus.HEALTHY:
            return 0.0
        
        # Score based on response time and connection count
        avg_response = self.get_average_response_time()
        connection_score = max(0, 1 - (self.active_connections / 10))
        response_score = max(0, 1 - (avg_response / 1.0))
        
        return (connection_score + response_score) / 2


class LoadBalancerAlgorithm(Enum):
    ROUND_ROBIN = "round_robin"
    LEAST_CONNECTIONS = "least_connections"
    WEIGHTED_ROUND_ROBIN = "weighted_round_robin"
    LEAST_RESPONSE_TIME = "least_response_time"
    RANDOM = "random"


class LoadBalancer:
    """Load balancer with multiple algorithms"""
    
    def __init__(self, servers: List[Server], algorithm: LoadBalancerAlgorithm = LoadBalancerAlgorithm.ROUND_ROBIN):
        self.servers = servers
        self.algorithm = algorithm
        self.current_index = 0
        self.weighted_counters = {server.id: 0 for server in servers}
        self.total_requests = 0
        self.failed_requests = 0
        self.lock = threading.Lock()
    
    def get_healthy_servers(self) -> List[Server]:
        """Get list of healthy servers"""
        return [server for server in self.servers if server.status == ServerStatus.HEALTHY]
    
    def select_server(self) -> Optional[Server]:
        """Select a server based on the configured algorithm"""
        healthy_servers = self.get_healthy_servers()
        
        if not healthy_servers:
            return None
        
        with self.lock:
            if self.algorithm == LoadBalancerAlgorithm.ROUND_ROBIN:
                server = healthy_servers[self.current_index % len(healthy_servers)]
                self.current_index += 1
                return server
            
            elif self.algorithm == LoadBalancerAlgorithm.LEAST_CONNECTIONS:
                return min(healthy_servers, key=lambda s: s.active_connections)
            
            elif self.algorithm == LoadBalancerAlgorithm.WEIGHTED_ROUND_ROBIN:
                # Select server based on weights
                for server in healthy_servers:
                    if self.weighted_counters[server.id] < server.weight:
                        self.weighted_counters[server.id] += 1
                        return server
                
                # Reset counters and select first server
                self.weighted_counters = {server.id: 0 for server in self.servers}
                if healthy_servers:
                    self.weighted_counters[healthy_servers[0].id] = 1
                    return healthy_servers[0]
            
            elif self.algorithm == LoadBalancerAlgorithm.LEAST_RESPONSE_TIME:
                return min(healthy_servers, key=lambda s: s.get_average_response_time())
            
            elif self.algorithm == LoadBalancerAlgorithm.RANDOM:
                return random.choice(healthy_servers)
        
        return None
    
    def handle_request(self, request_id: int) -> Dict[str, Any]:
        """Handle incoming request"""
        start_time = time.time()
        self.total_requests += 1
        
        server = self.select_server()
        if not server:
            self.failed_requests += 1
            return {
                'request_id': request_id,
                'status': 'failed',
                'error': 'No healthy servers available',
                'response_time': 0,
                'server_id': None
            }
        
        try:
            response_time = server.process_request()
            total_time = time.time() - start_time
            
            return {
                'request_id': request_id,
                'status': 'success',
                'server_id': server.id,
                'response_time': response_time,
                'total_time': total_time,
                'server_connections': server.active_connections
            }
        
        except Exception as e:
            self.failed_requests += 1
            return {
                'request_id': request_id,
                'status': 'failed',
                'error': str(e),
                'response_time': 0,
                'server_id': server.id if server else None
            }
    
    def get_statistics(self) -> Dict[str, Any]:
        """Get load balancer statistics"""
        healthy_servers = self.get_healthy_servers()
        
        return {
            'algorithm': self.algorithm.value,
            'total_requests': self.total_requests,
            'failed_requests': self.failed_requests,
            'success_rate': (self.total_requests - self.failed_requests) / max(1, self.total_requests),
            'healthy_servers': len(healthy_servers),
            'total_servers': len(self.servers),
            'server_stats': [
                {
                    'id': server.id,
                    'status': server.status.value,
                    'requests': server.total_requests,
                    'active_connections': server.active_connections,
                    'avg_response_time': server.get_average_response_time(),
                    'health_score': server.get_health_score(),
                    'weight': server.weight
                }
                for server in self.servers
            ]
        }


def simulate_load_balancing():
    """Simulate load balancing with different algorithms"""
    print("🔄 Load Balancer Simulation")
    print("=" * 50)
    
    # Create servers with different configurations
    servers = [
        Server(id="server-1", weight=3),  # High-capacity server
        Server(id="server-2", weight=2),  # Medium-capacity server
        Server(id="server-3", weight=1),  # Low-capacity server
        Server(id="server-4", weight=2),  # Medium-capacity server
    ]
    
    algorithms = [
        LoadBalancerAlgorithm.ROUND_ROBIN,
        LoadBalancerAlgorithm.LEAST_CONNECTIONS,
        LoadBalancerAlgorithm.WEIGHTED_ROUND_ROBIN,
        LoadBalancerAlgorithm.LEAST_RESPONSE_TIME
    ]
    
    for algorithm in algorithms:
        print(f"\n📊 Testing {algorithm.value.replace('_', ' ').title()} Algorithm")
        print("-" * 40)
        
        # Reset server states
        for server in servers:
            server.active_connections = 0
            server.total_requests = 0
            server.response_times = []
            server.status = ServerStatus.HEALTHY
        
        # Simulate one server going down during test
        if algorithm == LoadBalancerAlgorithm.LEAST_CONNECTIONS:
            servers[2].status = ServerStatus.UNHEALTHY
            print("⚠️  Server-3 marked as unhealthy for this test")
        
        load_balancer = LoadBalancer(servers, algorithm)
        
        # Simulate concurrent requests
        def worker(request_count: int):
            results = []
            for i in range(request_count):
                result = load_balancer.handle_request(i)
                results.append(result)
                time.sleep(random.uniform(0.01, 0.05))  # Simulate request intervals
            return results
        
        # Run simulation with multiple threads
        threads = []
        num_threads = 5
        requests_per_thread = 20
        
        start_time = time.time()
        
        for t in range(num_threads):
            thread = threading.Thread(target=worker, args=(requests_per_thread,))
            threads.append(thread)
            thread.start()
        
        for thread in threads:
            thread.join()
        
        total_time = time.time() - start_time
        
        # Print statistics
        stats = load_balancer.get_statistics()
        print(f"Total Requests: {stats['total_requests']}")
        print(f"Success Rate: {stats['success_rate']:.2%}")
        print(f"Total Time: {total_time:.2f} seconds")
        print(f"Requests/second: {stats['total_requests'] / total_time:.2f}")
        
        print("\nServer Distribution:")
        for server_stat in stats['server_stats']:
            status_emoji = "✅" if server_stat['status'] == "healthy" else "❌"
            print(f"  {status_emoji} {server_stat['id']}: "
                  f"{server_stat['requests']} requests "
                  f"(Weight: {server_stat['weight']}, "
                  f"Avg Response: {server_stat['avg_response_time']:.3f}s)")


def demonstrate_health_checks():
    """Demonstrate health check and failover scenarios"""
    print("\n🏥 Health Check and Failover Demonstration")
    print("=" * 50)
    
    servers = [
        Server(id="primary", weight=2),
        Server(id="secondary", weight=1),
        Server(id="backup", weight=1)
    ]
    
    load_balancer = LoadBalancer(servers, LoadBalancerAlgorithm.WEIGHTED_ROUND_ROBIN)
    
    print("Initial state: All servers healthy")
    
    # Send some requests
    for i in range(10):
        result = load_balancer.handle_request(i)
        print(f"Request {i}: Server {result.get('server_id', 'None')} "
              f"({result['status']})")
        time.sleep(0.1)
    
    # Simulate primary server failure
    print("\n⚠️  Primary server failure detected!")
    servers[0].status = ServerStatus.UNHEALTHY
    
    # Continue sending requests
    for i in range(10, 15):
        result = load_balancer.handle_request(i)
        print(f"Request {i}: Server {result.get('server_id', 'None')} "
              f"({result['status']})")
        time.sleep(0.1)
    
    # Simulate recovery
    print("\n✅ Primary server recovered!")
    servers[0].status = ServerStatus.HEALTHY
    
    # Final requests
    for i in range(15, 20):
        result = load_balancer.handle_request(i)
        print(f"Request {i}: Server {result.get('server_id', 'None')} "
              f"({result['status']})")
        time.sleep(0.1)
    
    # Print final statistics
    stats = load_balancer.get_statistics()
    print("\nFinal Statistics:")
    print(f"Total Requests: {stats['total_requests']}")
    print(f"Failed Requests: {stats['failed_requests']}")
    print(f"Success Rate: {stats['success_rate']:.2%}")


def main():
    """Main function to run load balancer demonstrations"""
    print("🏗️ Load Balancer System Design Demonstration")
    print("=" * 60)
    
    try:
        # Run algorithm comparison
        simulate_load_balancing()
        
        # Demonstrate health checks
        demonstrate_health_checks()
        
        print("\n✅ Load Balancer Simulation Completed!")
        print("\n💡 Key System Design Insights:")
        print("   - Different algorithms suit different scenarios")
        print("   - Health checks enable automatic failover")
        print("   - Weighted distribution optimizes resource utilization")
        print("   - Monitoring is crucial for load balancer performance")
        print("   - Multiple algorithms can be combined for hybrid approaches")
        
    except Exception as e:
        print(f"❌ Error during simulation: {e}")


if __name__ == "__main__":
    main()
