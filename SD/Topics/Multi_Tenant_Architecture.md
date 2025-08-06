# Multi-Tenant Architecture

## Overview
Multi-tenancy is an architectural pattern where a single instance of software serves multiple tenants (customers/organizations), with each tenant's data and configuration isolated from others. This approach maximizes resource utilization while providing logical separation and customization for each tenant.

## Core Concepts

### Tenancy Models
1. **Single-Tenant**: Dedicated infrastructure per customer
2. **Multi-Tenant**: Shared infrastructure with logical separation
3. **Hybrid**: Combination approach based on requirements

### Key Requirements
- **Isolation**: Data and process separation between tenants
- **Scalability**: Handle varying tenant sizes and loads
- **Customization**: Tenant-specific configurations and features
- **Security**: Prevent data leakage between tenants
- **Performance**: Maintain SLAs across all tenants

## Multi-Tenancy Patterns

### 1. Database-Level Patterns

#### Shared Database, Shared Schema
All tenants share the same database and tables with tenant ID columns.

```python
# Example: Shared schema implementation
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

Base = declarative_base()

class Tenant(Base):
    __tablename__ = 'tenants'
    
    id = Column(String, primary_key=True)
    name = Column(String, nullable=False)
    plan = Column(String, nullable=False)
    created_at = Column(DateTime, nullable=False)

class User(Base):
    __tablename__ = 'users'
    
    id = Column(Integer, primary_key=True)
    tenant_id = Column(String, ForeignKey('tenants.id'), nullable=False)
    email = Column(String, nullable=False)
    name = Column(String, nullable=False)

class Order(Base):
    __tablename__ = 'orders'
    
    id = Column(Integer, primary_key=True)
    tenant_id = Column(String, ForeignKey('tenants.id'), nullable=False)
    user_id = Column(Integer, ForeignKey('users.id'), nullable=False)
    amount = Column(Integer, nullable=False)
    status = Column(String, nullable=False)

# Tenant-aware query wrapper
class TenantAwareQuery:
    def __init__(self, session, tenant_id):
        self.session = session
        self.tenant_id = tenant_id
    
    def get_users(self):
        return self.session.query(User).filter(User.tenant_id == self.tenant_id).all()
    
    def get_orders(self, user_id=None):
        query = self.session.query(Order).filter(Order.tenant_id == self.tenant_id)
        if user_id:
            query = query.filter(Order.user_id == user_id)
        return query.all()
    
    def create_user(self, email, name):
        user = User(tenant_id=self.tenant_id, email=email, name=name)
        self.session.add(user)
        self.session.commit()
        return user
```

**Pros:**
- Simple implementation
- Cost-effective for large numbers of tenants
- Easy to maintain

**Cons:**
- Limited customization
- Security risks from shared schema
- Potential for data leakage

#### Shared Database, Separate Schema
Each tenant gets their own database schema.

```python
# Example: Schema-per-tenant implementation
class SchemaPerTenantManager:
    def __init__(self, base_connection_string):
        self.base_connection_string = base_connection_string
        self.schema_cache = {}
    
    def get_tenant_schema_name(self, tenant_id):
        return f"tenant_{tenant_id}"
    
    def create_tenant_schema(self, tenant_id):
        schema_name = self.get_tenant_schema_name(tenant_id)
        
        # Create schema
        with self.get_admin_connection() as conn:
            conn.execute(f"CREATE SCHEMA IF NOT EXISTS {schema_name}")
            
            # Create tables in the new schema
            self.create_tenant_tables(conn, schema_name)
    
    def get_tenant_connection(self, tenant_id):
        schema_name = self.get_tenant_schema_name(tenant_id)
        return create_engine(
            f"{self.base_connection_string}",
            connect_args={"options": f"-csearch_path={schema_name}"}
        )
    
    def create_tenant_tables(self, connection, schema_name):
        # Set search path and create tables
        connection.execute(f"SET search_path TO {schema_name}")
        Base.metadata.create_all(connection)

# Usage
tenant_manager = SchemaPerTenantManager("postgresql://user:pass@localhost/app")
tenant_manager.create_tenant_schema("tenant_123")

# Get tenant-specific connection
tenant_engine = tenant_manager.get_tenant_connection("tenant_123")
TenantSession = sessionmaker(bind=tenant_engine)
```

#### Separate Database
Each tenant gets their own database instance.

```python
# Example: Database-per-tenant implementation
class DatabasePerTenantManager:
    def __init__(self, base_connection_string):
        self.base_connection_string = base_connection_string
        self.tenant_databases = {}
    
    def get_tenant_database_name(self, tenant_id):
        return f"tenant_{tenant_id}_db"
    
    def provision_tenant_database(self, tenant_id):
        db_name = self.get_tenant_database_name(tenant_id)
        
        # Create database
        with self.get_admin_connection() as conn:
            conn.execute(f"CREATE DATABASE {db_name}")
        
        # Initialize schema
        tenant_engine = self.get_tenant_engine(tenant_id)
        Base.metadata.create_all(tenant_engine)
        
        return tenant_engine
    
    def get_tenant_engine(self, tenant_id):
        if tenant_id not in self.tenant_databases:
            db_name = self.get_tenant_database_name(tenant_id)
            connection_string = f"{self.base_connection_string}/{db_name}"
            self.tenant_databases[tenant_id] = create_engine(connection_string)
        
        return self.tenant_databases[tenant_id]
```

### 2. Application-Level Patterns

#### Tenant Context Management
```python
# Example: Tenant context implementation
from contextvars import ContextVar
from functools import wraps

# Global tenant context
current_tenant: ContextVar[str] = ContextVar('current_tenant', default=None)

class TenantContext:
    def __init__(self, tenant_id):
        self.tenant_id = tenant_id
        self.token = None
    
    def __enter__(self):
        self.token = current_tenant.set(self.tenant_id)
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        current_tenant.reset(self.token)

def require_tenant(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        tenant_id = current_tenant.get()
        if not tenant_id:
            raise ValueError("No tenant context found")
        return func(*args, **kwargs)
    return wrapper

# Middleware for web frameworks
class TenantMiddleware:
    def __init__(self, app):
        self.app = app
    
    def __call__(self, environ, start_response):
        # Extract tenant from subdomain, header, or path
        tenant_id = self.extract_tenant_id(environ)
        
        if tenant_id:
            with TenantContext(tenant_id):
                return self.app(environ, start_response)
        else:
            # Handle missing tenant
            start_response('400 Bad Request', [('Content-Type', 'text/plain')])
            return [b'Tenant not specified']
    
    def extract_tenant_id(self, environ):
        # Method 1: Subdomain
        host = environ.get('HTTP_HOST', '')
        if '.' in host:
            subdomain = host.split('.')[0]
            if subdomain != 'www':
                return subdomain
        
        # Method 2: Header
        tenant_header = environ.get('HTTP_X_TENANT_ID')
        if tenant_header:
            return tenant_header
        
        # Method 3: Path prefix
        path = environ.get('PATH_INFO', '')
        if path.startswith('/tenant/'):
            return path.split('/')[2]
        
        return None

# Usage in business logic
@require_tenant
def get_user_orders(user_id):
    tenant_id = current_tenant.get()
    query = TenantAwareQuery(db_session, tenant_id)
    return query.get_orders(user_id)
```

### 3. Infrastructure-Level Patterns

#### Container-Based Isolation
```python
# Example: Docker-based tenant isolation
import docker
import yaml

class ContainerTenantManager:
    def __init__(self):
        self.docker_client = docker.from_env()
        self.tenant_containers = {}
    
    def provision_tenant_container(self, tenant_id, config):
        container_name = f"tenant-{tenant_id}"
        
        # Generate tenant-specific configuration
        tenant_config = self.generate_tenant_config(tenant_id, config)
        
        # Create and start container
        container = self.docker_client.containers.run(
            image="myapp:latest",
            name=container_name,
            environment=tenant_config,
            ports={'8080/tcp': None},  # Random port
            detach=True,
            restart_policy={"Name": "unless-stopped"}
        )
        
        self.tenant_containers[tenant_id] = container
        return container
    
    def generate_tenant_config(self, tenant_id, config):
        return {
            'TENANT_ID': tenant_id,
            'DATABASE_URL': f"postgresql://user:pass@db/tenant_{tenant_id}",
            'REDIS_DB': str(hash(tenant_id) % 16),
            'LOG_LEVEL': config.get('log_level', 'INFO'),
            'FEATURE_FLAGS': json.dumps(config.get('features', {}))
        }
    
    def scale_tenant(self, tenant_id, replicas):
        # Use Docker Swarm or Kubernetes for scaling
        pass
    
    def get_tenant_endpoint(self, tenant_id):
        container = self.tenant_containers.get(tenant_id)
        if container:
            container.reload()
            ports = container.attrs['NetworkSettings']['Ports']
            if '8080/tcp' in ports and ports['8080/tcp']:
                host_port = ports['8080/tcp'][0]['HostPort']
                return f"http://localhost:{host_port}"
        return None
```

#### Kubernetes-Based Multi-Tenancy
```yaml
# Example: Kubernetes namespace-per-tenant
apiVersion: v1
kind: Namespace
metadata:
  name: tenant-{{.TenantID}}
  labels:
    tenant: "{{.TenantID}}"
    managed-by: "tenant-operator"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app
  namespace: tenant-{{.TenantID}}
spec:
  replicas: {{.Replicas}}
  selector:
    matchLabels:
      app: tenant-app
  template:
    metadata:
      labels:
        app: tenant-app
    spec:
      containers:
      - name: app
        image: myapp:latest
        env:
        - name: TENANT_ID
          value: "{{.TenantID}}"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: tenant-db-secret
              key: url
        resources:
          requests:
            memory: "{{.MemoryRequest}}"
            cpu: "{{.CPURequest}}"
          limits:
            memory: "{{.MemoryLimit}}"
            cpu: "{{.CPULimit}}"
```

## Security Considerations

### 1. Data Isolation
```python
# Example: Row-level security in PostgreSQL
class RowLevelSecurityManager:
    def __init__(self, connection):
        self.connection = connection
    
    def enable_rls(self, table_name):
        """Enable row-level security on a table"""
        self.connection.execute(f"ALTER TABLE {table_name} ENABLE ROW LEVEL SECURITY")
    
    def create_tenant_policy(self, table_name):
        """Create policy to restrict access to tenant's data"""
        policy_sql = f"""
        CREATE POLICY tenant_isolation ON {table_name}
        USING (tenant_id = current_setting('app.current_tenant')::text)
        WITH CHECK (tenant_id = current_setting('app.current_tenant')::text)
        """
        self.connection.execute(policy_sql)
    
    def set_tenant_context(self, tenant_id):
        """Set the current tenant context"""
        self.connection.execute(f"SET app.current_tenant = '{tenant_id}'")

# Usage
rls_manager = RowLevelSecurityManager(db_connection)
rls_manager.enable_rls('users')
rls_manager.enable_rls('orders')
rls_manager.create_tenant_policy('users')
rls_manager.create_tenant_policy('orders')
```

### 2. Authentication and Authorization
```python
# Example: Multi-tenant JWT authentication
import jwt
from datetime import datetime, timedelta

class MultiTenantAuth:
    def __init__(self, secret_key):
        self.secret_key = secret_key
    
    def generate_tenant_token(self, user_id, tenant_id, roles):
        payload = {
            'user_id': user_id,
            'tenant_id': tenant_id,
            'roles': roles,
            'exp': datetime.utcnow() + timedelta(hours=24),
            'iat': datetime.utcnow()
        }
        return jwt.encode(payload, self.secret_key, algorithm='HS256')
    
    def verify_token(self, token):
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=['HS256'])
            return payload
        except jwt.ExpiredSignatureError:
            raise AuthenticationError("Token has expired")
        except jwt.InvalidTokenError:
            raise AuthenticationError("Invalid token")
    
    def check_tenant_access(self, token, required_tenant_id):
        payload = self.verify_token(token)
        if payload['tenant_id'] != required_tenant_id:
            raise AuthorizationError("Access denied for this tenant")
        return payload

class TenantAccessControl:
    def __init__(self):
        self.permissions = {}
    
    def define_permission(self, tenant_id, resource, actions):
        if tenant_id not in self.permissions:
            self.permissions[tenant_id] = {}
        self.permissions[tenant_id][resource] = actions
    
    def check_permission(self, tenant_id, resource, action, user_roles):
        tenant_perms = self.permissions.get(tenant_id, {})
        resource_perms = tenant_perms.get(resource, {})
        
        for role in user_roles:
            if role in resource_perms and action in resource_perms[role]:
                return True
        
        return False
```

## Performance and Scaling

### 1. Resource Allocation
```python
# Example: Tenant resource management
class TenantResourceManager:
    def __init__(self):
        self.tenant_limits = {}
        self.current_usage = {}
    
    def set_tenant_limits(self, tenant_id, limits):
        """Set resource limits for a tenant"""
        self.tenant_limits[tenant_id] = {
            'max_cpu': limits.get('cpu', 1.0),
            'max_memory': limits.get('memory', '1Gi'),
            'max_storage': limits.get('storage', '10Gi'),
            'max_connections': limits.get('connections', 100),
            'rate_limit': limits.get('rate_limit', 1000)  # requests per minute
        }
    
    def check_resource_usage(self, tenant_id, resource_type, current_usage):
        """Check if tenant is within resource limits"""
        limits = self.tenant_limits.get(tenant_id, {})
        max_limit = limits.get(f'max_{resource_type}')
        
        if max_limit and current_usage > max_limit:
            raise ResourceLimitExceededError(
                f"Tenant {tenant_id} exceeded {resource_type} limit"
            )
        
        return True
    
    def allocate_resources(self, tenant_id, resource_requirements):
        """Allocate resources for a tenant workload"""
        for resource, amount in resource_requirements.items():
            current = self.current_usage.get(tenant_id, {}).get(resource, 0)
            self.check_resource_usage(tenant_id, resource, current + amount)
        
        # Update usage tracking
        if tenant_id not in self.current_usage:
            self.current_usage[tenant_id] = {}
        
        for resource, amount in resource_requirements.items():
            self.current_usage[tenant_id][resource] = \
                self.current_usage[tenant_id].get(resource, 0) + amount
```

### 2. Caching Strategies
```python
# Example: Multi-tenant caching
import redis
import json

class MultiTenantCache:
    def __init__(self, redis_client):
        self.redis = redis_client
    
    def _get_tenant_key(self, tenant_id, key):
        """Generate tenant-specific cache key"""
        return f"tenant:{tenant_id}:cache:{key}"
    
    def get(self, tenant_id, key):
        """Get value from tenant's cache"""
        tenant_key = self._get_tenant_key(tenant_id, key)
        value = self.redis.get(tenant_key)
        return json.loads(value) if value else None
    
    def set(self, tenant_id, key, value, ttl=3600):
        """Set value in tenant's cache"""
        tenant_key = self._get_tenant_key(tenant_id, key)
        self.redis.setex(tenant_key, ttl, json.dumps(value))
    
    def delete(self, tenant_id, key):
        """Delete value from tenant's cache"""
        tenant_key = self._get_tenant_key(tenant_id, key)
        self.redis.delete(tenant_key)
    
    def clear_tenant_cache(self, tenant_id):
        """Clear all cache entries for a tenant"""
        pattern = f"tenant:{tenant_id}:cache:*"
        keys = self.redis.keys(pattern)
        if keys:
            self.redis.delete(*keys)
    
    def get_tenant_cache_stats(self, tenant_id):
        """Get cache statistics for a tenant"""
        pattern = f"tenant:{tenant_id}:cache:*"
        keys = self.redis.keys(pattern)
        
        return {
            'total_keys': len(keys),
            'memory_usage': sum(self.redis.memory_usage(key) for key in keys)
        }
```

## Tenant Lifecycle Management

### 1. Tenant Provisioning
```python
# Example: Automated tenant provisioning
class TenantProvisioningService:
    def __init__(self, db_manager, cache_manager, resource_manager):
        self.db_manager = db_manager
        self.cache_manager = cache_manager
        self.resource_manager = resource_manager
    
    def provision_tenant(self, tenant_config):
        """Provision a new tenant with all required resources"""
        tenant_id = tenant_config['tenant_id']
        
        try:
            # 1. Create database schema/instance
            if tenant_config['isolation_level'] == 'database':
                self.db_manager.provision_tenant_database(tenant_id)
            elif tenant_config['isolation_level'] == 'schema':
                self.db_manager.create_tenant_schema(tenant_id)
            
            # 2. Set up resource limits
            self.resource_manager.set_tenant_limits(
                tenant_id, 
                tenant_config['resource_limits']
            )
            
            # 3. Initialize tenant configuration
            self._initialize_tenant_config(tenant_id, tenant_config)
            
            # 4. Create default admin user
            self._create_admin_user(tenant_id, tenant_config['admin_user'])
            
            # 5. Set up monitoring
            self._setup_tenant_monitoring(tenant_id)
            
            return {
                'status': 'success',
                'tenant_id': tenant_id,
                'endpoints': self._get_tenant_endpoints(tenant_id)
            }
            
        except Exception as e:
            # Rollback on failure
            self.deprovision_tenant(tenant_id)
            raise TenantProvisioningError(f"Failed to provision tenant: {str(e)}")
    
    def deprovision_tenant(self, tenant_id):
        """Remove all tenant resources"""
        # 1. Remove data
        self.db_manager.remove_tenant_data(tenant_id)
        
        # 2. Clear cache
        self.cache_manager.clear_tenant_cache(tenant_id)
        
        # 3. Release resources
        self.resource_manager.release_tenant_resources(tenant_id)
        
        # 4. Clean up configuration
        self._cleanup_tenant_config(tenant_id)
```

### 2. Tenant Migration
```python
# Example: Tenant data migration
class TenantMigrationService:
    def __init__(self, source_db, target_db):
        self.source_db = source_db
        self.target_db = target_db
    
    def migrate_tenant(self, tenant_id, migration_config):
        """Migrate tenant from one system to another"""
        migration_id = str(uuid.uuid4())
        
        try:
            # 1. Create migration record
            self._create_migration_record(migration_id, tenant_id, migration_config)
            
            # 2. Export tenant data
            export_path = self._export_tenant_data(tenant_id)
            
            # 3. Provision target tenant
            self._provision_target_tenant(tenant_id, migration_config)
            
            # 4. Import data to target
            self._import_tenant_data(tenant_id, export_path)
            
            # 5. Validate migration
            validation_result = self._validate_migration(tenant_id)
            
            if validation_result['success']:
                # 6. Switch traffic to new system
                self._switch_tenant_traffic(tenant_id)
                
                # 7. Clean up old system (after confirmation)
                self._schedule_cleanup(tenant_id)
            
            return validation_result
            
        except Exception as e:
            self._rollback_migration(migration_id, tenant_id)
            raise
    
    def _validate_migration(self, tenant_id):
        """Validate that migration completed successfully"""
        source_counts = self._get_table_counts(self.source_db, tenant_id)
        target_counts = self._get_table_counts(self.target_db, tenant_id)
        
        discrepancies = []
        for table, source_count in source_counts.items():
            target_count = target_counts.get(table, 0)
            if source_count != target_count:
                discrepancies.append({
                    'table': table,
                    'source_count': source_count,
                    'target_count': target_count
                })
        
        return {
            'success': len(discrepancies) == 0,
            'discrepancies': discrepancies
        }
```

## Real-World Use Cases

### 1. SaaS Applications
```python
# Example: Multi-tenant SaaS configuration
class SaaSMultiTenancy:
    def __init__(self):
        self.tenant_configs = {}
        self.feature_flags = {}
    
    def configure_tenant_features(self, tenant_id, plan_type):
        """Configure features based on subscription plan"""
        feature_matrix = {
            'basic': {
                'max_users': 10,
                'storage_gb': 1,
                'api_calls_per_day': 1000,
                'features': ['basic_reports', 'email_support']
            },
            'professional': {
                'max_users': 100,
                'storage_gb': 10,
                'api_calls_per_day': 10000,
                'features': ['basic_reports', 'advanced_reports', 'priority_support', 'api_access']
            },
            'enterprise': {
                'max_users': 'unlimited',
                'storage_gb': 100,
                'api_calls_per_day': 100000,
                'features': ['all_features', 'dedicated_support', 'custom_integration']
            }
        }
        
        self.tenant_configs[tenant_id] = feature_matrix.get(plan_type, feature_matrix['basic'])
    
    def can_access_feature(self, tenant_id, feature_name):
        """Check if tenant can access a specific feature"""
        config = self.tenant_configs.get(tenant_id, {})
        return feature_name in config.get('features', [])
```

### 2. Multi-Tenant Analytics
```python
# Example: Tenant-aware analytics
class MultiTenantAnalytics:
    def __init__(self, analytics_db):
        self.analytics_db = analytics_db
    
    def track_event(self, tenant_id, user_id, event_name, properties):
        """Track analytics event for a specific tenant"""
        event = {
            'tenant_id': tenant_id,
            'user_id': user_id,
            'event_name': event_name,
            'properties': properties,
            'timestamp': datetime.utcnow()
        }
        
        self.analytics_db.insert_event(event)
    
    def get_tenant_metrics(self, tenant_id, start_date, end_date):
        """Get analytics metrics for a specific tenant"""
        return self.analytics_db.query("""
            SELECT 
                event_name,
                COUNT(*) as event_count,
                COUNT(DISTINCT user_id) as unique_users
            FROM events 
            WHERE tenant_id = %s 
                AND timestamp BETWEEN %s AND %s
            GROUP BY event_name
        """, [tenant_id, start_date, end_date])
```

## Best Practices

### 1. Design Principles
- Start with the highest level of isolation possible
- Plan for tenant growth and scaling
- Implement proper monitoring and alerting
- Design for tenant-specific customization

### 2. Security Best Practices
- Never trust tenant-provided input
- Implement defense in depth
- Regular security audits
- Principle of least privilege

### 3. Performance Best Practices
- Use connection pooling effectively
- Implement proper caching strategies
- Monitor and optimize query performance
- Plan for data archiving and cleanup

## Interview Questions

### Architectural Questions
1. **Compare the trade-offs between different multi-tenancy patterns**
2. **How would you design a multi-tenant system that needs to support both small and enterprise customers?**
3. **What are the security considerations in a multi-tenant architecture?**

### Implementation Questions
1. **How would you implement tenant-aware caching?**
2. **Design a tenant migration strategy for zero-downtime moves**
3. **How would you handle tenant-specific customizations?**

### Scaling Questions
1. **How would you scale a multi-tenant system to handle 10,000+ tenants?**
2. **What metrics would you monitor in a multi-tenant system?**
3. **How would you handle noisy neighbor problems?**

## Conclusion

Multi-tenant architecture enables efficient resource utilization while providing isolation and customization. The key is choosing the right tenancy pattern based on your requirements for isolation, scalability, and customization. Success requires careful attention to security, performance, and operational complexity while maintaining the ability to serve diverse tenant needs effectively.
