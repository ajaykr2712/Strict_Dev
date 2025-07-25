# Database Performance Optimization

## Overview

Database performance optimization is crucial for system scalability and user experience. This guide covers comprehensive strategies for optimizing database performance across different database systems and use cases.

## Query Optimization

### Index Strategies
```sql
-- Composite index for multi-column queries
CREATE INDEX idx_user_orders 
ON orders (user_id, order_date, status);

-- Partial index for filtered queries
CREATE INDEX idx_active_users 
ON users (email) 
WHERE status = 'active';

-- Covering index to avoid table lookups
CREATE INDEX idx_order_summary 
ON orders (user_id) 
INCLUDE (order_date, total_amount, status);
```

### Query Analysis Tools
```python
import psycopg2
import time
from contextlib import contextmanager

class DatabaseProfiler:
    def __init__(self, connection_string):
        self.connection_string = connection_string
        
    @contextmanager
    def profile_query(self, query_name):
        conn = psycopg2.connect(self.connection_string)
        cursor = conn.cursor()
        
        start_time = time.time()
        
        try:
            # Enable query planning
            cursor.execute("EXPLAIN (ANALYZE, BUFFERS) " + query_name)
            plan = cursor.fetchall()
            
            yield cursor
            
        finally:
            end_time = time.time()
            execution_time = end_time - start_time
            
            print(f"Query: {query_name}")
            print(f"Execution time: {execution_time:.4f} seconds")
            print("Query plan:")
            for row in plan:
                print(row[0])
                
            cursor.close()
            conn.close()

# Usage example
profiler = DatabaseProfiler("postgresql://user:pass@localhost/db")

with profiler.profile_query("SELECT * FROM orders WHERE user_id = %s") as cursor:
    cursor.execute("SELECT * FROM orders WHERE user_id = %s", (123,))
    results = cursor.fetchall()
```

### Query Optimization Techniques
```python
class QueryOptimizer:
    def __init__(self, db_connection):
        self.db = db_connection
        
    def optimize_pagination(self, table, offset, limit, order_column):
        """
        Optimize pagination using cursor-based pagination instead of OFFSET
        """
        # Inefficient: OFFSET becomes slow with large offsets
        # SELECT * FROM table ORDER BY id LIMIT 20 OFFSET 10000;
        
        # Efficient: Cursor-based pagination
        if offset == 0:
            query = f"""
                SELECT * FROM {table} 
                ORDER BY {order_column} 
                LIMIT {limit}
            """
        else:
            query = f"""
                SELECT * FROM {table} 
                WHERE {order_column} > %s 
                ORDER BY {order_column} 
                LIMIT {limit}
            """
            
        return query
        
    def optimize_joins(self, base_table, join_conditions):
        """
        Optimize JOIN operations by reordering and using appropriate join types
        """
        # Use EXISTS instead of IN for better performance
        optimized_query = f"""
            SELECT b.* FROM {base_table} b
            WHERE EXISTS (
                SELECT 1 FROM related_table r 
                WHERE r.base_id = b.id 
                AND r.status = 'active'
            )
        """
        
        return optimized_query
        
    def batch_operations(self, table, records, batch_size=1000):
        """
        Optimize bulk operations using batching
        """
        for i in range(0, len(records), batch_size):
            batch = records[i:i + batch_size]
            
            # Use bulk insert instead of individual inserts
            values = ','.join(['%s'] * len(batch))
            query = f"INSERT INTO {table} VALUES {values}"
            
            self.db.execute(query, batch)
            self.db.commit()
```

## Connection Pool Optimization

### Advanced Connection Pool
```python
import threading
import time
from queue import Queue, Empty
import psycopg2

class OptimizedConnectionPool:
    def __init__(self, connection_params, 
                 min_connections=5, 
                 max_connections=20,
                 connection_timeout=30,
                 idle_timeout=300):
        self.connection_params = connection_params
        self.min_connections = min_connections
        self.max_connections = max_connections
        self.connection_timeout = connection_timeout
        self.idle_timeout = idle_timeout
        
        self.pool = Queue(maxsize=max_connections)
        self.active_connections = 0
        self.lock = threading.Lock()
        
        # Initialize minimum connections
        self._initialize_pool()
        
        # Start cleanup thread
        self.cleanup_thread = threading.Thread(target=self._cleanup_idle_connections, daemon=True)
        self.cleanup_thread.start()
        
    def _initialize_pool(self):
        for _ in range(self.min_connections):
            conn = self._create_connection()
            if conn:
                self.pool.put({
                    'connection': conn,
                    'created_at': time.time(),
                    'last_used': time.time()
                })
                self.active_connections += 1
                
    def _create_connection(self):
        try:
            conn = psycopg2.connect(**self.connection_params)
            conn.autocommit = True
            return conn
        except psycopg2.Error as e:
            print(f"Failed to create connection: {e}")
            return None
            
    def get_connection(self):
        try:
            # Try to get existing connection
            conn_info = self.pool.get(timeout=self.connection_timeout)
            conn_info['last_used'] = time.time()
            return conn_info['connection']
            
        except Empty:
            # Create new connection if under max limit
            with self.lock:
                if self.active_connections < self.max_connections:
                    conn = self._create_connection()
                    if conn:
                        self.active_connections += 1
                        return conn
                        
            raise Exception("Connection pool exhausted")
            
    def return_connection(self, connection):
        if connection and not connection.closed:
            # Reset connection state
            try:
                connection.rollback()
                self.pool.put({
                    'connection': connection,
                    'created_at': time.time(),
                    'last_used': time.time()
                })
            except psycopg2.Error:
                # Connection is broken, don't return to pool
                self.active_connections -= 1
                
    def _cleanup_idle_connections(self):
        while True:
            time.sleep(60)  # Check every minute
            
            current_time = time.time()
            cleanup_connections = []
            
            # Collect idle connections
            while not self.pool.empty():
                try:
                    conn_info = self.pool.get_nowait()
                    
                    if (current_time - conn_info['last_used'] > self.idle_timeout and 
                        self.active_connections > self.min_connections):
                        cleanup_connections.append(conn_info['connection'])
                        self.active_connections -= 1
                    else:
                        self.pool.put(conn_info)
                        
                except Empty:
                    break
                    
            # Close idle connections
            for conn in cleanup_connections:
                try:
                    conn.close()
                except:
                    pass
```

## Caching Strategies

### Multi-Level Caching
```python
import redis
import memcache
from functools import wraps
import pickle
import hashlib

class MultiLevelCache:
    def __init__(self):
        # L1: In-memory cache (fastest)
        self.l1_cache = {}
        self.l1_max_size = 1000
        
        # L2: Redis cache (fast, distributed)
        self.redis_client = redis.Redis(host='localhost', port=6379, db=0)
        
        # L3: Memcached (larger capacity)
        self.memcache_client = memcache.Client(['127.0.0.1:11211'])
        
    def get(self, key):
        # Try L1 cache first
        if key in self.l1_cache:
            return self.l1_cache[key]['value']
            
        # Try L2 cache (Redis)
        redis_value = self.redis_client.get(key)
        if redis_value:
            value = pickle.loads(redis_value)
            self._set_l1_cache(key, value)
            return value
            
        # Try L3 cache (Memcached)
        memcache_value = self.memcache_client.get(key)
        if memcache_value:
            self._set_l2_cache(key, memcache_value, ttl=3600)
            self._set_l1_cache(key, memcache_value)
            return memcache_value
            
        return None
        
    def set(self, key, value, ttl=3600):
        # Set in all cache levels
        self._set_l1_cache(key, value)
        self._set_l2_cache(key, value, ttl)
        self._set_l3_cache(key, value, ttl)
        
    def _set_l1_cache(self, key, value):
        if len(self.l1_cache) >= self.l1_max_size:
            # LRU eviction
            oldest_key = min(self.l1_cache.keys(), 
                           key=lambda k: self.l1_cache[k]['access_time'])
            del self.l1_cache[oldest_key]
            
        self.l1_cache[key] = {
            'value': value,
            'access_time': time.time()
        }
        
    def _set_l2_cache(self, key, value, ttl):
        serialized_value = pickle.dumps(value)
        self.redis_client.setex(key, ttl, serialized_value)
        
    def _set_l3_cache(self, key, value, ttl):
        self.memcache_client.set(key, value, time=ttl)

def cached(ttl=3600, cache_key_generator=None):
    """
    Decorator for caching function results
    """
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            # Generate cache key
            if cache_key_generator:
                cache_key = cache_key_generator(*args, **kwargs)
            else:
                key_data = f"{func.__name__}:{str(args)}:{str(sorted(kwargs.items()))}"
                cache_key = hashlib.md5(key_data.encode()).hexdigest()
                
            # Try to get from cache
            cache = MultiLevelCache()
            cached_result = cache.get(cache_key)
            
            if cached_result is not None:
                return cached_result
                
            # Execute function and cache result
            result = func(*args, **kwargs)
            cache.set(cache_key, result, ttl)
            
            return result
            
        return wrapper
    return decorator

# Usage example
@cached(ttl=1800)
def get_user_profile(user_id):
    # Expensive database operation
    return fetch_user_from_database(user_id)
```

## Read Replicas and Sharding

### Read Replica Router
```python
import random
from enum import Enum

class QueryType(Enum):
    READ = "read"
    WRITE = "write"

class DatabaseRouter:
    def __init__(self, master_config, replica_configs):
        self.master_config = master_config
        self.replica_configs = replica_configs
        self.replica_weights = [config.get('weight', 1) for config in replica_configs]
        
    def get_connection(self, query_type=QueryType.READ):
        if query_type == QueryType.WRITE:
            return self._get_master_connection()
        else:
            return self._get_replica_connection()
            
    def _get_master_connection(self):
        return psycopg2.connect(**self.master_config)
        
    def _get_replica_connection(self):
        # Weighted random selection of replica
        replica_config = random.choices(
            self.replica_configs, 
            weights=self.replica_weights
        )[0]
        
        try:
            return psycopg2.connect(**replica_config)
        except psycopg2.Error:
            # Fallback to master if replica is unavailable
            return self._get_master_connection()

class ShardedDatabase:
    def __init__(self, shard_configs, shard_key_function):
        self.shard_configs = shard_configs
        self.shard_key_function = shard_key_function
        
    def get_shard_connection(self, shard_key):
        shard_index = self.shard_key_function(shard_key, len(self.shard_configs))
        shard_config = self.shard_configs[shard_index]
        return psycopg2.connect(**shard_config)
        
    def execute_on_shard(self, shard_key, query, params=None):
        conn = self.get_shard_connection(shard_key)
        try:
            cursor = conn.cursor()
            cursor.execute(query, params)
            return cursor.fetchall()
        finally:
            conn.close()
            
    def execute_on_all_shards(self, query, params=None):
        results = []
        for shard_config in self.shard_configs:
            conn = psycopg2.connect(**shard_config)
            try:
                cursor = conn.cursor()
                cursor.execute(query, params)
                results.extend(cursor.fetchall())
            finally:
                conn.close()
        return results

# Hash-based sharding function
def hash_shard_key(key, num_shards):
    return hash(str(key)) % num_shards

# Range-based sharding function
def range_shard_key(key, num_shards):
    # Assuming key is numeric
    ranges = [(0, 1000), (1001, 2000), (2001, 3000)]  # Example ranges
    for i, (start, end) in enumerate(ranges):
        if start <= key <= end:
            return i
    return 0  # Default shard
```

## Performance Monitoring

### Database Performance Monitor
```python
import psutil
import threading
import time
from dataclasses import dataclass
from typing import List, Dict

@dataclass
class PerformanceMetrics:
    timestamp: float
    cpu_usage: float
    memory_usage: float
    active_connections: int
    slow_queries: int
    cache_hit_ratio: float
    avg_query_time: float

class DatabasePerformanceMonitor:
    def __init__(self, db_connection, alert_thresholds=None):
        self.db_connection = db_connection
        self.alert_thresholds = alert_thresholds or {
            'cpu_usage': 80.0,
            'memory_usage': 85.0,
            'active_connections': 100,
            'slow_queries': 10,
            'cache_hit_ratio': 0.95,
            'avg_query_time': 1.0
        }
        self.metrics_history = []
        self.monitoring = False
        
    def start_monitoring(self, interval=60):
        self.monitoring = True
        monitor_thread = threading.Thread(target=self._monitor_loop, args=(interval,))
        monitor_thread.daemon = True
        monitor_thread.start()
        
    def stop_monitoring(self):
        self.monitoring = False
        
    def _monitor_loop(self, interval):
        while self.monitoring:
            metrics = self._collect_metrics()
            self.metrics_history.append(metrics)
            
            # Keep only last 24 hours of data
            cutoff_time = time.time() - (24 * 60 * 60)
            self.metrics_history = [
                m for m in self.metrics_history 
                if m.timestamp > cutoff_time
            ]
            
            # Check for alerts
            self._check_alerts(metrics)
            
            time.sleep(interval)
            
    def _collect_metrics(self):
        cursor = self.db_connection.cursor()
        
        # Get database-specific metrics
        cursor.execute("""
            SELECT 
                (SELECT count(*) FROM pg_stat_activity WHERE state = 'active') as active_connections,
                (SELECT count(*) FROM pg_stat_statements WHERE mean_time > 1000) as slow_queries,
                (SELECT blks_hit::float / (blks_read + blks_hit) FROM pg_stat_database WHERE datname = current_database()) as cache_hit_ratio,
                (SELECT avg(mean_time) FROM pg_stat_statements) as avg_query_time
        """)
        
        db_metrics = cursor.fetchone()
        
        return PerformanceMetrics(
            timestamp=time.time(),
            cpu_usage=psutil.cpu_percent(),
            memory_usage=psutil.virtual_memory().percent,
            active_connections=db_metrics[0] or 0,
            slow_queries=db_metrics[1] or 0,
            cache_hit_ratio=db_metrics[2] or 0.0,
            avg_query_time=db_metrics[3] or 0.0
        )
        
    def _check_alerts(self, metrics):
        alerts = []
        
        if metrics.cpu_usage > self.alert_thresholds['cpu_usage']:
            alerts.append(f"High CPU usage: {metrics.cpu_usage}%")
            
        if metrics.memory_usage > self.alert_thresholds['memory_usage']:
            alerts.append(f"High memory usage: {metrics.memory_usage}%")
            
        if metrics.active_connections > self.alert_thresholds['active_connections']:
            alerts.append(f"Too many active connections: {metrics.active_connections}")
            
        if metrics.cache_hit_ratio < self.alert_thresholds['cache_hit_ratio']:
            alerts.append(f"Low cache hit ratio: {metrics.cache_hit_ratio:.2%}")
            
        for alert in alerts:
            self._send_alert(alert, metrics)
            
    def _send_alert(self, message, metrics):
        # Send alert through various channels
        print(f"ALERT: {message}")
        # Could also send email, Slack notification, etc.
        
    def get_performance_report(self, hours=24):
        cutoff_time = time.time() - (hours * 60 * 60)
        recent_metrics = [
            m for m in self.metrics_history 
            if m.timestamp > cutoff_time
        ]
        
        if not recent_metrics:
            return "No metrics available"
            
        avg_cpu = sum(m.cpu_usage for m in recent_metrics) / len(recent_metrics)
        avg_memory = sum(m.memory_usage for m in recent_metrics) / len(recent_metrics)
        max_connections = max(m.active_connections for m in recent_metrics)
        avg_cache_hit = sum(m.cache_hit_ratio for m in recent_metrics) / len(recent_metrics)
        
        return f"""
        Performance Report (Last {hours} hours):
        - Average CPU Usage: {avg_cpu:.2f}%
        - Average Memory Usage: {avg_memory:.2f}%
        - Peak Connections: {max_connections}
        - Average Cache Hit Ratio: {avg_cache_hit:.2%}
        - Total Metrics Collected: {len(recent_metrics)}
        """
```

## Optimization Best Practices

### 1. Query Optimization
- Use proper indexing strategies
- Avoid N+1 queries
- Implement query result caching
- Use EXPLAIN to analyze query plans
- Optimize JOIN operations

### 2. Schema Design
- Normalize appropriately (3NF for OLTP)
- Consider denormalization for read-heavy workloads
- Use appropriate data types
- Implement proper constraints
- Design for query patterns

### 3. Connection Management
- Use connection pooling
- Implement proper timeout settings
- Monitor connection usage
- Handle connection failures gracefully
- Scale connection pools with load

### 4. Caching Strategy
- Implement multi-level caching
- Use appropriate cache invalidation
- Monitor cache hit ratios
- Consider cache warming strategies
- Handle cache failures gracefully

### 5. Monitoring and Alerting
- Track key performance metrics
- Set up proactive alerting
- Monitor query performance
- Track resource utilization
- Implement capacity planning

## Common Performance Anti-Patterns

1. **Missing Indexes**: Queries without proper index support
2. **Over-Indexing**: Too many indexes causing write performance issues
3. **SELECT \***: Fetching unnecessary columns
4. **Chatty Interfaces**: Too many database round trips
5. **Unbounded Queries**: Queries without LIMIT clauses
6. **Connection Leaks**: Not properly closing database connections
7. **Synchronous Processing**: Blocking operations in critical paths

## Conclusion

Database performance optimization requires a comprehensive approach covering query optimization, caching, connection management, and monitoring. Success depends on understanding your specific workload patterns, implementing appropriate optimizations, and continuously monitoring performance metrics to identify and address bottlenecks proactively.
