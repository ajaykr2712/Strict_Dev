#!/usr/bin/env python3
"""
Message Queue Implementation
Demonstrates different message queue patterns and implementations.
"""

import time
import threading
import queue
from collections import defaultdict, deque
from typing import Dict, List, Callable, Any
from dataclasses import dataclass
from enum import Enum
import uuid


class MessagePriority(Enum):
    LOW = 1
    MEDIUM = 2
    HIGH = 3
    CRITICAL = 4


@dataclass
class Message:
    id: str
    content: Any
    priority: MessagePriority = MessagePriority.MEDIUM
    timestamp: float = None
    retry_count: int = 0
    max_retries: int = 3
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = time.time()
        if self.id is None:
            self.id = str(uuid.uuid4())


class SimpleQueue:
    """Basic FIFO Queue Implementation"""
    
    def __init__(self, max_size: int = 1000):
        self.queue = queue.Queue(maxsize=max_size)
        self.stats = {"sent": 0, "received": 0, "failed": 0}
    
    def send(self, message: Message) -> bool:
        """Send message to queue"""
        try:
            self.queue.put(message, block=False)
            self.stats["sent"] += 1
            return True
        except queue.Full:
            self.stats["failed"] += 1
            return False
    
    def receive(self, timeout: float = 1.0) -> Message:
        """Receive message from queue"""
        try:
            message = self.queue.get(timeout=timeout)
            self.stats["received"] += 1
            return message
        except queue.Empty:
            return None
    
    def size(self) -> int:
        return self.queue.qsize()


class PriorityQueue:
    """Priority-based Message Queue"""
    
    def __init__(self, max_size: int = 1000):
        self.queues = {
            MessagePriority.CRITICAL: deque(),
            MessagePriority.HIGH: deque(),
            MessagePriority.MEDIUM: deque(),
            MessagePriority.LOW: deque()
        }
        self.max_size = max_size
        self.lock = threading.Lock()
        self.stats = {"sent": 0, "received": 0, "failed": 0}
    
    def send(self, message: Message) -> bool:
        """Send message based on priority"""
        with self.lock:
            total_size = sum(len(q) for q in self.queues.values())
            if total_size >= self.max_size:
                self.stats["failed"] += 1
                return False
            
            self.queues[message.priority].append(message)
            self.stats["sent"] += 1
            return True
    
    def receive(self) -> Message:
        """Receive highest priority message"""
        with self.lock:
            # Check queues in priority order
            for priority in [MessagePriority.CRITICAL, MessagePriority.HIGH, 
                           MessagePriority.MEDIUM, MessagePriority.LOW]:
                if self.queues[priority]:
                    message = self.queues[priority].popleft()
                    self.stats["received"] += 1
                    return message
            return None
    
    def size(self) -> int:
        with self.lock:
            return sum(len(q) for q in self.queues.values())


class TopicBasedQueue:
    """Pub/Sub Topic-based Message Queue"""
    
    def __init__(self):
        self.topics: Dict[str, List[queue.Queue]] = defaultdict(list)
        self.subscribers: Dict[str, List[str]] = defaultdict(list)
        self.lock = threading.Lock()
        self.stats = {"published": 0, "delivered": 0, "failed": 0}
    
    def subscribe(self, subscriber_id: str, topic: str) -> queue.Queue:
        """Subscribe to a topic"""
        with self.lock:
            subscriber_queue = queue.Queue()
            self.topics[topic].append(subscriber_queue)
            self.subscribers[topic].append(subscriber_id)
            return subscriber_queue
    
    def publish(self, topic: str, message: Message) -> int:
        """Publish message to topic"""
        with self.lock:
            delivered_count = 0
            for subscriber_queue in self.topics[topic]:
                try:
                    subscriber_queue.put(message, block=False)
                    delivered_count += 1
                except queue.Full:
                    self.stats["failed"] += 1
            
            self.stats["published"] += 1
            self.stats["delivered"] += delivered_count
            return delivered_count
    
    def get_topic_info(self, topic: str) -> Dict:
        """Get information about a topic"""
        with self.lock:
            return {
                "topic": topic,
                "subscribers": len(self.subscribers[topic]),
                "subscriber_ids": self.subscribers[topic].copy()
            }


class DeadLetterQueue:
    """Dead Letter Queue for failed messages"""
    
    def __init__(self):
        self.queue = deque()
        self.lock = threading.Lock()
    
    def add_failed_message(self, message: Message, reason: str):
        """Add failed message to DLQ"""
        with self.lock:
            failed_message = {
                "message": message,
                "reason": reason,
                "failed_at": time.time()
            }
            self.queue.append(failed_message)
    
    def get_failed_messages(self) -> List[Dict]:
        """Get all failed messages"""
        with self.lock:
            return list(self.queue)
    
    def retry_message(self, message_id: str) -> Message:
        """Retry a specific failed message"""
        with self.lock:
            for i, failed_msg in enumerate(self.queue):
                if failed_msg["message"].id == message_id:
                    message = failed_msg["message"]
                    message.retry_count += 1
                    del self.queue[i]
                    return message
            return None


class MessageProcessor:
    """Message processor with retry logic"""
    
    def __init__(self, processor_func: Callable[[Message], bool]):
        self.processor_func = processor_func
        self.dlq = DeadLetterQueue()
        self.stats = {"processed": 0, "failed": 0, "retried": 0}
    
    def process_message(self, message: Message) -> bool:
        """Process message with retry logic"""
        try:
            success = self.processor_func(message)
            if success:
                self.stats["processed"] += 1
                return True
            else:
                return self._handle_failure(message, "Processing failed")
        except Exception as e:
            return self._handle_failure(message, f"Exception: {str(e)}")
    
    def _handle_failure(self, message: Message, reason: str) -> bool:
        """Handle processing failure"""
        if message.retry_count < message.max_retries:
            message.retry_count += 1
            self.stats["retried"] += 1
            return False  # Will be retried
        else:
            self.dlq.add_failed_message(message, reason)
            self.stats["failed"] += 1
            return True  # Moved to DLQ, don't retry


class MessageQueueCluster:
    """Distributed message queue cluster simulation"""
    
    def __init__(self, num_nodes: int = 3):
        self.nodes = [SimpleQueue() for _ in range(num_nodes)]
        self.node_index = 0
        self.lock = threading.Lock()
    
    def send(self, message: Message) -> bool:
        """Send message using round-robin load balancing"""
        with self.lock:
            node = self.nodes[self.node_index]
            self.node_index = (self.node_index + 1) % len(self.nodes)
            return node.send(message)
    
    def receive_from_any(self) -> Message:
        """Receive message from any available node"""
        for node in self.nodes:
            message = node.receive(timeout=0.1)
            if message:
                return message
        return None
    
    def get_cluster_stats(self) -> Dict:
        """Get cluster-wide statistics"""
        total_stats = {"sent": 0, "received": 0, "failed": 0, "total_size": 0}
        for i, node in enumerate(self.nodes):
            total_stats["sent"] += node.stats["sent"]
            total_stats["received"] += node.stats["received"]
            total_stats["failed"] += node.stats["failed"]
            total_stats["total_size"] += node.size()
        return total_stats


def demo_simple_queue():
    """Demonstrate simple FIFO queue"""
    print("1. Simple FIFO Queue Demo")
    print("-" * 30)
    
    simple_queue = SimpleQueue(max_size=5)
    
    # Send messages
    for i in range(7):  # Try to send more than max size
        message = Message(id=f"msg_{i}", content=f"Hello {i}")
        success = simple_queue.send(message)
        print(f"Sent message {i}: {'✓' if success else '✗ (queue full)'}")
    
    # Receive messages
    print("\nReceiving messages:")
    while simple_queue.size() > 0:
        message = simple_queue.receive()
        if message:
            print(f"Received: {message.content}")
    
    print(f"Queue stats: {simple_queue.stats}")


def demo_priority_queue():
    """Demonstrate priority queue"""
    print("\n2. Priority Queue Demo")
    print("-" * 25)
    
    pq = PriorityQueue()
    
    # Send messages with different priorities
    messages = [
        Message(id="1", content="Low priority", priority=MessagePriority.LOW),
        Message(id="2", content="Critical task!", priority=MessagePriority.CRITICAL),
        Message(id="3", content="Medium task", priority=MessagePriority.MEDIUM),
        Message(id="4", content="High priority", priority=MessagePriority.HIGH),
        Message(id="5", content="Another critical!", priority=MessagePriority.CRITICAL)
    ]
    
    for msg in messages:
        pq.send(msg)
        print(f"Sent: {msg.content} ({msg.priority.name})")
    
    print("\nReceiving in priority order:")
    while pq.size() > 0:
        message = pq.receive()
        if message:
            print(f"Received: {message.content} ({message.priority.name})")


def demo_topic_queue():
    """Demonstrate topic-based pub/sub"""
    print("\n3. Topic-Based Pub/Sub Demo")
    print("-" * 30)
    
    topic_queue = TopicBasedQueue()
    
    # Subscribe to topics
    user_queue = topic_queue.subscribe("user_service", "user_events")
    order_queue = topic_queue.subscribe("order_service", "order_events")
    analytics_queue = topic_queue.subscribe("analytics", "user_events")
    
    # Publish messages
    user_msg = Message(id="u1", content="User registered")
    order_msg = Message(id="o1", content="Order placed")
    
    print("Publishing messages:")
    delivered = topic_queue.publish("user_events", user_msg)
    print(f"User event delivered to {delivered} subscribers")
    
    delivered = topic_queue.publish("order_events", order_msg)
    print(f"Order event delivered to {delivered} subscribers")
    
    # Receive messages
    print("\nReceiving messages:")
    msg = user_queue.get()
    print(f"User service received: {msg.content}")
    
    msg = analytics_queue.get()
    print(f"Analytics received: {msg.content}")
    
    msg = order_queue.get()
    print(f"Order service received: {msg.content}")


def demo_message_processing():
    """Demonstrate message processing with retries"""
    print("\n4. Message Processing with Retries Demo")
    print("-" * 40)
    
    # Simulate a processor that fails sometimes
    def unreliable_processor(message: Message) -> bool:
        # Simulate random failures
        import random
        if random.random() < 0.3:  # 30% failure rate
            print(f"Processing failed for: {message.content}")
            return False
        print(f"Successfully processed: {message.content}")
        return True
    
    processor = MessageProcessor(unreliable_processor)
    
    # Process messages
    messages = [
        Message(id=f"proc_{i}", content=f"Task {i}", max_retries=2)
        for i in range(5)
    ]
    
    for message in messages:
        while not processor.process_message(message):
            print(f"Retrying message: {message.content} (attempt {message.retry_count})")
    
    print(f"\nProcessing stats: {processor.stats}")
    print(f"Dead letter queue: {len(processor.dlq.get_failed_messages())} messages")


def demo_cluster():
    """Demonstrate message queue cluster"""
    print("\n5. Message Queue Cluster Demo")
    print("-" * 32)
    
    cluster = MessageQueueCluster(num_nodes=3)
    
    # Send messages (distributed across nodes)
    for i in range(9):
        message = Message(id=f"cluster_{i}", content=f"Distributed message {i}")
        cluster.send(message)
    
    print("Sent 9 messages across cluster")
    print(f"Cluster stats: {cluster.get_cluster_stats()}")
    
    # Receive some messages
    print("\nReceiving messages from cluster:")
    for _ in range(5):
        message = cluster.receive_from_any()
        if message:
            print(f"Received: {message.content}")


if __name__ == "__main__":
    print("Message Queue Implementations Demo")
    print("=" * 50)
    
    demo_simple_queue()
    demo_priority_queue()
    demo_topic_queue()
    demo_message_processing()
    demo_cluster()
    
    print("\nMessage queue demonstration completed!")
