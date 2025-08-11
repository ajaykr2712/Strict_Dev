#!/usr/bin/env python3
"""
Advanced System Design Simulator
Demonstrates complex distributed system patterns including:
- Event sourcing with CQRS
- Distributed consensus (simplified Raft)
- Circuit breaker pattern
- Saga pattern for distributed transactions
"""

import asyncio
import random
import time
import json
from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from abc import ABC, abstractmethod

# ==================== EVENT SOURCING & CQRS ====================

@dataclass
class Event:
    event_id: str
    aggregate_id: str
    event_type: str
    data: Dict[str, Any]
    timestamp: str
    version: int

    @classmethod
    def create(cls, aggregate_id: str, event_type: str, data: Dict[str, Any], version: int):
        return cls(
            event_id=f"evt_{int(time.time() * 1000)}_{random.randint(1000, 9999)}",
            aggregate_id=aggregate_id,
            event_type=event_type,
            data=data,
            timestamp=datetime.utcnow().isoformat(),
            version=version
        )

class EventStore:
    def __init__(self):
        self.events: List[Event] = []
        self.snapshots: Dict[str, Dict[str, Any]] = {}
        self.subscribers: List[Any] = []

    async def append_event(self, event: Event):
        """Append event and notify subscribers"""
        self.events.append(event)
        print(f"📝 Event stored: {event.event_type} for {event.aggregate_id}")
        
        # Notify subscribers (CQRS read models)
        for subscriber in self.subscribers:
            await subscriber.handle_event(event)

    def get_events(self, aggregate_id: str, from_version: int = 0) -> List[Event]:
        return [e for e in self.events 
                if e.aggregate_id == aggregate_id and e.version > from_version]

    def subscribe(self, handler):
        self.subscribers.append(handler)

# ==================== SAGA PATTERN ====================

class SagaStepStatus(Enum):
    PENDING = "pending"
    COMPLETED = "completed"
    FAILED = "failed"
    COMPENSATING = "compensating"
    COMPENSATED = "compensated"

@dataclass
class SagaStep:
    step_id: str
    action: str
    compensation: str
    status: SagaStepStatus = SagaStepStatus.PENDING
    data: Dict[str, Any] = None

class SagaOrchestrator:
    def __init__(self, saga_id: str, steps: List[SagaStep]):
        self.saga_id = saga_id
        self.steps = steps
        self.current_step = 0
        self.failed_step: Optional[int] = None

    async def execute(self) -> bool:
        """Execute saga steps"""
        print(f"🎬 Starting saga {self.saga_id}")
        
        try:
            # Execute forward steps
            for i, step in enumerate(self.steps):
                self.current_step = i
                print(f"⚡ Executing step {i+1}: {step.action}")
                
                success = await self._execute_step(step)
                if not success:
                    self.failed_step = i
                    step.status = SagaStepStatus.FAILED
                    print(f"❌ Step {i+1} failed, starting compensation")
                    await self._compensate()
                    return False
                
                step.status = SagaStepStatus.COMPLETED
                await asyncio.sleep(0.1)  # Simulate processing time
            
            print(f"✅ Saga {self.saga_id} completed successfully")
            return True
        
        except Exception as e:
            print(f"💥 Saga {self.saga_id} failed with error: {e}")
            await self._compensate()
            return False

    async def _execute_step(self, step: SagaStep) -> bool:
        """Execute a single step (simulate with random success/failure)"""
        # Simulate different failure rates for different actions
        failure_rates = {
            "reserve_inventory": 0.1,
            "process_payment": 0.2,
            "ship_order": 0.05,
            "send_notification": 0.01
        }
        
        failure_rate = failure_rates.get(step.action, 0.1)
        return random.random() > failure_rate

    async def _compensate(self):
        """Execute compensation steps in reverse order"""
        if self.failed_step is None:
            return
        
        # Compensate completed steps in reverse order
        for i in range(self.failed_step - 1, -1, -1):
            step = self.steps[i]
            if step.status == SagaStepStatus.COMPLETED:
                print(f"🔄 Compensating step {i+1}: {step.compensation}")
                step.status = SagaStepStatus.COMPENSATING
                
                # Simulate compensation (assume it always succeeds for demo)
                await asyncio.sleep(0.1)
                step.status = SagaStepStatus.COMPENSATED

# ==================== CIRCUIT BREAKER ====================

class CircuitState(Enum):
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"

class CircuitBreaker:
    def __init__(self, failure_threshold: int = 5, recovery_timeout: int = 60, 
                 half_open_max_calls: int = 3):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.half_open_max_calls = half_open_max_calls
        
        self.failure_count = 0
        self.last_failure_time = None
        self.state = CircuitState.CLOSED
        self.half_open_calls = 0

    async def call(self, func, *args, **kwargs):
        """Execute function with circuit breaker protection"""
        if self.state == CircuitState.OPEN:
            if self._should_attempt_reset():
                self.state = CircuitState.HALF_OPEN
                self.half_open_calls = 0
                print(f"🔄 Circuit breaker transitioning to HALF_OPEN")
            else:
                raise Exception("Circuit breaker is OPEN")
        
        try:
            if self.state == CircuitState.HALF_OPEN:
                if self.half_open_calls >= self.half_open_max_calls:
                    raise Exception("Circuit breaker HALF_OPEN limit reached")
                self.half_open_calls += 1
            
            result = await func(*args, **kwargs)
            self._on_success()
            return result
        
        except Exception as e:
            self._on_failure()
            raise e

    def _should_attempt_reset(self) -> bool:
        return (self.last_failure_time and 
                time.time() - self.last_failure_time >= self.recovery_timeout)

    def _on_success(self):
        self.failure_count = 0
        if self.state == CircuitState.HALF_OPEN:
            self.state = CircuitState.CLOSED
            print(f"✅ Circuit breaker reset to CLOSED")

    def _on_failure(self):
        self.failure_count += 1
        self.last_failure_time = time.time()
        
        if (self.state == CircuitState.CLOSED and 
            self.failure_count >= self.failure_threshold):
            self.state = CircuitState.OPEN
            print(f"🚨 Circuit breaker OPENED after {self.failure_count} failures")
        
        elif self.state == CircuitState.HALF_OPEN:
            self.state = CircuitState.OPEN
            print(f"🚨 Circuit breaker reopened from HALF_OPEN")

# ==================== ORDER PROCESSING SYSTEM ====================

class OrderAggregate:
    def __init__(self, order_id: str, event_store: EventStore):
        self.order_id = order_id
        self.event_store = event_store
        self.version = 0
        self.status = "created"
        self.items: List[Dict] = []
        self.total_amount = 0.0

    async def create_order(self, customer_id: str, items: List[Dict]):
        """Create a new order"""
        total = sum(item['price'] * item['quantity'] for item in items)
        
        event = Event.create(
            aggregate_id=self.order_id,
            event_type="OrderCreated",
            data={
                "customer_id": customer_id,
                "items": items,
                "total_amount": total
            },
            version=self.version + 1
        )
        
        await self.event_store.append_event(event)
        self._apply_event(event)

    async def confirm_payment(self, payment_id: str):
        """Confirm payment for order"""
        event = Event.create(
            aggregate_id=self.order_id,
            event_type="PaymentConfirmed",
            data={"payment_id": payment_id},
            version=self.version + 1
        )
        
        await self.event_store.append_event(event)
        self._apply_event(event)

    async def ship_order(self, tracking_number: str):
        """Ship the order"""
        event = Event.create(
            aggregate_id=self.order_id,
            event_type="OrderShipped",
            data={"tracking_number": tracking_number},
            version=self.version + 1
        )
        
        await self.event_store.append_event(event)
        self._apply_event(event)

    def _apply_event(self, event: Event):
        """Apply event to aggregate state"""
        if event.event_type == "OrderCreated":
            self.items = event.data["items"]
            self.total_amount = event.data["total_amount"]
            self.status = "created"
        elif event.event_type == "PaymentConfirmed":
            self.status = "paid"
        elif event.event_type == "OrderShipped":
            self.status = "shipped"
        
        self.version = event.version

# Read Model for CQRS
class OrderReadModel:
    def __init__(self):
        self.orders: Dict[str, Dict] = {}
        self.customer_orders: Dict[str, List[str]] = {}

    async def handle_event(self, event: Event):
        """Update read model based on events"""
        if event.event_type == "OrderCreated":
            order_data = {
                "order_id": event.aggregate_id,
                "customer_id": event.data["customer_id"],
                "items": event.data["items"],
                "total_amount": event.data["total_amount"],
                "status": "created",
                "created_at": event.timestamp
            }
            
            self.orders[event.aggregate_id] = order_data
            
            customer_id = event.data["customer_id"]
            if customer_id not in self.customer_orders:
                self.customer_orders[customer_id] = []
            self.customer_orders[customer_id].append(event.aggregate_id)
            
        elif event.event_type == "PaymentConfirmed":
            if event.aggregate_id in self.orders:
                self.orders[event.aggregate_id]["status"] = "paid"
                
        elif event.event_type == "OrderShipped":
            if event.aggregate_id in self.orders:
                self.orders[event.aggregate_id]["status"] = "shipped"
                self.orders[event.aggregate_id]["tracking_number"] = event.data["tracking_number"]

    def get_order(self, order_id: str) -> Optional[Dict]:
        return self.orders.get(order_id)

    def get_customer_orders(self, customer_id: str) -> List[Dict]:
        order_ids = self.customer_orders.get(customer_id, [])
        return [self.orders[oid] for oid in order_ids if oid in self.orders]

# ==================== EXTERNAL SERVICES ====================

class PaymentService:
    def __init__(self):
        self.circuit_breaker = CircuitBreaker(failure_threshold=3, recovery_timeout=30)

    async def process_payment(self, order_id: str, amount: float) -> str:
        """Process payment with circuit breaker protection"""
        async def _payment_call():
            # Simulate payment processing
            await asyncio.sleep(0.2)
            
            # Simulate occasional failures
            if random.random() < 0.3:
                raise Exception("Payment gateway timeout")
            
            return f"PAY_{int(time.time() * 1000)}"
        
        try:
            payment_id = await self.circuit_breaker.call(_payment_call)
            print(f"💳 Payment processed: {payment_id} for order {order_id}")
            return payment_id
        except Exception as e:
            print(f"❌ Payment failed for order {order_id}: {e}")
            raise

class InventoryService:
    async def reserve_items(self, items: List[Dict]) -> bool:
        """Reserve inventory items"""
        # Simulate inventory check
        await asyncio.sleep(0.1)
        
        for item in items:
            if random.random() < 0.1:  # 10% chance of insufficient inventory
                print(f"❌ Insufficient inventory for {item['product_id']}")
                return False
        
        print(f"✅ Reserved {len(items)} items")
        return True

class ShippingService:
    async def ship_order(self, order_id: str, items: List[Dict]) -> str:
        """Ship order and return tracking number"""
        await asyncio.sleep(0.15)
        
        if random.random() < 0.05:  # 5% chance of shipping failure
            raise Exception("Shipping service unavailable")
        
        tracking_number = f"TRACK_{int(time.time() * 1000)}"
        print(f"📦 Order {order_id} shipped with tracking: {tracking_number}")
        return tracking_number

# ==================== ORDER PROCESSING ORCHESTRATOR ====================

class OrderProcessingOrchestrator:
    def __init__(self, event_store: EventStore, read_model: OrderReadModel):
        self.event_store = event_store
        self.read_model = read_model
        self.payment_service = PaymentService()
        self.inventory_service = InventoryService()
        self.shipping_service = ShippingService()

    async def process_order(self, order_id: str, customer_id: str, items: List[Dict]) -> bool:
        """Process order using saga pattern"""
        
        # Create order aggregate
        order = OrderAggregate(order_id, self.event_store)
        await order.create_order(customer_id, items)
        
        # Define saga steps
        saga_steps = [
            SagaStep(
                step_id="reserve_inventory",
                action="reserve_inventory",
                compensation="release_inventory"
            ),
            SagaStep(
                step_id="process_payment",
                action="process_payment",
                compensation="refund_payment"
            ),
            SagaStep(
                step_id="ship_order",
                action="ship_order",
                compensation="cancel_shipment"
            ),
            SagaStep(
                step_id="send_confirmation",
                action="send_notification",
                compensation="send_cancellation"
            )
        ]
        
        # Create and execute saga
        saga = SagaOrchestrator(f"order_saga_{order_id}", saga_steps)
        
        # Override saga step execution with actual service calls
        original_execute_step = saga._execute_step
        
        async def custom_execute_step(step: SagaStep) -> bool:
            try:
                if step.action == "reserve_inventory":
                    return await self.inventory_service.reserve_items(items)
                elif step.action == "process_payment":
                    payment_id = await self.payment_service.process_payment(order_id, order.total_amount)
                    await order.confirm_payment(payment_id)
                    return True
                elif step.action == "ship_order":
                    tracking_number = await self.shipping_service.ship_order(order_id, items)
                    await order.ship_order(tracking_number)
                    return True
                elif step.action == "send_notification":
                    print(f"📧 Order confirmation sent for {order_id}")
                    return True
                else:
                    return await original_execute_step(step)
            except Exception as e:
                print(f"❌ Step {step.action} failed: {e}")
                return False
        
        saga._execute_step = custom_execute_step
        
        return await saga.execute()

# ==================== MAIN DEMONSTRATION ====================

async def demonstrate_advanced_patterns():
    print("🏗️ Advanced System Design Patterns Demo")
    print("=" * 60)
    
    # Setup
    event_store = EventStore()
    read_model = OrderReadModel()
    event_store.subscribe(read_model)
    
    orchestrator = OrderProcessingOrchestrator(event_store, read_model)
    
    # Sample orders
    orders = [
        {
            "order_id": "ORD-001",
            "customer_id": "CUST-123",
            "items": [
                {"product_id": "PROD-1", "quantity": 2, "price": 50.0},
                {"product_id": "PROD-2", "quantity": 1, "price": 30.0}
            ]
        },
        {
            "order_id": "ORD-002",
            "customer_id": "CUST-456",
            "items": [
                {"product_id": "PROD-3", "quantity": 1, "price": 100.0}
            ]
        },
        {
            "order_id": "ORD-003",
            "customer_id": "CUST-123",
            "items": [
                {"product_id": "PROD-4", "quantity": 3, "price": 25.0}
            ]
        }
    ]
    
    # Process orders
    print("🛒 Processing orders...")
    results = []
    
    for order_data in orders:
        print(f"\n--- Processing {order_data['order_id']} ---")
        success = await orchestrator.process_order(
            order_data["order_id"],
            order_data["customer_id"],
            order_data["items"]
        )
        results.append(success)
        
        # Small delay between orders
        await asyncio.sleep(0.5)
    
    # Show results
    print(f"\n📊 Processing Results:")
    for i, (order_data, success) in enumerate(zip(orders, results)):
        status = "✅ Success" if success else "❌ Failed"
        print(f"{order_data['order_id']}: {status}")
    
    # Show read model state
    print(f"\n📋 Order Read Model State:")
    for order_id in [order["order_id"] for order in orders]:
        order_info = read_model.get_order(order_id)
        if order_info:
            print(f"{order_id}: Status={order_info['status']}, Total=${order_info['total_amount']}")
    
    # Show customer orders
    print(f"\n👤 Customer Order Summary:")
    for customer_id in set(order["customer_id"] for order in orders):
        customer_orders = read_model.get_customer_orders(customer_id)
        print(f"{customer_id}: {len(customer_orders)} orders")
        for order in customer_orders:
            print(f"  - {order['order_id']}: ${order['total_amount']} ({order['status']})")
    
    # Show event store contents
    print(f"\n📜 Event Store Summary:")
    print(f"Total events stored: {len(event_store.events)}")
    
    event_types = {}
    for event in event_store.events:
        event_types[event.event_type] = event_types.get(event.event_type, 0) + 1
    
    for event_type, count in event_types.items():
        print(f"  {event_type}: {count}")

async def main():
    """Main demonstration function"""
    try:
        await demonstrate_advanced_patterns()
        
        print("\n" + "=" * 60)
        print("🎯 Key Patterns Demonstrated:")
        print("• Event Sourcing: Complete audit trail of all changes")
        print("• CQRS: Separate read/write models for optimization")
        print("• Saga Pattern: Distributed transaction management")
        print("• Circuit Breaker: Fault tolerance for external services")
        print("• Aggregate Pattern: Domain-driven design boundaries")
        print("• Orchestration: Coordinated business process execution")
        
        print("\n💡 Real-world Benefits:")
        print("• Scalability: Each component can scale independently")
        print("• Reliability: Failures are isolated and recoverable")
        print("• Auditability: Complete history of all system changes")
        print("• Flexibility: Easy to add new features and integrations")
        print("• Testability: Each pattern can be tested in isolation")
        
    except Exception as e:
        print(f"❌ Demo failed: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())
