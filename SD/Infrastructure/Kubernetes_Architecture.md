# Kubernetes Architecture and Patterns

## Overview

Kubernetes is a portable, extensible, open-source platform for managing containerized workloads and services. This guide covers comprehensive Kubernetes architecture patterns, best practices, and real-world implementations.

## Core Architecture Components

### Control Plane Components
```yaml
# kube-apiserver configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: kube-apiserver-config
  namespace: kube-system
data:
  config.yaml: |
    apiVersion: kubeadm.k8s.io/v1beta2
    kind: ClusterConfiguration
    apiServer:
      certSANs:
      - "api.example.com"
      extraArgs:
        audit-log-maxage: "30"
        audit-log-maxbackup: "10"
        audit-log-maxsize: "100"
        audit-log-path: "/var/log/audit.log"
        enable-admission-plugins: "NodeRestriction,PodSecurityPolicy"
```

### Node Components
```yaml
# kubelet configuration
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
authentication:
  anonymous:
    enabled: false
  webhook:
    enabled: true
authorization:
  mode: Webhook
cgroupDriver: systemd
clusterDNS:
- 10.96.0.10
clusterDomain: cluster.local
resolvConf: /etc/resolv.conf
serverTLSBootstrap: true
```

## Deployment Patterns

### Blue-Green Deployment
```python
import kubernetes
from kubernetes import client, config
import time

class BlueGreenDeployment:
    def __init__(self, namespace="default"):
        config.load_incluster_config()  # or load_kube_config() for local
        self.k8s_apps = client.AppsV1Api()
        self.k8s_core = client.CoreV1Api()
        self.namespace = namespace
        
    def deploy_blue_green(self, app_name, new_image, replicas=3):
        """
        Implement blue-green deployment pattern
        """
        current_color = self._get_current_color(app_name)
        new_color = "green" if current_color == "blue" else "blue"
        
        # Deploy new version
        self._deploy_version(app_name, new_color, new_image, replicas)
        
        # Wait for new deployment to be ready
        self._wait_for_deployment_ready(f"{app_name}-{new_color}")
        
        # Run health checks
        if self._health_check_passed(app_name, new_color):
            # Switch traffic to new version
            self._switch_service(app_name, new_color)
            
            # Clean up old version
            self._cleanup_old_version(app_name, current_color)
            
            return True
        else:
            # Rollback - delete failed deployment
            self._cleanup_old_version(app_name, new_color)
            return False
            
    def _deploy_version(self, app_name, color, image, replicas):
        deployment_name = f"{app_name}-{color}"
        
        deployment = client.V1Deployment(
            metadata=client.V1ObjectMeta(name=deployment_name),
            spec=client.V1DeploymentSpec(
                replicas=replicas,
                selector=client.V1LabelSelector(
                    match_labels={"app": app_name, "version": color}
                ),
                template=client.V1PodTemplateSpec(
                    metadata=client.V1ObjectMeta(
                        labels={"app": app_name, "version": color}
                    ),
                    spec=client.V1PodSpec(
                        containers=[
                            client.V1Container(
                                name=app_name,
                                image=image,
                                ports=[client.V1ContainerPort(container_port=8080)],
                                resources=client.V1ResourceRequirements(
                                    requests={"cpu": "100m", "memory": "128Mi"},
                                    limits={"cpu": "500m", "memory": "512Mi"}
                                ),
                                readiness_probe=client.V1Probe(
                                    http_get=client.V1HTTPGetAction(
                                        path="/health",
                                        port=8080
                                    ),
                                    initial_delay_seconds=10,
                                    period_seconds=5
                                ),
                                liveness_probe=client.V1Probe(
                                    http_get=client.V1HTTPGetAction(
                                        path="/health",
                                        port=8080
                                    ),
                                    initial_delay_seconds=30,
                                    period_seconds=10
                                )
                            )
                        ]
                    )
                )
            )
        )
        
        self.k8s_apps.create_namespaced_deployment(
            body=deployment,
            namespace=self.namespace
        )
        
    def _switch_service(self, app_name, new_color):
        service = self.k8s_core.read_namespaced_service(
            name=app_name,
            namespace=self.namespace
        )
        
        service.spec.selector["version"] = new_color
        
        self.k8s_core.patch_namespaced_service(
            name=app_name,
            namespace=self.namespace,
            body=service
        )
```

### Canary Deployment
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: canary-rollout
spec:
  replicas: 10
  strategy:
    canary:
      steps:
      - setWeight: 10
      - pause: {duration: 30s}
      - setWeight: 20
      - pause: {duration: 30s}
      - setWeight: 50
      - pause: {duration: 30s}
      - setWeight: 100
      canaryService: canary-service
      stableService: stable-service
      trafficRouting:
        istio:
          virtualService:
            name: rollout-vsvc
            routes:
            - primary
  selector:
    matchLabels:
      app: canary-app
  template:
    metadata:
      labels:
        app: canary-app
    spec:
      containers:
      - name: app
        image: nginx:1.20
        ports:
        - containerPort: 80
```

## Service Mesh Integration

### Istio Configuration
```yaml
# Virtual Service for traffic management
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: app-routing
spec:
  hosts:
  - app.example.com
  gateways:
  - app-gateway
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: app-service
        subset: canary
  - route:
    - destination:
        host: app-service
        subset: stable
      weight: 90
    - destination:
        host: app-service
        subset: canary
      weight: 10

---
# Destination Rule for load balancing
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: app-destination
spec:
  host: app-service
  subsets:
  - name: stable
    labels:
      version: stable
  - name: canary
    labels:
      version: canary
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
    circuitBreaker:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
```

## Auto-scaling Patterns

### Horizontal Pod Autoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: app-deployment
  minReplicas: 3
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: custom_metric
      target:
        type: AverageValue
        averageValue: "100"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 30
      policies:
      - type: Percent
        value: 50
        periodSeconds: 15
```

### Vertical Pod Autoscaler
```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: app-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: app-deployment
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: app
      maxAllowed:
        cpu: 2
        memory: 4Gi
      minAllowed:
        cpu: 100m
        memory: 128Mi
      controlledResources: ["cpu", "memory"]
      controlledValues: RequestsAndLimits
```

## Cluster Autoscaler
```python
import boto3
from kubernetes import client, config

class ClusterAutoscaler:
    def __init__(self, cluster_name, region):
        self.cluster_name = cluster_name
        self.region = region
        self.asg_client = boto3.client('autoscaling', region_name=region)
        self.ec2_client = boto3.client('ec2', region_name=region)
        
        config.load_incluster_config()
        self.k8s_core = client.CoreV1Api()
        
    def scale_cluster(self, desired_capacity):
        """
        Scale cluster by adjusting Auto Scaling Group
        """
        # Get Auto Scaling Groups for the cluster
        asgs = self._get_cluster_asgs()
        
        for asg in asgs:
            self.asg_client.update_auto_scaling_group(
                AutoScalingGroupName=asg['AutoScalingGroupName'],
                DesiredCapacity=desired_capacity,
                MinSize=min(asg['MinSize'], desired_capacity),
                MaxSize=max(asg['MaxSize'], desired_capacity)
            )
            
    def check_node_utilization(self):
        """
        Check node resource utilization
        """
        nodes = self.k8s_core.list_node()
        
        node_metrics = []
        for node in nodes.items:
            # Get node allocatable resources
            allocatable = node.status.allocatable
            
            # Get pod resource requests on this node
            field_selector = f"spec.nodeName={node.metadata.name}"
            pods = self.k8s_core.list_pod_for_all_namespaces(
                field_selector=field_selector
            )
            
            total_cpu_requests = 0
            total_memory_requests = 0
            
            for pod in pods.items:
                for container in pod.spec.containers:
                    if container.resources and container.resources.requests:
                        if 'cpu' in container.resources.requests:
                            total_cpu_requests += self._parse_cpu(
                                container.resources.requests['cpu']
                            )
                        if 'memory' in container.resources.requests:
                            total_memory_requests += self._parse_memory(
                                container.resources.requests['memory']
                            )
                            
            node_metrics.append({
                'node_name': node.metadata.name,
                'cpu_utilization': total_cpu_requests / self._parse_cpu(allocatable['cpu']),
                'memory_utilization': total_memory_requests / self._parse_memory(allocatable['memory']),
                'pod_count': len(pods.items)
            })
            
        return node_metrics
        
    def _parse_cpu(self, cpu_string):
        """Parse CPU string to millicores"""
        if cpu_string.endswith('m'):
            return int(cpu_string[:-1])
        else:
            return int(float(cpu_string) * 1000)
            
    def _parse_memory(self, memory_string):
        """Parse memory string to bytes"""
        units = {'Ki': 1024, 'Mi': 1024**2, 'Gi': 1024**3}
        for unit, multiplier in units.items():
            if memory_string.endswith(unit):
                return int(memory_string[:-2]) * multiplier
        return int(memory_string)
```

## Storage Patterns

### StatefulSet with Persistent Storage
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: database-cluster
spec:
  serviceName: database-service
  replicas: 3
  selector:
    matchLabels:
      app: database
  template:
    metadata:
      labels:
        app: database
    spec:
      containers:
      - name: database
        image: postgres:13
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: "myapp"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: password
        volumeMounts:
        - name: database-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 2
            memory: 4Gi
  volumeClaimTemplates:
  - metadata:
      name: database-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 100Gi

---
apiVersion: v1
kind: Service
metadata:
  name: database-service
spec:
  clusterIP: None
  selector:
    app: database
  ports:
  - port: 5432
    targetPort: 5432
```

## Security Best Practices

### Pod Security Standards
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
  annotations:
    seccomp.security.alpha.kubernetes.io/pod: runtime/default
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    runAsGroup: 3000
    fsGroup: 2000
    supplementalGroups: [4000]
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: app
    image: nginx:1.20
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      runAsNonRoot: true
      runAsUser: 1000
      capabilities:
        drop:
        - ALL
        add:
        - NET_BIND_SERVICE
    resources:
      requests:
        cpu: 100m
        memory: 128Mi
      limits:
        cpu: 500m
        memory: 512Mi
    volumeMounts:
    - name: temp-volume
      mountPath: /tmp
    - name: cache-volume
      mountPath: /var/cache/nginx
  volumes:
  - name: temp-volume
    emptyDir: {}
  - name: cache-volume
    emptyDir: {}
```

### Network Policies
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: frontend-network-policy
spec:
  podSelector:
    matchLabels:
      tier: frontend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: loadbalancer
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          tier: backend
    ports:
    - protocol: TCP
      port: 3000
  - to: []
    ports:
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53

---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-network-policy
spec:
  podSelector:
    matchLabels:
      tier: backend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: frontend
    ports:
    - protocol: TCP
      port: 3000
  egress:
  - to:
    - podSelector:
        matchLabels:
          tier: database
    ports:
    - protocol: TCP
      port: 5432
```

## Monitoring and Observability

### Prometheus Configuration
```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: app-service-monitor
spec:
  selector:
    matchLabels:
      app: myapp
  endpoints:
  - port: metrics
    interval: 30s
    path: /metrics

---
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: app-alerts
spec:
  groups:
  - name: app.rules
    rules:
    - alert: HighPodCPU
      expr: rate(container_cpu_usage_seconds_total[5m]) > 0.8
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High CPU usage detected"
        description: "Pod {{ $labels.pod }} has high CPU usage"
        
    - alert: PodMemoryUsage
      expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.9
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: "High memory usage detected"
        description: "Pod {{ $labels.pod }} is using {{ $value | humanizePercentage }} of its memory limit"
```

### Distributed Tracing
```python
from opentelemetry import trace
from opentelemetry.exporter.jaeger.thrift import JaegerExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.instrumentation.kubernetes import KubernetesInstrumentor

class KubernetesTracing:
    def __init__(self, service_name, jaeger_endpoint):
        self.service_name = service_name
        
        # Set up tracing
        trace.set_tracer_provider(TracerProvider())
        tracer = trace.get_tracer(__name__)
        
        # Configure Jaeger exporter
        jaeger_exporter = JaegerExporter(
            agent_host_name="jaeger-agent",
            agent_port=6831,
        )
        
        span_processor = BatchSpanProcessor(jaeger_exporter)
        trace.get_tracer_provider().add_span_processor(span_processor)
        
        # Auto-instrument Kubernetes API calls
        KubernetesInstrumentor().instrument()
        
    def trace_deployment(self, deployment_name):
        tracer = trace.get_tracer(__name__)
        
        with tracer.start_as_current_span("kubernetes_deployment") as span:
            span.set_attribute("deployment.name", deployment_name)
            span.set_attribute("service.name", self.service_name)
            
            # Your deployment logic here
            self._perform_deployment(deployment_name)
            
            span.set_attribute("deployment.status", "completed")
```

## Cost Optimization

### Resource Quotas and Limits
```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: namespace-quota
  namespace: production
spec:
  hard:
    requests.cpu: "100"
    requests.memory: 200Gi
    limits.cpu: "200"
    limits.memory: 400Gi
    persistentvolumeclaims: "10"
    pods: "50"
    services: "20"
    secrets: "30"
    configmaps: "20"

---
apiVersion: v1
kind: LimitRange
metadata:
  name: default-limits
  namespace: production
spec:
  limits:
  - default:
      cpu: 500m
      memory: 512Mi
    defaultRequest:
      cpu: 100m
      memory: 128Mi
    type: Container
  - max:
      cpu: 2
      memory: 4Gi
    min:
      cpu: 50m
      memory: 64Mi
    type: Container
```

## Best Practices

### 1. Resource Management
- Set appropriate resource requests and limits
- Use Horizontal Pod Autoscaler for dynamic scaling
- Implement proper health checks
- Use init containers for setup tasks

### 2. Security
- Follow Pod Security Standards
- Implement Network Policies
- Use service accounts with minimal permissions
- Scan images for vulnerabilities

### 3. Observability
- Implement comprehensive monitoring
- Use distributed tracing
- Set up proper logging
- Create meaningful alerts

### 4. High Availability
- Distribute workloads across multiple nodes
- Use pod disruption budgets
- Implement proper backup strategies
- Test disaster recovery procedures

### 5. Performance
- Optimize container images
- Use appropriate storage classes
- Implement caching strategies
- Monitor and tune resource usage

## Common Anti-Patterns

1. **Running as Root**: Security vulnerability
2. **No Resource Limits**: Can cause resource starvation
3. **Tight Coupling**: Services too dependent on each other
4. **Missing Health Checks**: Reduces reliability
5. **Ignoring Security Context**: Potential security issues
6. **Over-privileged Containers**: Violates principle of least privilege

## Conclusion

Kubernetes provides a powerful platform for container orchestration, but success requires understanding and implementing appropriate patterns for deployment, scaling, security, and monitoring. The key is to start with basic patterns and gradually adopt more advanced techniques as your understanding and requirements grow.
