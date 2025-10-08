# Amazon's Distributed Cache System - Case Study

## Overview

Amazon's e-commerce platform serves millions of requests per second. This case study demonstrates a production-grade distributed caching system inspired by Amazon's ElastiCache and DynamoDB, showcasing how Amazon achieves sub-millisecond latency at massive scale.

## Business Problem

### Challenges Addressed:
1. **Database Bottlenecks**: Relational databases can't handle millions of QPS
2. **Read Scalability**: 90% of requests are reads, must scale independently
3. **Global Distribution**: Users worldwide need low-latency access
4. **Cost Optimization**: Caching reduces expensive database queries
5. **High Availability**: System must be resilient to failures

## Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│              Application Layer                        │
│   (Product Service, Cart Service, Order Service)     │
└────────────────────┬─────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────┐
│           Distributed Cache Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │  Node 1  │  │  Node 2  │  │  Node 3  │          │
│  │ (Redis)  │  │ (Redis)  │  │ (Redis)  │          │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘          │
│       │             │             │                  │
│       └─────────────┴─────────────┘                  │
│         Consistent Hashing Ring                      │
└────────────────────┬─────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────┐
│           Write-Through Cache                         │
│              to Primary DB                            │
└──────────────────────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────┐
│         Primary Database (DynamoDB/RDS)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ Shard 1  │  │ Shard 2  │  │ Shard 3  │          │
│  └──────────┘  └──────────┘  └──────────┘          │
└──────────────────────────────────────────────────────┘
```

## Key Features

### 1. Consistent Hashing
- **Even Distribution**: Keys distributed evenly across cache nodes
- **Minimal Reshuffling**: Adding/removing nodes only affects adjacent keys
- **Virtual Nodes**: Better distribution with multiple hash points per node

### 2. Cache Strategies
- **Cache-Aside (Lazy Loading)**: Read from cache, populate on miss
- **Write-Through**: Write to cache and DB simultaneously
- **Write-Behind**: Async write to DB, immediate cache update
- **TTL Management**: Automatic expiration of stale data

### 3. High Availability
- **Replication**: Each key replicated to N nodes (default: 3)
- **Health Checks**: Automatic detection and routing around failed nodes
- **Circuit Breaker**: Prevents cascade failures
- **Fallback to DB**: Graceful degradation when cache unavailable

### 4. Performance Optimization
- **Connection Pooling**: Reuse connections to cache nodes
- **Batch Operations**: MGET/MSET for multiple keys
- **Pipeline Commands**: Reduce network round trips
- **Compression**: Reduce memory footprint for large objects

## Design Patterns Used

1. **Cache-Aside Pattern**: Application manages cache explicitly
2. **Circuit Breaker Pattern**: Fault tolerance
3. **Consistent Hashing**: Data partitioning
4. **Observer Pattern**: Cache invalidation events
5. **Strategy Pattern**: Different caching strategies per use case
6. **Proxy Pattern**: Cache as proxy to database

## Technology Stack

- **Cache Engine**: Redis (in-memory key-value store)
- **Partitioning**: Consistent hashing with virtual nodes
- **Replication**: Master-slave replication
- **Monitoring**: Metrics for hit rate, latency, memory usage

## Performance Metrics

- **Throughput**: 100,000+ operations per second per node
- **Latency**: P99 < 1ms for cache hits
- **Cache Hit Rate**: 95%+ for product catalog
- **Memory Efficiency**: 80-90% memory utilization
- **Availability**: 99.99% uptime

## Use Cases

1. **Product Catalog**: Cache product details, images, reviews
2. **Session Management**: Store user sessions for fast access
3. **Shopping Cart**: Real-time cart updates with high concurrency
4. **Rate Limiting**: Track API usage per user/IP
5. **Recommendation Engine**: Cache personalized recommendations

## Running the Example

```bash
# Compile the code
javac DistributedCacheSystemDemo.java

# Run the demo
java DistributedCacheSystemDemo
```

## Key Learnings

1. **Consistent Hashing**: Essential for scalable distributed caching
2. **Replication**: Trade-off between consistency and availability
3. **Cache Invalidation**: Hardest problem in computer science
4. **Monitoring**: Track hit rates and latency religiously
5. **Fallback Strategy**: Always have a plan when cache fails

## Performance Comparison

| Operation | Database | Cache | Improvement |
|-----------|----------|-------|-------------|
| Product Read | 45ms | 0.8ms | 56x faster |
| User Profile | 38ms | 0.5ms | 76x faster |
| Cart Update | 52ms | 1.2ms | 43x faster |

## References

- [Amazon ElastiCache Documentation](https://aws.amazon.com/elasticache/)
- [DynamoDB Accelerator (DAX)](https://aws.amazon.com/dynamodb/dax/)
- [Consistent Hashing Algorithm](https://en.wikipedia.org/wiki/Consistent_hashing)
- [Cache Strategies Best Practices](https://aws.amazon.com/caching/best-practices/)
