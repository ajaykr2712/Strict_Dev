# Deployment and Operations Guide

## Overview
This guide covers the deployment, monitoring, and operational aspects of the e-commerce platform, following DevOps best practices and production-ready patterns.

## Infrastructure as Code

### Docker Configuration

#### Application Dockerfile
```dockerfile
FROM openjdk:17-jre-slim

# Create application user
RUN addgroup --system --gid 1001 ecommerce && \
    adduser --system --uid 1001 --gid 1001 ecommerce

# Set working directory
WORKDIR /app

# Copy application JAR
COPY target/ecommerce-platform.jar app.jar

# Set ownership
RUN chown ecommerce:ecommerce app.jar

# Switch to non-root user
USER ecommerce

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

#### Docker Compose for Development
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ecommerce
      - REDIS_URL=redis://redis:6379
      - KAFKA_BROKERS=kafka:9092
    depends_on:
      - postgres
      - redis
      - kafka

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ecommerce
      POSTGRES_USER: ecommerce
      POSTGRES_PASSWORD: secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

volumes:
  postgres_data:
  redis_data:
```

### Kubernetes Deployment

#### Application Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ecommerce-app
  labels:
    app: ecommerce-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ecommerce-app
  template:
    metadata:
      labels:
        app: ecommerce-app
    spec:
      containers:
      - name: app
        image: ecommerce-platform:latest
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: REDIS_URL
          valueFrom:
            configMapKeyRef:
              name: cache-config
              key: redis-url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

#### Service Configuration
```yaml
apiVersion: v1
kind: Service
metadata:
  name: ecommerce-service
spec:
  selector:
    app: ecommerce-app
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

#### Ingress Configuration
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - api.ecommerce.com
    secretName: ecommerce-tls
  rules:
  - host: api.ecommerce.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ecommerce-service
            port:
              number: 80
```

## CI/CD Pipeline

### GitHub Actions Workflow
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: mvn clean test
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build application
      run: mvn clean package -DskipTests
    
    - name: Build Docker image
      run: |
        docker build -t ecommerce-platform:${{ github.sha }} .
        docker tag ecommerce-platform:${{ github.sha }} ecommerce-platform:latest
    
    - name: Push to registry
      run: |
        echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
        docker push ecommerce-platform:${{ github.sha }}
        docker push ecommerce-platform:latest

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Deploy to Kubernetes
      run: |
        echo "${{ secrets.KUBECONFIG }}" | base64 -d > kubeconfig
        export KUBECONFIG=kubeconfig
        kubectl set image deployment/ecommerce-app app=ecommerce-platform:${{ github.sha }}
        kubectl rollout status deployment/ecommerce-app
```

## Monitoring and Observability

### Prometheus Configuration
```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ecommerce-app'
    static_configs:
      - targets: ['app:8080']
    metrics_path: /actuator/prometheus
    scrape_interval: 15s

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

### Grafana Dashboards

#### Application Metrics Dashboard
```json
{
  "dashboard": {
    "title": "E-Commerce Platform Metrics",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m]) / rate(http_requests_total[5m])",
            "legendFormat": "Error Rate"
          }
        ]
      }
    ]
  }
}
```

### Alerting Rules
```yaml
# alerting-rules.yml
groups:
  - name: ecommerce-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} for the last 5 minutes"

      - alert: DatabaseConnectionsHigh
        expr: postgres_stat_activity_count > 80
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High number of database connections"
          description: "{{ $value }} database connections active"

      - alert: CacheHitRateLow
        expr: redis_cache_hit_rate < 0.8
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Low cache hit rate"
          description: "Cache hit rate is {{ $value }}"
```

## Security Configuration

### Application Security
```yaml
# security.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.ecommerce.com
          
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
      
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### Network Security
```yaml
# NetworkPolicy for Kubernetes
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ecommerce-network-policy
spec:
  podSelector:
    matchLabels:
      app: ecommerce-app
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: nginx-ingress
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
```

## Backup and Disaster Recovery

### Database Backup Strategy
```bash
#!/bin/bash
# backup-script.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"
DB_NAME="ecommerce"

# Create backup
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME -F c -b -v -f "$BACKUP_DIR/ecommerce_backup_$DATE.dump"

# Upload to S3
aws s3 cp "$BACKUP_DIR/ecommerce_backup_$DATE.dump" s3://ecommerce-backups/database/

# Cleanup old backups (keep last 7 days)
find $BACKUP_DIR -name "ecommerce_backup_*.dump" -mtime +7 -delete

# Verify backup integrity
pg_restore --list "$BACKUP_DIR/ecommerce_backup_$DATE.dump" > /dev/null
if [ $? -eq 0 ]; then
    echo "Backup created successfully: ecommerce_backup_$DATE.dump"
else
    echo "Backup verification failed!"
    exit 1
fi
```

### Disaster Recovery Plan
```yaml
# disaster-recovery.yml
recovery_procedures:
  database_failure:
    - Switch to read replica
    - Promote replica to master
    - Update application configuration
    - Restore from latest backup if needed
    
  application_failure:
    - Roll back to previous version
    - Scale up healthy instances
    - Check system dependencies
    - Implement circuit breaker
    
  infrastructure_failure:
    - Failover to secondary region
    - Update DNS records
    - Restore from cross-region backups
    - Validate system functionality
```

## Performance Tuning

### JVM Configuration
```bash
# jvm-options.txt
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled
-XX:+UseStringDeduplication
-XX:MaxRAMPercentage=75
-XX:InitialRAMPercentage=50
-Xlog:gc*:gc.log:time
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/dumps/
```

### Database Optimization
```sql
-- PostgreSQL configuration
shared_buffers = '256MB'
effective_cache_size = '1GB'
maintenance_work_mem = '64MB'
checkpoint_completion_target = 0.9
wal_buffers = '16MB'
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
```

### Redis Configuration
```conf
# redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru
tcp-keepalive 300
timeout 0
tcp-backlog 511
databases 16
save 900 1
save 300 10
save 60 10000
```

## Maintenance Procedures

### Rolling Updates
```bash
#!/bin/bash
# rolling-update.sh

NEW_VERSION=$1
REPLICAS=$(kubectl get deployment ecommerce-app -o jsonpath='{.spec.replicas}')

echo "Starting rolling update to version $NEW_VERSION"

# Update deployment
kubectl set image deployment/ecommerce-app app=ecommerce-platform:$NEW_VERSION

# Wait for rollout to complete
kubectl rollout status deployment/ecommerce-app --timeout=600s

# Verify deployment
if kubectl rollout status deployment/ecommerce-app | grep -q "successfully rolled out"; then
    echo "Deployment successful"
else
    echo "Deployment failed, rolling back"
    kubectl rollout undo deployment/ecommerce-app
    exit 1
fi
```

### Health Checks
```bash
#!/bin/bash
# health-check.sh

HEALTH_ENDPOINT="http://localhost:8080/actuator/health"
MAX_RETRIES=5
RETRY_DELAY=10

for i in $(seq 1 $MAX_RETRIES); do
    if curl -f -s $HEALTH_ENDPOINT > /dev/null; then
        echo "Health check passed"
        exit 0
    else
        echo "Health check failed, attempt $i/$MAX_RETRIES"
        sleep $RETRY_DELAY
    fi
done

echo "Health check failed after $MAX_RETRIES attempts"
exit 1
```
