# Service Discovery

## Overview
Service Discovery is a mechanism that allows services in a distributed system to find and communicate with each other without hard-coding network locations. It automatically maintains a registry of available services and their endpoints, enabling dynamic service-to-service communication in microservices architectures.

## Key Concepts
- **Service Registry**: Central repository of available services and their locations
- **Health Checks**: Continuous monitoring of service availability
- **Load Balancing**: Distribution of requests across service instances
- **Automatic Registration**: Services register themselves upon startup

## Advanced Topics

### 1. Service Discovery Patterns

#### Client-Side Discovery
```
[Client] → [Service Registry] → Get Service List
[Client] → [Load Balancer Logic] → [Service Instance]
```
- Client queries registry directly
- Client implements load balancing
- Examples: Netflix Eureka, Apache Zookeeper

#### Server-Side Discovery
```
[Client] → [Load Balancer] → [Service Registry] → [Service Instance]
```
- Load balancer handles discovery
- Client unaware of service locations
- Examples: AWS ELB, Kubernetes Service

### 2. Service Registry Implementations

#### Netflix Eureka
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka-server:8761/eureka/
  instance:
    preferIpAddress: true
    instanceId: ${spring.application.name}:${random.value}
```

#### Consul by HashiCorp
```json
{
  "service": {
    "name": "user-service",
    "port": 8080,
    "check": {
      "http": "http://localhost:8080/health",
      "interval": "10s"
    }
  }
}
```

#### Apache Zookeeper
```python
from kazoo.client import KazooClient

zk = KazooClient(hosts='127.0.0.1:2181')
zk.start()

# Register service
service_path = "/services/user-service/instance-1"
zk.create(service_path, b"192.168.1.100:8080", ephemeral=True)
```

### 3. Health Check Mechanisms

#### HTTP Health Checks
```python
@app.route('/health')
def health_check():
    # Check database connectivity
    if not database.is_connected():
        return {"status": "unhealthy", "reason": "database"}, 503
    
    # Check external dependencies
    if not external_service.is_available():
        return {"status": "degraded", "reason": "external_service"}, 200
    
    return {"status": "healthy"}, 200
```

#### TCP Health Checks
- Simple socket connection test
- Faster than HTTP checks
- Limited health information

#### Custom Health Checks
- Application-specific validation
- Business logic verification
- Dependency health aggregation

### 4. Service Registration Strategies

#### Self-Registration
```python
class ServiceRegistration:
    def __init__(self, registry_url, service_info):
        self.registry_url = registry_url
        self.service_info = service_info
    
    def register(self):
        """Register service with registry"""
        response = requests.post(
            f"{self.registry_url}/register",
            json=self.service_info
        )
        if response.status_code == 200:
            self.start_heartbeat()
    
    def start_heartbeat(self):
        """Send periodic heartbeats"""
        threading.Timer(30.0, self.send_heartbeat).start()
    
    def send_heartbeat(self):
        requests.put(
            f"{self.registry_url}/heartbeat/{self.service_info['id']}"
        )
        self.start_heartbeat()
```

#### Third-Party Registration
- External agent registers services
- Service mesh sidecar pattern
- Container orchestrator integration

### 5. Service Mesh Integration

#### Istio Service Discovery
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: ServiceEntry
metadata:
  name: external-service
spec:
  hosts:
  - external-service.com
  ports:
  - number: 80
    name: http
    protocol: HTTP
  location: MESH_EXTERNAL
```

#### Linkerd Integration
- Automatic service discovery
- Built-in load balancing
- Circuit breaker integration

### 6. DNS-Based Service Discovery

#### Kubernetes DNS
```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
  - port: 80
    targetPort: 8080
```

#### AWS Route 53 Service Discovery
```python
import boto3

client = boto3.client('servicediscovery')

# Create service
response = client.create_service(
    Name='user-service',
    NamespaceId='ns-12345',
    DnsConfig={
        'DnsRecords': [
            {
                'Type': 'A',
                'TTL': 60
            }
        ]
    }
)
```

### 7. Load Balancing Integration

#### Round Robin Discovery
```python
class ServiceDiscovery:
    def __init__(self):
        self.services = {}
        self.current_index = {}
    
    def get_instance(self, service_name):
        """Get next service instance using round robin"""
        if service_name not in self.services:
            return None
        
        instances = self.services[service_name]
        if not instances:
            return None
        
        index = self.current_index.get(service_name, 0)
        instance = instances[index % len(instances)]
        self.current_index[service_name] = index + 1
        
        return instance
```

#### Weighted Load Balancing
```python
def get_weighted_instance(self, service_name):
    """Get instance based on weights"""
    instances = self.services.get(service_name, [])
    total_weight = sum(instance.weight for instance in instances)
    
    if total_weight == 0:
        return None
    
    random_weight = random.randint(1, total_weight)
    current_weight = 0
    
    for instance in instances:
        current_weight += instance.weight
        if random_weight <= current_weight:
            return instance
    
    return instances[-1]  # Fallback
```

### 8. Failure Handling

#### Circuit Breaker Integration
```python
from circuit_breaker import CircuitBreaker

class ServiceClient:
    def __init__(self, service_discovery):
        self.discovery = service_discovery
        self.circuit_breakers = {}
    
    def call_service(self, service_name, endpoint, data=None):
        instance = self.discovery.get_instance(service_name)
        if not instance:
            raise ServiceUnavailableError(f"No instances for {service_name}")
        
        # Get or create circuit breaker for this instance
        cb_key = f"{service_name}:{instance.host}:{instance.port}"
        if cb_key not in self.circuit_breakers:
            self.circuit_breakers[cb_key] = CircuitBreaker()
        
        circuit_breaker = self.circuit_breakers[cb_key]
        
        return circuit_breaker.call(
            self._make_request, 
            instance, 
            endpoint, 
            data
        )
```

#### Retry Mechanisms
```python
def call_with_retry(self, service_name, endpoint, max_retries=3):
    """Call service with automatic retry and instance switching"""
    for attempt in range(max_retries):
        try:
            instance = self.discovery.get_instance(service_name)
            return self._make_request(instance, endpoint)
        except (ConnectionError, TimeoutError) as e:
            if attempt == max_retries - 1:
                raise e
            # Mark instance as unhealthy
            self.discovery.mark_unhealthy(instance)
            time.sleep(2 ** attempt)  # Exponential backoff
```

### 9. Security Considerations

#### Service Authentication
```python
class SecureServiceRegistry:
    def __init__(self):
        self.services = {}
        self.api_keys = {}
    
    def register_service(self, service_info, api_key):
        """Register service with authentication"""
        if not self.validate_api_key(api_key):
            raise AuthenticationError("Invalid API key")
        
        self.services[service_info['name']] = service_info
    
    def discover_services(self, api_key, service_name=None):
        """Discover services with authorization"""
        if not self.validate_api_key(api_key):
            raise AuthenticationError("Invalid API key")
        
        # Return services based on client permissions
        return self.filter_services_by_permissions(api_key, service_name)
```

#### TLS/mTLS Support
- Encrypted service communication
- Mutual authentication between services
- Certificate-based service identity

### 10. Monitoring and Observability

#### Registry Health Monitoring
```python
class RegistryMonitor:
    def __init__(self, registry):
        self.registry = registry
        self.metrics = {}
    
    def monitor_service_health(self):
        """Monitor all registered services"""
        for service_name, instances in self.registry.services.items():
            healthy_count = 0
            total_count = len(instances)
            
            for instance in instances:
                if self.health_check(instance):
                    healthy_count += 1
                else:
                    self.registry.mark_unhealthy(instance)
            
            self.metrics[service_name] = {
                'healthy': healthy_count,
                'total': total_count,
                'health_ratio': healthy_count / total_count if total_count > 0 else 0
            }
```

### 11. Benefits
- **Dynamic Scaling**: Automatic discovery of new service instances
- **Fault Tolerance**: Automatic removal of failed instances
- **Load Distribution**: Built-in load balancing capabilities
- **Service Isolation**: Loose coupling between services

### 12. Challenges
- **Single Point of Failure**: Registry availability is critical
- **Network Partitions**: Handling split-brain scenarios
- **Configuration Drift**: Keeping registry data consistent
- **Performance**: Registry lookup latency

### 13. Interview Questions
- How does service discovery enable microservices communication?
- What are the trade-offs between client-side and server-side discovery?
- How do you handle service discovery in a multi-region setup?
- Explain the role of health checks in service discovery

---
Continue to the next topic for deeper mastery!
