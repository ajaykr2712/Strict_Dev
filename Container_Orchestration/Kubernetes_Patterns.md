# Container Orchestration and Kubernetes Patterns

This guide covers advanced container orchestration patterns using Kubernetes, based on real-world deployments at companies like Netflix, Uber, and WhatsApp.

## 1. Microservices Deployment Patterns

### Deployment Strategies

#### Blue-Green Deployment
```yaml
# Blue environment (current production)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: recommendation-service-blue
  labels:
    app: recommendation-service
    version: blue
    environment: production
spec:
  replicas: 10
  selector:
    matchLabels:
      app: recommendation-service
      version: blue
  template:
    metadata:
      labels:
        app: recommendation-service
        version: blue
    spec:
      containers:
      - name: recommendation-service
        image: netflix/recommendation-service:v1.2.3
        ports:
        - containerPort: 8080
        env:
        - name: ENVIRONMENT
          value: "production"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5

---
# Green environment (new version)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: recommendation-service-green
  labels:
    app: recommendation-service
    version: green
    environment: staging
spec:
  replicas: 0  # Initially scaled to 0
  selector:
    matchLabels:
      app: recommendation-service
      version: green
  template:
    metadata:
      labels:
        app: recommendation-service
        version: green
    spec:
      containers:
      - name: recommendation-service
        image: netflix/recommendation-service:v1.3.0
        ports:
        - containerPort: 8080
        env:
        - name: ENVIRONMENT
          value: "production"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"

---
# Service routing traffic
apiVersion: v1
kind: Service
metadata:
  name: recommendation-service
spec:
  selector:
    app: recommendation-service
    version: blue  # Switch to green during deployment
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

#### Canary Deployment with Istio
```yaml
# Canary deployment configuration
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: recommendation-service-canary
spec:
  hosts:
  - recommendation-service
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: recommendation-service
        subset: canary
      weight: 100
  - route:
    - destination:
        host: recommendation-service
        subset: stable
      weight: 95
    - destination:
        host: recommendation-service
        subset: canary
      weight: 5  # 5% traffic to canary

---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: recommendation-service-destination
spec:
  host: recommendation-service
  subsets:
  - name: stable
    labels:
      version: v1.2.3
  - name: canary
    labels:
      version: v1.3.0
```

## 2. Auto-scaling Patterns

### Horizontal Pod Autoscaler (HPA)
```yaml
# HPA for Netflix recommendation service
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: recommendation-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: recommendation-service
  minReplicas: 5
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
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100  # Double the replicas
        periodSeconds: 60
      - type: Pods
        value: 10   # Or add 10 pods
        periodSeconds: 60
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10   # Remove 10% of replicas
        periodSeconds: 60
```

### Vertical Pod Autoscaler (VPA)
```yaml
# VPA for resource optimization
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: recommendation-service-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: recommendation-service
  updatePolicy:
    updateMode: "Auto"  # Automatically apply recommendations
  resourcePolicy:
    containerPolicies:
    - containerName: recommendation-service
      minAllowed:
        cpu: 100m
        memory: 256Mi
      maxAllowed:
        cpu: 2000m
        memory: 4Gi
      controlledResources: ["cpu", "memory"]
      controlledValues: RequestsAndLimits
```

### Cluster Autoscaler
```yaml
# Node pool configuration for GKE
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-autoscaler-config
  namespace: kube-system
data:
  nodes.max: "100"
  nodes.min: "10"
  scale-down-delay-after-add: "10m"
  scale-down-unneeded-time: "10m"
  skip-nodes-with-local-storage: "false"
  skip-nodes-with-system-pods: "false"

---
# Cluster autoscaler deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cluster-autoscaler
  template:
    metadata:
      labels:
        app: cluster-autoscaler
    spec:
      containers:
      - image: k8s.gcr.io/autoscaling/cluster-autoscaler:v1.21.0
        name: cluster-autoscaler
        resources:
          limits:
            cpu: 100m
            memory: 300Mi
          requests:
            cpu: 100m
            memory: 300Mi
        command:
        - ./cluster-autoscaler
        - --v=4
        - --stderrthreshold=info
        - --cloud-provider=gce
        - --skip-nodes-with-local-storage=false
        - --expander=least-waste
        - --node-group-auto-discovery=mig:name=gke-cluster-default-pool
        - --scale-down-delay-after-add=10m
        - --scale-down-unneeded-time=10m
```

## 3. Service Mesh Patterns

### Istio Configuration for Netflix-style Architecture
```yaml
# Gateway for external traffic
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: netflix-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: netflix-tls-secret
    hosts:
    - api.netflix.com

---
# Virtual service for API routing
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: netflix-api-routing
spec:
  hosts:
  - api.netflix.com
  gateways:
  - netflix-gateway
  http:
  - match:
    - uri:
        prefix: /api/v1/recommendations
    route:
    - destination:
        host: recommendation-service
        port:
          number: 80
    timeout: 30s
    retries:
      attempts: 3
      perTryTimeout: 10s
  - match:
    - uri:
        prefix: /api/v1/content
    route:
    - destination:
        host: content-service
        port:
          number: 80
  - match:
    - uri:
        prefix: /api/v1/user
    route:
    - destination:
        host: user-service
        port:
          number: 80

---
# Destination rules for load balancing
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: recommendation-service-destination
spec:
  host: recommendation-service
  trafficPolicy:
    loadBalancer:
      consistentHash:
        httpHeader: "user-id"  # Consistent hashing by user ID
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
    circuitBreaker:
      consecutiveErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

### Security Policies
```yaml
# Network policy for Netflix microservices
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: recommendation-service-netpol
spec:
  podSelector:
    matchLabels:
      app: recommendation-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: api-gateway
    - podSelector:
        matchLabels:
          app: content-service
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: user-service
    ports:
    - protocol: TCP
      port: 8080
  - to:
    - podSelector:
        matchLabels:
          app: analytics-service
    ports:
    - protocol: TCP
      port: 8080
  # Allow DNS
  - to: []
    ports:
    - protocol: UDP
      port: 53

---
# Pod Security Policy
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: netflix-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

## 4. Configuration Management

### ConfigMaps and Secrets
```yaml
# Application configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: recommendation-service-config
data:
  application.yml: |
    server:
      port: 8080
    spring:
      profiles:
        active: production
    app:
      recommendation:
        algorithm: "collaborative-filtering"
        cache:
          ttl: 3600
          size: 10000
      external-services:
        user-service: "http://user-service:8080"
        content-service: "http://content-service:8080"
        analytics-service: "http://analytics-service:8080"
      monitoring:
        metrics:
          enabled: true
          endpoint: "/metrics"
        tracing:
          enabled: true
          jaeger:
            endpoint: "http://jaeger-collector:14268"

---
# Database credentials
apiVersion: v1
kind: Secret
metadata:
  name: database-credentials
type: Opaque
data:
  username: dXNlcm5hbWU=  # base64 encoded
  password: cGFzc3dvcmQ=  # base64 encoded
  connection-string: cG9zdGdyZXNxbDovL3VzZXJuYW1lOnBhc3N3b3JkQGRiLmV4YW1wbGUuY29tOjU0MzIvbmV0ZmxpeA==

---
# TLS certificates
apiVersion: v1
kind: Secret
metadata:
  name: netflix-tls-secret
  namespace: istio-system
type: kubernetes.io/tls
data:
  tls.crt: LS0tLS1CRUdJTi... # base64 encoded certificate
  tls.key: LS0tLS1CRUdJTi... # base64 encoded private key
```

### External Secrets Operator
```yaml
# External secret for AWS Secrets Manager
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: database-credentials
    creationPolicy: Owner
  data:
  - secretKey: username
    remoteRef:
      key: prod/database/credentials
      property: username
  - secretKey: password
    remoteRef:
      key: prod/database/credentials
      property: password

---
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
      auth:
        secretRef:
          accessKeyIDSecretRef:
            name: aws-credentials
            key: access-key-id
          secretAccessKeySecretRef:
            name: aws-credentials
            key: secret-access-key
```

## 5. Persistent Storage Patterns

### StatefulSet for Database
```yaml
# PostgreSQL StatefulSet for Netflix user data
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql-primary
spec:
  serviceName: postgresql-primary
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
      role: primary
  template:
    metadata:
      labels:
        app: postgresql
        role: primary
    spec:
      containers:
      - name: postgresql
        image: postgres:13
        env:
        - name: POSTGRES_DB
          value: netflix
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgresql-storage
          mountPath: /var/lib/postgresql/data
        - name: postgresql-config
          mountPath: /etc/postgresql
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - pg_isready -U $POSTGRES_USER -d $POSTGRES_DB
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - pg_isready -U $POSTGRES_USER -d $POSTGRES_DB
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: postgresql-config
        configMap:
          name: postgresql-config
  volumeClaimTemplates:
  - metadata:
      name: postgresql-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 100Gi

---
# PostgreSQL configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-config
data:
  postgresql.conf: |
    # Performance tuning for Netflix workload
    shared_buffers = 1GB
    effective_cache_size = 3GB
    maintenance_work_mem = 256MB
    checkpoint_completion_target = 0.9
    wal_buffers = 16MB
    default_statistics_target = 100
    random_page_cost = 1.1
    effective_io_concurrency = 200
    
    # Connection settings
    max_connections = 200
    
    # Logging
    log_statement = 'all'
    log_duration = on
    log_checkpoints = on
```

### Redis Cluster for Caching
```yaml
# Redis cluster for Netflix recommendation caching
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis-cluster
spec:
  serviceName: redis-cluster
  replicas: 6
  selector:
    matchLabels:
      app: redis-cluster
  template:
    metadata:
      labels:
        app: redis-cluster
    spec:
      containers:
      - name: redis
        image: redis:6.2-alpine
        command:
        - redis-server
        - /etc/redis/redis.conf
        - --cluster-enabled
        - "yes"
        - --cluster-config-file
        - nodes.conf
        - --cluster-node-timeout
        - "5000"
        - --appendonly
        - "yes"
        ports:
        - containerPort: 6379
          name: client
        - containerPort: 16379
          name: gossip
        volumeMounts:
        - name: redis-data
          mountPath: /data
        - name: redis-config
          mountPath: /etc/redis
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
      volumes:
      - name: redis-config
        configMap:
          name: redis-config
  volumeClaimTemplates:
  - metadata:
      name: redis-data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 20Gi

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
data:
  redis.conf: |
    # Redis configuration for Netflix caching
    maxmemory 1.5gb
    maxmemory-policy allkeys-lru
    save 900 1
    save 300 10
    save 60 10000
    tcp-keepalive 60
    tcp-backlog 511
```

## 6. Monitoring and Observability

### Prometheus Monitoring
```yaml
# ServiceMonitor for Prometheus scraping
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: recommendation-service-monitor
  labels:
    app: recommendation-service
spec:
  selector:
    matchLabels:
      app: recommendation-service
  endpoints:
  - port: metrics
    interval: 30s
    path: /metrics
    honorLabels: true

---
# PrometheusRule for alerting
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: recommendation-service-alerts
spec:
  groups:
  - name: recommendation-service
    rules:
    - alert: HighErrorRate
      expr: |
        (
          rate(http_requests_total{app="recommendation-service", status=~"5.."}[5m])
          /
          rate(http_requests_total{app="recommendation-service"}[5m])
        ) > 0.05
      for: 5m
      labels:
        severity: critical
        service: recommendation-service
      annotations:
        summary: "High error rate detected"
        description: "Error rate is {{ $value | humanizePercentage }} for {{ $labels.instance }}"
    
    - alert: HighLatency
      expr: |
        histogram_quantile(0.95, 
          rate(http_request_duration_seconds_bucket{app="recommendation-service"}[5m])
        ) > 0.5
      for: 5m
      labels:
        severity: warning
        service: recommendation-service
      annotations:
        summary: "High latency detected"
        description: "95th percentile latency is {{ $value }}s for {{ $labels.instance }}"
```

### Grafana Dashboard Configuration
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: netflix-dashboard
data:
  dashboard.json: |
    {
      "dashboard": {
        "title": "Netflix Microservices Dashboard",
        "panels": [
          {
            "title": "Request Rate",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(http_requests_total{app=~\".*-service\"}[5m])",
                "legendFormat": "{{ app }} - {{ method }}"
              }
            ]
          },
          {
            "title": "Error Rate",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(http_requests_total{app=~\".*-service\", status=~\"5..\"}[5m]) / rate(http_requests_total{app=~\".*-service\"}[5m])",
                "legendFormat": "{{ app }} Error Rate"
              }
            ]
          },
          {
            "title": "Response Time P95",
            "type": "graph",
            "targets": [
              {
                "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{app=~\".*-service\"}[5m]))",
                "legendFormat": "{{ app }} P95"
              }
            ]
          }
        ]
      }
    }
```

## 7. Resource Management

### Resource Quotas
```yaml
# Namespace resource quota for Netflix production
apiVersion: v1
kind: ResourceQuota
metadata:
  name: netflix-production-quota
  namespace: netflix-production
spec:
  hard:
    requests.cpu: "100"
    requests.memory: 200Gi
    limits.cpu: "200"
    limits.memory: 400Gi
    persistentvolumeclaims: "20"
    pods: "100"
    services: "20"
    secrets: "20"
    configmaps: "20"

---
# Limit ranges for containers
apiVersion: v1
kind: LimitRange
metadata:
  name: netflix-limit-range
  namespace: netflix-production
spec:
  limits:
  - default:
      cpu: "1000m"
      memory: "1Gi"
    defaultRequest:
      cpu: "100m"
      memory: "128Mi"
    max:
      cpu: "4000m"
      memory: "8Gi"
    min:
      cpu: "50m"
      memory: "64Mi"
    type: Container
```

### Priority Classes
```yaml
# Priority class for critical services
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: netflix-critical
value: 1000000
globalDefault: false
description: "Critical Netflix services that must not be preempted"

---
# Priority class for normal services
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: netflix-normal
value: 100000
globalDefault: true
description: "Normal Netflix services"

---
# Priority class for batch jobs
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: netflix-batch
value: 1000
globalDefault: false
description: "Netflix batch processing jobs that can be preempted"
```

## Best Practices Summary

1. **Resource Management**: Set appropriate requests and limits for all containers
2. **Health Checks**: Implement comprehensive liveness and readiness probes
3. **Security**: Use network policies, pod security policies, and least privilege access
4. **Monitoring**: Implement comprehensive observability with metrics, logs, and traces
5. **Auto-scaling**: Configure HPA, VPA, and cluster autoscaler for dynamic scaling
6. **High Availability**: Use anti-affinity rules and multiple availability zones
7. **Configuration**: Externalize configuration using ConfigMaps and Secrets
8. **Storage**: Use appropriate storage classes and backup strategies
9. **Networking**: Implement service mesh for advanced traffic management
10. **GitOps**: Use GitOps workflows for deployment automation and version control
