"""
Cache Implementation Strategies - System Design Concept Demonstration
Purpose: Implement and compare different caching strategies and policies
Author: System Design Daily
Date: 2025-07-24

This script demonstrates:
1. Different cache eviction policies (LRU, LFU, FIFO)
2. Cache warming and invalidation strategies
3. Distributed caching concepts
4. Performance analysis of different approaches
"""

import time
import threading
import random
from typing import Any, Optional, Dict, List
from abc import ABC, abstractmethod
from collections import OrderedDict, defaultdict
from dataclasses import dataclass
import statistics


@dataclass
class CacheStats:
    """Cache performance statistics"""
    hits: int = 0
    misses: int = 0
    evictions: int = 0
    total_requests: int = 0
    
    @property
    def hit_rate(self) -> float:
        if self.total_requests == 0:
            return 0.0
        return self.hits / self.total_requests
    
    @property
    def miss_rate(self) -> float:
        return 1.0 - self.hit_rate


class CachePolicy(ABC):
    """Abstract base class for cache eviction policies"""
    
    @abstractmethod
    def get(self, key: str) -> Optional[Any]:
        """Get value from cache"""
        pass
    
    @abstractmethod
    def put(self, key: str, value: Any) -> Optional[str]:
        """Put value in cache, return evicted key if any"""
        pass
    
    @abstractmethod
    def remove(self, key: str) -> bool:
        """Remove key from cache"""
        pass
    
    @abstractmethod
    def clear(self):
        """Clear all cache entries"""
        pass


class LRUCache(CachePolicy):
    """Least Recently Used cache implementation"""
    
    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache = OrderedDict()
        self.stats = CacheStats()
        self.lock = threading.RLock()
    
    def get(self, key: str) -> Optional[Any]:
        with self.lock:
            self.stats.total_requests += 1
            
            if key in self.cache:
                # Move to end (most recently used)
                value = self.cache.pop(key)
                self.cache[key] = value
                self.stats.hits += 1
                return value
            
            self.stats.misses += 1
            return None
    
    def put(self, key: str, value: Any) -> Optional[str]:
        with self.lock:
            evicted_key = None
            
            if key in self.cache:
                # Update existing key
                self.cache.pop(key)
            elif len(self.cache) >= self.capacity:
                # Evict least recently used
                evicted_key, _ = self.cache.popitem(last=False)
                self.stats.evictions += 1
            
            self.cache[key] = value
            return evicted_key
    
    def remove(self, key: str) -> bool:
        with self.lock:
            if key in self.cache:
                del self.cache[key]
                return True
            return False
    
    def clear(self):
        with self.lock:
            self.cache.clear()
    
    def size(self) -> int:
        return len(self.cache)


class LFUCache(CachePolicy):
    """Least Frequently Used cache implementation"""
    
    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache = {}
        self.frequencies = defaultdict(int)
        self.frequency_buckets = defaultdict(OrderedDict)
        self.min_frequency = 0
        self.stats = CacheStats()
        self.lock = threading.RLock()
    
    def get(self, key: str) -> Optional[Any]:
        with self.lock:
            self.stats.total_requests += 1
            
            if key not in self.cache:
                self.stats.misses += 1
                return None
            
            # Update frequency
            self._update_frequency(key)
            self.stats.hits += 1
            return self.cache[key]
    
    def put(self, key: str, value: Any) -> Optional[str]:
        with self.lock:
            evicted_key = None
            
            if self.capacity <= 0:
                return evicted_key
            
            if key in self.cache:
                # Update existing key
                self.cache[key] = value
                self._update_frequency(key)
            else:
                # Add new key
                if len(self.cache) >= self.capacity:
                    evicted_key = self._evict_lfu()
                
                self.cache[key] = value
                self.frequencies[key] = 1
                self.frequency_buckets[1][key] = True
                self.min_frequency = 1
            
            return evicted_key
    
    def _update_frequency(self, key: str):
        """Update frequency of accessed key"""
        freq = self.frequencies[key]
        
        # Remove from current frequency bucket
        del self.frequency_buckets[freq][key]
        
        # Update frequency
        self.frequencies[key] += 1
        new_freq = self.frequencies[key]
        
        # Add to new frequency bucket
        self.frequency_buckets[new_freq][key] = True
        
        # Update min_frequency if needed
        if freq == self.min_frequency and not self.frequency_buckets[freq]:
            self.min_frequency += 1
    
    def _evict_lfu(self) -> Optional[str]:
        """Evict least frequently used item"""
        if not self.frequency_buckets[self.min_frequency]:
            return None
        
        # Get LFU key (first in order for this frequency)
        lfu_key, _ = self.frequency_buckets[self.min_frequency].popitem(last=False)
        
        # Remove from cache and frequencies
        del self.cache[lfu_key]
        del self.frequencies[lfu_key]
        
        self.stats.evictions += 1
        return lfu_key
    
    def remove(self, key: str) -> bool:
        with self.lock:
            if key in self.cache:
                freq = self.frequencies[key]
                del self.cache[key]
                del self.frequencies[key]
                del self.frequency_buckets[freq][key]
                return True
            return False
    
    def clear(self):
        with self.lock:
            self.cache.clear()
            self.frequencies.clear()
            self.frequency_buckets.clear()
            self.min_frequency = 0


class FIFOCache(CachePolicy):
    """First In First Out cache implementation"""
    
    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache = {}
        self.order = []
        self.stats = CacheStats()
        self.lock = threading.RLock()
    
    def get(self, key: str) -> Optional[Any]:
        with self.lock:
            self.stats.total_requests += 1
            
            if key in self.cache:
                self.stats.hits += 1
                return self.cache[key]
            
            self.stats.misses += 1
            return None
    
    def put(self, key: str, value: Any) -> Optional[str]:
        with self.lock:
            evicted_key = None
            
            if key in self.cache:
                # Update existing key
                self.cache[key] = value
            else:
                # Add new key
                if len(self.cache) >= self.capacity:
                    # Evict first in
                    evicted_key = self.order.pop(0)
                    del self.cache[evicted_key]
                    self.stats.evictions += 1
                
                self.cache[key] = value
                self.order.append(key)
            
            return evicted_key
    
    def remove(self, key: str) -> bool:
        with self.lock:
            if key in self.cache:
                del self.cache[key]
                self.order.remove(key)
                return True
            return False
    
    def clear(self):
        with self.lock:
            self.cache.clear()
            self.order.clear()


class DistributedCache:
    """Simple distributed cache using consistent hashing"""
    
    def __init__(self, nodes: List[str], cache_factory, capacity_per_node: int = 100):
        self.nodes = nodes
        self.caches = {node: cache_factory(capacity_per_node) for node in nodes}
        self.hash_ring = self._build_hash_ring()
        self.stats = CacheStats()
        self.lock = threading.RLock()
    
    def _build_hash_ring(self) -> Dict[int, str]:
        """Build consistent hash ring"""
        ring = {}
        virtual_nodes = 100  # Virtual nodes per physical node
        
        for node in self.nodes:
            for i in range(virtual_nodes):
                virtual_node = f"{node}:{i}"
                hash_value = hash(virtual_node) % (2**32)
                ring[hash_value] = node
        
        return ring
    
    def _get_node(self, key: str) -> str:
        """Get responsible node for key"""
        key_hash = hash(key) % (2**32)
        
        # Find first node clockwise from key position
        for ring_position in sorted(self.hash_ring.keys()):
            if key_hash <= ring_position:
                return self.hash_ring[ring_position]
        
        # Wrap around to first node
        return self.hash_ring[min(self.hash_ring.keys())]
    
    def get(self, key: str) -> Optional[Any]:
        with self.lock:
            node = self._get_node(key)
            result = self.caches[node].get(key)
            
            # Aggregate stats
            self.stats.total_requests += 1
            if result is not None:
                self.stats.hits += 1
            else:
                self.stats.misses += 1
            
            return result
    
    def put(self, key: str, value: Any):
        with self.lock:
            node = self._get_node(key)
            evicted = self.caches[node].put(key, value)
            
            if evicted:
                self.stats.evictions += 1
    
    def remove(self, key: str) -> bool:
        with self.lock:
            node = self._get_node(key)
            return self.caches[node].remove(key)
    
    def add_node(self, new_node: str):
        """Add new node to distributed cache"""
        with self.lock:
            self.nodes.append(new_node)
            self.caches[new_node] = type(list(self.caches.values())[0])(100)
            self.hash_ring = self._build_hash_ring()
            # In real implementation, would need to migrate data
    
    def remove_node(self, node: str):
        """Remove node from distributed cache"""
        with self.lock:
            if node in self.nodes:
                self.nodes.remove(node)
                del self.caches[node]
                self.hash_ring = self._build_hash_ring()
                # In real implementation, would need to migrate data


class CacheWarmer:
    """Cache warming utility"""
    
    def __init__(self, cache: CachePolicy, data_source: callable):
        self.cache = cache
        self.data_source = data_source
    
    def warm_cache(self, keys: List[str]):
        """Warm cache with specified keys"""
        print(f"🔥 Warming cache with {len(keys)} keys...")
        
        start_time = time.time()
        warmed_count = 0
        
        for key in keys:
            try:
                value = self.data_source(key)
                if value is not None:
                    self.cache.put(key, value)
                    warmed_count += 1
            except Exception as e:
                print(f"Failed to warm key {key}: {e}")
        
        duration = time.time() - start_time
        print(f"✅ Warmed {warmed_count}/{len(keys)} keys in {duration:.2f} seconds")
    
    def warm_popular_keys(self, access_log: List[str], top_n: int = 100):
        """Warm cache with most popular keys from access log"""
        # Count key access frequency
        key_counts = defaultdict(int)
        for key in access_log:
            key_counts[key] += 1
        
        # Get top N most popular keys
        popular_keys = sorted(key_counts.items(), key=lambda x: x[1], reverse=True)[:top_n]
        keys_to_warm = [key for key, count in popular_keys]
        
        self.warm_cache(keys_to_warm)


def simulate_data_source(key: str) -> Optional[str]:
    """Simulate expensive data source operation"""
    # Simulate database lookup delay
    time.sleep(random.uniform(0.01, 0.05))
    
    # Simulate some keys not found
    if random.random() < 0.1:
        return None
    
    return f"value_for_{key}"


def benchmark_cache_policies():
    """Benchmark different cache policies"""
    print("🏃 Benchmarking Cache Policies")
    print("=" * 50)
    
    capacity = 100
    test_keys = [f"key_{i}" for i in range(200)]  # More keys than capacity
    access_pattern = []
    
    # Generate access pattern (Zipf distribution - some keys more popular)
    for _ in range(1000):
        if random.random() < 0.8:
            # 80% access to top 20% keys (hot data)
            key = random.choice(test_keys[:40])
        else:
            # 20% access to remaining keys (cold data)
            key = random.choice(test_keys[40:])
        access_pattern.append(key)
    
    # Test different cache policies
    policies = {
        "LRU": LRUCache(capacity),
        "LFU": LFUCache(capacity),
        "FIFO": FIFOCache(capacity)
    }
    
    results = {}
    
    for policy_name, cache in policies.items():
        print(f"\n🧪 Testing {policy_name} Policy")
        print("-" * 30)
        
        cache.clear()
        start_time = time.time()
        
        for key in access_pattern:
            value = cache.get(key)
            if value is None:
                # Cache miss - fetch from data source
                value = simulate_data_source(key)
                if value:
                    cache.put(key, value)
        
        duration = time.time() - start_time
        
        # Collect results
        results[policy_name] = {
            'hit_rate': cache.stats.hit_rate,
            'miss_rate': cache.stats.miss_rate,
            'total_requests': cache.stats.total_requests,
            'evictions': cache.stats.evictions,
            'duration': duration
        }
        
        print(f"Hit Rate: {cache.stats.hit_rate:.2%}")
        print(f"Miss Rate: {cache.stats.miss_rate:.2%}")
        print(f"Total Requests: {cache.stats.total_requests}")
        print(f"Evictions: {cache.stats.evictions}")
        print(f"Duration: {duration:.2f} seconds")
    
    # Compare results
    print(f"\n📊 Performance Comparison")
    print("-" * 40)
    
    best_hit_rate = max(results.values(), key=lambda x: x['hit_rate'])
    best_performance = min(results.values(), key=lambda x: x['duration'])
    
    for policy, stats in results.items():
        print(f"{policy:6s}: Hit Rate {stats['hit_rate']:6.2%}, "
              f"Duration {stats['duration']:6.2f}s")
    
    return results


def test_distributed_cache():
    """Test distributed cache behavior"""
    print("\n🌐 Testing Distributed Cache")
    print("=" * 50)
    
    # Create distributed cache with 3 nodes
    nodes = ["node1", "node2", "node3"]
    dist_cache = DistributedCache(nodes, lambda cap: LRUCache(cap), capacity_per_node=50)
    
    # Add some data
    test_data = {f"key_{i}": f"value_{i}" for i in range(100)}
    
    print("📝 Adding data to distributed cache...")
    for key, value in test_data.items():
        dist_cache.put(key, value)
    
    # Test retrieval
    print("🔍 Testing data retrieval...")
    found_count = 0
    for key in test_data.keys():
        if dist_cache.get(key) is not None:
            found_count += 1
    
    print(f"Found {found_count}/{len(test_data)} items")
    print(f"Hit Rate: {dist_cache.stats.hit_rate:.2%}")
    
    # Test node addition
    print("➕ Adding new node...")
    dist_cache.add_node("node4")
    print(f"Cache now has {len(dist_cache.nodes)} nodes")
    
    # Test node removal
    print("➖ Removing a node...")
    dist_cache.remove_node("node2")
    print(f"Cache now has {len(dist_cache.nodes)} nodes")


def test_cache_warming():
    """Test cache warming strategies"""
    print("\n🔥 Testing Cache Warming")
    print("=" * 50)
    
    cache = LRUCache(capacity=50)
    warmer = CacheWarmer(cache, simulate_data_source)
    
    # Simulate access log
    access_log = []
    popular_keys = [f"popular_{i}" for i in range(10)]
    regular_keys = [f"regular_{i}" for i in range(50)]
    
    # Generate access pattern with popular keys accessed more frequently
    for _ in range(1000):
        if random.random() < 0.7:
            access_log.append(random.choice(popular_keys))
        else:
            access_log.append(random.choice(regular_keys))
    
    # Warm cache with popular keys
    warmer.warm_popular_keys(access_log, top_n=20)
    
    # Test performance after warming
    print("\n🎯 Testing performance after cache warming...")
    
    test_keys = random.sample(access_log, 100)
    hits = 0
    
    for key in test_keys:
        if cache.get(key) is not None:
            hits += 1
    
    print(f"Cache hit rate after warming: {hits/len(test_keys):.2%}")


def main():
    """Main function to run cache demonstrations"""
    print("🏗️ Cache Implementation Strategies Demonstration")
    print("=" * 60)
    
    try:
        # Benchmark different cache policies
        benchmark_results = benchmark_cache_policies()
        
        # Test distributed caching
        test_distributed_cache()
        
        # Test cache warming
        test_cache_warming()
        
        print("\n✅ Cache Strategy Demonstration Completed!")
        print("\n💡 Key System Design Insights:")
        print("   - LRU works well for temporal locality patterns")
        print("   - LFU is better for frequency-based access patterns")
        print("   - Distributed caching enables horizontal scaling")
        print("   - Cache warming improves initial performance")
        print("   - Monitor hit rates to optimize cache configuration")
        print("   - Choose eviction policy based on access patterns")
        
    except Exception as e:
        print(f"❌ Error during demonstration: {e}")


if __name__ == "__main__":
    main()
