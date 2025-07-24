# Saga Pattern

## Overview
The Saga pattern is a design pattern for managing distributed transactions across multiple microservices. Instead of using traditional ACID transactions that span multiple services, the Saga pattern breaks down the transaction into a series of smaller, local transactions, each with its own compensating action for rollback scenarios.

## Problem Statement
In microservices architecture, maintaining data consistency across multiple services is challenging because:
- **Traditional ACID transactions** don't work across service boundaries
- **Two-phase commit (2PC)** doesn't scale well and creates tight coupling
- **Service failures** can leave the system in an inconsistent state
- **Long-running transactions** can lock resources for extended periods
- **Network partitions** can cause distributed deadlocks

## Solution: Saga Pattern

The Saga pattern solves distributed transaction challenges by:
1. **Breaking down** complex transactions into smaller, manageable steps
2. **Executing** each step as a local transaction
3. **Coordinating** the sequence of operations
4. **Compensating** for failures by undoing completed steps
5. **Ensuring** eventual consistency across services

## Types of Saga Implementation

### 1. Orchestration-based Saga
- **Central coordinator** (orchestrator) manages the saga
- **Synchronous communication** between orchestrator and services
- **Centralized logic** for transaction flow control
- **Easier to monitor** and debug

### 2. Choreography-based Saga
- **Decentralized approach** with no central coordinator
- **Event-driven communication** between services
- **Each service** knows the next step in the saga
- **More resilient** but harder to monitor

## Saga Components

### Core Elements
- **Saga Execution Coordinator (SEC)**: Manages saga execution
- **Local Transactions**: Individual service operations
- **Compensating Actions**: Rollback operations for each step
- **Saga Log**: Persistent record of saga progress
- **Event Bus**: Communication mechanism (for choreography)

### Compensation Actions
- **Semantic rollback** (not physical undo)
- **Idempotent operations** to handle retry scenarios
- **Business logic** to reverse the effect of completed steps
- **Error handling** for compensation failures

## Orchestration-based Saga Implementation

### Saga Orchestrator
```python
from enum import Enum
from typing import List, Dict, Any, Callable
import uuid
import time

class SagaStatus(Enum):
    STARTED = "started"
    STEP_COMPLETED = "step_completed"
    COMPENSATING = "compensating"
    COMPLETED = "completed"
    FAILED = "failed"
    COMPENSATED = "compensated"

class SagaStep:
    def __init__(self, 
                 name: str,
                 action: Callable,
                 compensation: Callable,
                 service_name: str):
        self.name = name
        self.action = action
        self.compensation = compensation
        self.service_name = service_name
        self.executed = False
        self.compensated = False
        self.result = None

class SagaOrchestrator:
    def __init__(self, saga_id: str = None):
        self.saga_id = saga_id or str(uuid.uuid4())
        self.steps: List[SagaStep] = []
        self.current_step = 0
        self.status = SagaStatus.STARTED
        self.execution_log: List[Dict] = []
        self.start_time = time.time()
    
    def add_step(self, step: SagaStep):
        """Add a step to the saga"""
        self.steps.append(step)
    
    def execute(self) -> Dict[str, Any]:
        """Execute the saga steps"""
        self._log_event("Saga started")
        
        try:
            # Execute all steps sequentially
            for i, step in enumerate(self.steps):
                self.current_step = i
                self._execute_step(step)
            
            self.status = SagaStatus.COMPLETED
            self._log_event("Saga completed successfully")
            
        except Exception as e:
            self.status = SagaStatus.COMPENSATING
            self._log_event(f"Saga failed at step {self.current_step}: {str(e)}")
            self._compensate()
        
        return self._get_saga_result()
    
    def _execute_step(self, step: SagaStep):
        """Execute a single saga step"""
        self._log_event(f"Executing step: {step.name}")
        
        try:
            # Execute the step action
            result = step.action()
            step.result = result
            step.executed = True
            
            self._log_event(f"Step completed: {step.name}")
            
        except Exception as e:
            self._log_event(f"Step failed: {step.name} - {str(e)}")
            raise e
    
    def _compensate(self):
        """Execute compensation actions for completed steps"""
        self._log_event("Starting compensation process")
        
        # Compensate in reverse order
        for i in range(self.current_step, -1, -1):
            step = self.steps[i]
            
            if step.executed and not step.compensated:
                try:
                    self._log_event(f"Compensating step: {step.name}")
                    step.compensation()
                    step.compensated = True
                    self._log_event(f"Step compensated: {step.name}")
                    
                except Exception as e:
                    self._log_event(f"Compensation failed: {step.name} - {str(e)}")
                    # Continue with other compensations
        
        self.status = SagaStatus.COMPENSATED
        self._log_event("Compensation process completed")
    
    def _log_event(self, message: str):
        """Log saga events"""
        event = {
            'timestamp': time.time(),
            'saga_id': self.saga_id,
            'step': self.current_step,
            'status': self.status.value,
            'message': message
        }
        self.execution_log.append(event)
        print(f"[{self.saga_id[:8]}] {message}")
    
    def _get_saga_result(self) -> Dict[str, Any]:
        """Get saga execution result"""
        return {
            'saga_id': self.saga_id,
            'status': self.status.value,
            'execution_time': time.time() - self.start_time,
            'completed_steps': len([s for s in self.steps if s.executed]),
            'compensated_steps': len([s for s in self.steps if s.compensated]),
            'execution_log': self.execution_log
        }
```

### E-commerce Order Processing Example
```python
class OrderService:
    def __init__(self):
        self.orders = {}
        self.reserved_inventory = {}
    
    def create_order(self, order_data: Dict) -> str:
        """Create a new order"""
        order_id = str(uuid.uuid4())
        self.orders[order_id] = {
            'id': order_id,
            'customer_id': order_data['customer_id'],
            'items': order_data['items'],
            'total': order_data['total'],
            'status': 'created'
        }
        print(f"✅ Order created: {order_id}")
        return order_id
    
    def cancel_order(self, order_id: str):
        """Cancel an order (compensation)"""
        if order_id in self.orders:
            self.orders[order_id]['status'] = 'cancelled'
            print(f"❌ Order cancelled: {order_id}")

class InventoryService:
    def __init__(self):
        self.inventory = {'product_1': 100, 'product_2': 50}
        self.reservations = {}
    
    def reserve_inventory(self, order_id: str, items: List[Dict]) -> bool:
        """Reserve inventory for order"""
        reservation_id = str(uuid.uuid4())
        
        for item in items:
            product_id = item['product_id']
            quantity = item['quantity']
            
            if self.inventory.get(product_id, 0) < quantity:
                raise Exception(f"Insufficient inventory for {product_id}")
            
            self.inventory[product_id] -= quantity
        
        self.reservations[reservation_id] = {
            'order_id': order_id,
            'items': items
        }
        
        print(f"📦 Inventory reserved: {reservation_id}")
        return reservation_id
    
    def release_reservation(self, order_id: str):
        """Release inventory reservation (compensation)"""
        for res_id, reservation in self.reservations.items():
            if reservation['order_id'] == order_id:
                for item in reservation['items']:
                    product_id = item['product_id']
                    quantity = item['quantity']
                    self.inventory[product_id] += quantity
                
                del self.reservations[res_id]
                print(f"🔄 Inventory released for order: {order_id}")
                break

class PaymentService:
    def __init__(self):
        self.payments = {}
        self.customer_balances = {'customer_1': 1000, 'customer_2': 500}
    
    def process_payment(self, order_id: str, customer_id: str, amount: float) -> str:
        """Process payment for order"""
        if self.customer_balances.get(customer_id, 0) < amount:
            raise Exception(f"Insufficient funds for customer {customer_id}")
        
        payment_id = str(uuid.uuid4())
        self.customer_balances[customer_id] -= amount
        self.payments[payment_id] = {
            'order_id': order_id,
            'customer_id': customer_id,
            'amount': amount,
            'status': 'completed'
        }
        
        print(f"💳 Payment processed: {payment_id}")
        return payment_id
    
    def refund_payment(self, order_id: str):
        """Refund payment (compensation)"""
        for payment_id, payment in self.payments.items():
            if payment['order_id'] == order_id:
                customer_id = payment['customer_id']
                amount = payment['amount']
                
                self.customer_balances[customer_id] += amount
                payment['status'] = 'refunded'
                
                print(f"💰 Payment refunded: {payment_id}")
                break

# Saga Definition for Order Processing
def create_order_saga(order_data: Dict) -> SagaOrchestrator:
    """Create saga for order processing"""
    order_service = OrderService()
    inventory_service = InventoryService()
    payment_service = PaymentService()
    
    saga = SagaOrchestrator()
    order_id = None
    
    # Step 1: Create Order
    def create_order():
        nonlocal order_id
        order_id = order_service.create_order(order_data)
        return order_id
    
    def compensate_order():
        if order_id:
            order_service.cancel_order(order_id)
    
    saga.add_step(SagaStep(
        name="create_order",
        action=create_order,
        compensation=compensate_order,
        service_name="order_service"
    ))
    
    # Step 2: Reserve Inventory
    def reserve_inventory():
        return inventory_service.reserve_inventory(order_id, order_data['items'])
    
    def compensate_inventory():
        inventory_service.release_reservation(order_id)
    
    saga.add_step(SagaStep(
        name="reserve_inventory",
        action=reserve_inventory,
        compensation=compensate_inventory,
        service_name="inventory_service"
    ))
    
    # Step 3: Process Payment
    def process_payment():
        return payment_service.process_payment(
            order_id, order_data['customer_id'], order_data['total']
        )
    
    def compensate_payment():
        payment_service.refund_payment(order_id)
    
    saga.add_step(SagaStep(
        name="process_payment",
        action=process_payment,
        compensation=compensate_payment,
        service_name="payment_service"
    ))
    
    return saga
```

## Choreography-based Saga

### Event-Driven Implementation
```python
import json
from typing import Set
from abc import ABC, abstractmethod

class EventBus:
    def __init__(self):
        self.subscribers = {}
    
    def subscribe(self, event_type: str, handler: Callable):
        if event_type not in self.subscribers:
            self.subscribers[event_type] = []
        self.subscribers[event_type].append(handler)
    
    def publish(self, event_type: str, event_data: Dict):
        print(f"📢 Event published: {event_type}")
        if event_type in self.subscribers:
            for handler in self.subscribers[event_type]:
                try:
                    handler(event_data)
                except Exception as e:
                    print(f"❌ Event handler failed: {str(e)}")

class SagaParticipant(ABC):
    def __init__(self, event_bus: EventBus):
        self.event_bus = event_bus
        self._register_handlers()
    
    @abstractmethod
    def _register_handlers(self):
        """Register event handlers"""
        pass
    
    def emit_event(self, event_type: str, data: Dict):
        """Emit an event to the event bus"""
        self.event_bus.publish(event_type, data)

# Choreography-based Order Service
class ChoreographyOrderService(SagaParticipant):
    def __init__(self, event_bus: EventBus):
        self.orders = {}
        super().__init__(event_bus)
    
    def _register_handlers(self):
        self.event_bus.subscribe('payment_failed', self._handle_payment_failure)
        self.event_bus.subscribe('inventory_reservation_failed', self._handle_inventory_failure)
    
    def create_order(self, order_data: Dict):
        """Create order and start the saga"""
        order_id = str(uuid.uuid4())
        self.orders[order_id] = order_data
        
        # Emit order created event
        self.emit_event('order_created', {
            'order_id': order_id,
            'customer_id': order_data['customer_id'],
            'items': order_data['items'],
            'total': order_data['total']
        })
        
        return order_id
    
    def _handle_payment_failure(self, event_data: Dict):
        """Handle payment failure compensation"""
        order_id = event_data['order_id']
        if order_id in self.orders:
            self.orders[order_id]['status'] = 'cancelled'
            print(f"❌ Order cancelled due to payment failure: {order_id}")
    
    def _handle_inventory_failure(self, event_data: Dict):
        """Handle inventory failure compensation"""
        order_id = event_data['order_id']
        if order_id in self.orders:
            self.orders[order_id]['status'] = 'cancelled'
            print(f"❌ Order cancelled due to inventory failure: {order_id}")
```

## Benefits and Trade-offs

### Benefits
- **Scalability**: No global locks or 2PC overhead
- **Fault Tolerance**: Services can fail and recover independently
- **Technology Diversity**: Each service can use different databases
- **Performance**: Local transactions are faster than distributed ones
- **Flexibility**: Easy to add new services to the workflow

### Trade-offs
- **Complexity**: More complex than ACID transactions
- **Eventual Consistency**: Temporary inconsistent states
- **Debugging**: Harder to trace across multiple services
- **Compensation Logic**: Additional code for rollback scenarios
- **Data Modeling**: Requires careful design of compensation actions

## Best Practices

### 1. Design Compensation Actions
- **Idempotent operations** that can be safely retried
- **Business-level rollback** rather than technical undo
- **Handle partial failures** gracefully
- **Log all compensation** attempts

### 2. Saga State Management
- **Persist saga state** to handle service restarts
- **Use unique identifiers** for correlation
- **Implement timeouts** for long-running sagas
- **Monitor saga progress** and health

### 3. Error Handling
- **Distinguish between retryable** and non-retryable errors
- **Implement circuit breakers** for external services
- **Use dead letter queues** for failed messages
- **Provide manual intervention** capabilities

### 4. Testing
- **Test happy path** scenarios
- **Test all failure scenarios** and compensations
- **Verify idempotency** of all operations
- **Load test** saga performance

## Interview Questions

1. **When would you use Saga pattern over traditional ACID transactions?**
   - Microservices, long-running processes, cross-service transactions

2. **Explain the difference between orchestration and choreography.**
   - Centralized vs. decentralized coordination approaches

3. **How do you handle partial failures in a saga?**
   - Compensation actions, state tracking, and rollback mechanisms

4. **What are the challenges of implementing saga pattern?**
   - Complexity, eventual consistency, debugging, compensation design

5. **How do you ensure data consistency in saga pattern?**
   - Eventually consistent through compensation actions and careful design

---
Continue to the next pattern for deeper system design mastery!
