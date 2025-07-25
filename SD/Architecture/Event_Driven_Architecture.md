# Event-Driven Architecture

## Overview

Event-Driven Architecture (EDA) is a software architecture pattern promoting the production, detection, consumption of, and reaction to events. An event can be defined as a significant change in state or an update.

## Core Concepts

### 1. Events
- **Domain Events**: Business-significant occurrences
- **System Events**: Technical events (errors, monitoring)
- **Integration Events**: Cross-service communication events

### 2. Event Producers
- Components that detect and publish events
- Can be user actions, system processes, or external integrations

### 3. Event Consumers
- Components that listen for and process events
- Can trigger business logic, update data, or produce new events

### 4. Event Bus/Broker
- Infrastructure for event routing and delivery
- Provides decoupling between producers and consumers

## Implementation Patterns

### Event Sourcing
```python
class EventStore:
    def __init__(self):
        self.events = []
        
    def append_event(self, aggregate_id, event):
        event_data = {
            'aggregate_id': aggregate_id,
            'event_type': event.__class__.__name__,
            'event_data': event.__dict__,
            'timestamp': datetime.utcnow(),
            'version': self._get_next_version(aggregate_id)
        }
        self.events.append(event_data)
        
    def get_events(self, aggregate_id, from_version=0):
        return [e for e in self.events 
                if e['aggregate_id'] == aggregate_id 
                and e['version'] > from_version]
        
    def _get_next_version(self, aggregate_id):
        events = self.get_events(aggregate_id)
        return len(events) + 1

class OrderAggregate:
    def __init__(self, order_id):
        self.order_id = order_id
        self.status = 'PENDING'
        self.items = []
        self.version = 0
        self.uncommitted_events = []
        
    def add_item(self, item):
        if self.status == 'PENDING':
            self.items.append(item)
            self._apply_event(OrderItemAdded(self.order_id, item))
            
    def confirm_order(self):
        if self.status == 'PENDING' and self.items:
            self.status = 'CONFIRMED'
            self._apply_event(OrderConfirmed(self.order_id))
            
    def _apply_event(self, event):
        self.uncommitted_events.append(event)
        
    def mark_events_committed(self):
        self.uncommitted_events.clear()
```

### CQRS with Events
```python
class EventHandler:
    def __init__(self, event_bus):
        self.event_bus = event_bus
        
    def handle(self, event):
        handlers = self._get_handlers(event.__class__)
        for handler in handlers:
            try:
                handler(event)
            except Exception as e:
                self._handle_error(event, e)
                
class OrderProjectionHandler:
    def __init__(self, read_db):
        self.read_db = read_db
        
    def handle_order_confirmed(self, event):
        # Update read model
        self.read_db.update_order_projection({
            'order_id': event.order_id,
            'status': 'CONFIRMED',
            'confirmed_at': event.timestamp
        })
        
    def handle_order_shipped(self, event):
        self.read_db.update_order_projection({
            'order_id': event.order_id,
            'status': 'SHIPPED',
            'tracking_number': event.tracking_number
        })
```

## Event Bus Implementation

### Simple In-Memory Event Bus
```python
import asyncio
from typing import Dict, List, Callable

class EventBus:
    def __init__(self):
        self.subscribers: Dict[str, List[Callable]] = {}
        
    def subscribe(self, event_type: str, handler: Callable):
        if event_type not in self.subscribers:
            self.subscribers[event_type] = []
        self.subscribers[event_type].append(handler)
        
    def publish(self, event_type: str, event_data: dict):
        if event_type in self.subscribers:
            for handler in self.subscribers[event_type]:
                try:
                    if asyncio.iscoroutinefunction(handler):
                        asyncio.create_task(handler(event_data))
                    else:
                        handler(event_data)
                except Exception as e:
                    self._handle_error(event_type, event_data, e)
                    
    def _handle_error(self, event_type, event_data, error):
        # Log error, send to dead letter queue, etc.
        print(f"Error handling event {event_type}: {error}")
```

### Distributed Event Bus with Message Broker
```python
import pika
import json

class RabbitMQEventBus:
    def __init__(self, connection_params):
        self.connection = pika.BlockingConnection(connection_params)
        self.channel = self.connection.channel()
        
    def publish_event(self, event_type, event_data, routing_key=None):
        exchange_name = 'events'
        self.channel.exchange_declare(
            exchange=exchange_name,
            exchange_type='topic',
            durable=True
        )
        
        message = {
            'event_type': event_type,
            'event_data': event_data,
            'timestamp': datetime.utcnow().isoformat()
        }
        
        self.channel.basic_publish(
            exchange=exchange_name,
            routing_key=routing_key or event_type,
            body=json.dumps(message),
            properties=pika.BasicProperties(
                delivery_mode=2,  # Make message persistent
                content_type='application/json'
            )
        )
        
    def subscribe_to_events(self, event_pattern, handler):
        queue_name = f"service_{event_pattern}"
        self.channel.queue_declare(queue=queue_name, durable=True)
        self.channel.queue_bind(
            exchange='events',
            queue=queue_name,
            routing_key=event_pattern
        )
        
        def callback(ch, method, properties, body):
            try:
                message = json.loads(body)
                handler(message)
                ch.basic_ack(delivery_tag=method.delivery_tag)
            except Exception as e:
                # Handle error, potentially send to DLQ
                ch.basic_nack(
                    delivery_tag=method.delivery_tag,
                    requeue=False
                )
                
        self.channel.basic_consume(
            queue=queue_name,
            on_message_callback=callback
        )
```

## Benefits

### 1. Loose Coupling
- Components don't need to know about each other directly
- Easy to add new event consumers without modifying producers
- Better system flexibility and maintainability

### 2. Scalability
- Event processing can be distributed across multiple consumers
- Asynchronous processing improves system responsiveness
- Natural partitioning and parallel processing

### 3. Resilience
- System can continue operating even if some components fail
- Events can be persisted and replayed
- Better fault isolation

### 4. Auditability
- Complete event history provides audit trail
- Easy to track what happened and when
- Supports compliance and debugging

## Challenges

### 1. Event Ordering
- Ensuring events are processed in correct order
- Handling out-of-order event delivery
- Implementing event versioning

### 2. Event Schema Evolution
- Managing changes to event structure
- Backward compatibility concerns
- Event versioning strategies

### 3. Error Handling
- Dead letter queues for failed events
- Retry mechanisms and backoff strategies
- Poison message handling

### 4. Debugging and Monitoring
- Distributed tracing across event flows
- Event correlation and causation tracking
- Performance monitoring and alerting

## Best Practices

### 1. Event Design
- Events should be immutable
- Include all necessary context
- Use clear, descriptive event names
- Version events from the beginning

### 2. Event Processing
- Ensure idempotent event handlers
- Implement proper error handling
- Use event correlation IDs for tracing
- Handle duplicate events gracefully

### 3. Performance Optimization
- Batch event processing where possible
- Use appropriate serialization formats
- Implement event partitioning for scaling
- Monitor and tune throughput

## Tools and Technologies

### Message Brokers
- **Apache Kafka**: High-throughput distributed streaming
- **RabbitMQ**: Feature-rich AMQP message broker
- **Amazon EventBridge**: Serverless event bus
- **Azure Event Hubs**: Big data streaming platform

### Event Streaming Platforms
- **Apache Pulsar**: Multi-tenant, geo-replicated messaging
- **Amazon Kinesis**: Real-time data streaming
- **Google Pub/Sub**: Asynchronous messaging service

### Event Sourcing Frameworks
- **EventStore**: Purpose-built event store database
- **Apache Kafka**: Can be used as event store
- **Azure Cosmos DB**: Change feed functionality

## Common Patterns

### 1. Choreography vs Orchestration
- **Choreography**: Each service knows what to do when events occur
- **Orchestration**: Central coordinator manages the workflow

### 2. Event Sourcing
- Store events as the primary source of truth
- Rebuild current state by replaying events
- Provides complete audit trail

### 3. CQRS (Command Query Responsibility Segregation)
- Separate models for reading and writing
- Events update multiple read models
- Optimized queries and commands

### 4. Saga Pattern
- Manage distributed transactions with events
- Compensating actions for rollback
- Choreography or orchestration based

## Real-World Use Cases

### E-commerce Platform
- Order processing workflow
- Inventory management
- Payment processing
- Shipping notifications

### IoT Systems
- Sensor data processing
- Real-time analytics
- Alerting and notifications
- Device management

### Financial Systems
- Transaction processing
- Risk management
- Compliance reporting
- Real-time fraud detection

## Conclusion

Event-Driven Architecture provides a powerful pattern for building scalable, resilient, and loosely coupled systems. While it introduces complexity in terms of event management and debugging, the benefits often outweigh the challenges for systems that need to handle high volumes of data and complex business workflows.
