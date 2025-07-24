#!/usr/bin/env python3
"""
Rate Limiter Implementation
Demonstrates different rate limiting algorithms for system design.
"""

import time
import threading
from collections import defaultdict, deque
from typing import Dict
import asyncio


class TokenBucketRateLimiter:
    """Token Bucket Algorithm Implementation"""
    
    def __init__(self, capacity: int, refill_rate: int):
        self.capacity = capacity
        self.tokens = capacity
        self.refill_rate = refill_rate
        self.last_refill = time.time()
        self.lock = threading.Lock()
    
    def allow_request(self, client_id: str) -> bool:
        """Check if request is allowed"""
        with self.lock:
            now = time.time()
            # Add tokens based on time elapsed
            tokens_to_add = (now - self.last_refill) * self.refill_rate
            self.tokens = min(self.capacity, self.tokens + tokens_to_add)
            self.last_refill = now
            
            if self.tokens >= 1:
                self.tokens -= 1
                return True
            return False


class SlidingWindowRateLimiter:
    """Sliding Window Algorithm Implementation"""
    
    def __init__(self, window_size: int, max_requests: int):
        self.window_size = window_size  # in seconds
        self.max_requests = max_requests
        self.requests: Dict[str, deque] = defaultdict(deque)
        self.lock = threading.Lock()
    
    def allow_request(self, client_id: str) -> bool:
        """Check if request is allowed"""
        with self.lock:
            now = time.time()
            window_start = now - self.window_size
            
            # Remove old requests outside the window
            while (self.requests[client_id] and 
                   self.requests[client_id][0] < window_start):
                self.requests[client_id].popleft()
            
            # Check if under limit
            if len(self.requests[client_id]) < self.max_requests:
                self.requests[client_id].append(now)
                return True
            return False


class FixedWindowRateLimiter:
    """Fixed Window Algorithm Implementation"""
    
    def __init__(self, window_size: int, max_requests: int):
        self.window_size = window_size
        self.max_requests = max_requests
        self.windows: Dict[str, Dict[int, int]] = defaultdict(lambda: defaultdict(int))
        self.lock = threading.Lock()
    
    def allow_request(self, client_id: str) -> bool:
        """Check if request is allowed"""
        with self.lock:
            now = int(time.time())
            window = now // self.window_size
            
            if self.windows[client_id][window] < self.max_requests:
                self.windows[client_id][window] += 1
                return True
            return False


class LeakyBucketRateLimiter:
    """Leaky Bucket Algorithm Implementation"""
    
    def __init__(self, capacity: int, leak_rate: int):
        self.capacity = capacity
        self.queue = []
        self.leak_rate = leak_rate
        self.last_leak = time.time()
        self.lock = threading.Lock()
    
    def allow_request(self, client_id: str) -> bool:
        """Check if request is allowed"""
        with self.lock:
            now = time.time()
            # Leak requests based on time elapsed
            time_passed = now - self.last_leak
            requests_to_leak = int(time_passed * self.leak_rate)
            
            for _ in range(min(requests_to_leak, len(self.queue))):
                self.queue.pop(0)
            
            self.last_leak = now
            
            # Add request if space available
            if len(self.queue) < self.capacity:
                self.queue.append((client_id, now))
                return True
            return False


class DistributedRateLimiter:
    """Redis-based Distributed Rate Limiter (simulation)"""
    
    def __init__(self, window_size: int, max_requests: int):
        self.window_size = window_size
        self.max_requests = max_requests
        # Simulate Redis with a dict
        self.redis_simulation = {}
        self.lock = threading.Lock()
    
    def allow_request(self, client_id: str) -> bool:
        """Check if request is allowed using sliding window log"""
        with self.lock:
            now = time.time()
            key = f"rate_limit:{client_id}"
            
            # Get current requests
            requests = self.redis_simulation.get(key, [])
            
            # Remove old requests
            window_start = now - self.window_size
            requests = [req_time for req_time in requests if req_time > window_start]
            
            # Check limit
            if len(requests) < self.max_requests:
                requests.append(now)
                self.redis_simulation[key] = requests
                return True
            
            self.redis_simulation[key] = requests
            return False


def demo_rate_limiters():
    """Demonstrate different rate limiting algorithms"""
    print("Rate Limiter Demonstration")
    print("=" * 50)
    
    # Token Bucket Demo
    print("\n1. Token Bucket Rate Limiter (5 tokens, 2 tokens/sec)")
    token_bucket = TokenBucketRateLimiter(capacity=5, refill_rate=2)
    
    for i in range(8):
        allowed = token_bucket.allow_request("user1")
        print(f"Request {i+1}: {'✓' if allowed else '✗'} "
              f"(tokens: {token_bucket.tokens:.2f})")
        time.sleep(0.3)
    
    # Sliding Window Demo
    print("\n2. Sliding Window Rate Limiter (3 requests/2 seconds)")
    sliding_window = SlidingWindowRateLimiter(window_size=2, max_requests=3)
    
    for i in range(6):
        allowed = sliding_window.allow_request("user2")
        print(f"Request {i+1}: {'✓' if allowed else '✗'}")
        time.sleep(0.4)
    
    # Fixed Window Demo
    print("\n3. Fixed Window Rate Limiter (2 requests/1 second)")
    fixed_window = FixedWindowRateLimiter(window_size=1, max_requests=2)
    
    for i in range(5):
        allowed = fixed_window.allow_request("user3")
        print(f"Request {i+1}: {'✓' if allowed else '✗'}")
        time.sleep(0.6)


async def async_rate_limiter_demo():
    """Async demonstration of rate limiting"""
    print("\n4. Async Rate Limiter Test")
    rate_limiter = TokenBucketRateLimiter(capacity=3, refill_rate=1)
    
    async def make_request(client_id: str, request_id: int):
        allowed = rate_limiter.allow_request(client_id)
        print(f"Async Request {request_id}: {'✓' if allowed else '✗'}")
        return allowed
    
    # Create multiple concurrent requests
    tasks = [make_request("async_user", i) for i in range(6)]
    results = await asyncio.gather(*tasks)
    print(f"Allowed requests: {sum(results)}/{len(results)}")


def benchmark_rate_limiters():
    """Benchmark different rate limiter implementations"""
    print("\n5. Rate Limiter Performance Benchmark")
    print("=" * 40)
    
    limiters = {
        "Token Bucket": TokenBucketRateLimiter(100, 50),
        "Sliding Window": SlidingWindowRateLimiter(1, 100),
        "Fixed Window": FixedWindowRateLimiter(1, 100),
        "Leaky Bucket": LeakyBucketRateLimiter(100, 50)
    }
    
    num_requests = 1000
    
    for name, limiter in limiters.items():
        start_time = time.time()
        allowed_count = 0
        
        for i in range(num_requests):
            if limiter.allow_request(f"bench_user_{i % 10}"):
                allowed_count += 1
        
        duration = time.time() - start_time
        print(f"{name}: {allowed_count}/{num_requests} allowed "
              f"in {duration:.3f}s ({num_requests/duration:.0f} req/s)")


if __name__ == "__main__":
    demo_rate_limiters()
    
    # Run async demo
    print("\nRunning async demo...")
    asyncio.run(async_rate_limiter_demo())
    
    # Run benchmark
    benchmark_rate_limiters()
    
    print("\nRate limiter demonstration completed!")
