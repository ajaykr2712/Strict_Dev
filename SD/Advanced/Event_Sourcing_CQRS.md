# Advanced System Design Concepts

## Table of Contents
1. [Event Sourcing Implementation](#event-sourcing-implementation)
2. [CQRS with Event Store](#cqrs-with-event-store)
3. [Distributed Consensus Algorithms](#distributed-consensus-algorithms)
4. [Service Mesh Architecture](#service-mesh-architecture)
5. [Chaos Engineering Practices](#chaos-engineering-practices)
6. [Edge Computing Patterns](#edge-computing-patterns)

---

## Event Sourcing Implementation

### Conceptual Overview
Event Sourcing ensures that all changes to application state are stored as a sequence of events. Instead of storing current state, we store the events that led to the current state.

### Benefits
- **Complete Audit Trail**: Every change is recorded
- **Temporal Queries**: Query state at any point in time
- **Event Replay**: Rebuild state from events
- **Debugging**: Understand how state was reached

### Implementation Example

```python
from datetime import datetime
from typing import List, Dict, Any
from abc import ABC, abstractmethod
import json

class Event(ABC):
    def __init__(self, aggregate_id: str, event_type: str, data: Dict[str, Any]):
        self.aggregate_id = aggregate_id
        self.event_type = event_type
        self.data = data
        self.timestamp = datetime.utcnow()
        self.version = 1

    def to_dict(self) -> Dict[str, Any]:
        return {
            'aggregate_id': self.aggregate_id,
            'event_type': self.event_type,
            'data': self.data,
            'timestamp': self.timestamp.isoformat(),
            'version': self.version
        }

class BankAccountCreated(Event):
    def __init__(self, account_id: str, initial_balance: float):
        super().__init__(account_id, 'AccountCreated', {
            'initial_balance': initial_balance
        })

class MoneyDeposited(Event):
    def __init__(self, account_id: str, amount: float):
        super().__init__(account_id, 'MoneyDeposited', {
            'amount': amount
        })

class MoneyWithdrawn(Event):
    def __init__(self, account_id: str, amount: float):
        super().__init__(account_id, 'MoneyWithdrawn', {
            'amount': amount
        })

class EventStore:
    def __init__(self):
        self.events: List[Event] = []
        self.snapshots: Dict[str, Any] = {}

    def append_event(self, event: Event):
        """Append an event to the event store"""
        self.events.append(event)
        print(f"📝 Event stored: {event.event_type} for {event.aggregate_id}")

    def get_events(self, aggregate_id: str, from_version: int = 0) -> List[Event]:
        """Get events for a specific aggregate"""
        return [e for e in self.events 
                if e.aggregate_id == aggregate_id and e.version > from_version]

    def create_snapshot(self, aggregate_id: str, state: Dict[str, Any], version: int):
        """Create a snapshot of current state"""
        self.snapshots[aggregate_id] = {
            'state': state,
            'version': version,
            'timestamp': datetime.utcnow()
        }

class BankAccount:
    def __init__(self, account_id: str):
        self.account_id = account_id
        self.balance = 0.0
        self.version = 0

    def apply_event(self, event: Event):
        """Apply an event to change state"""
        if event.event_type == 'AccountCreated':
            self.balance = event.data['initial_balance']
        elif event.event_type == 'MoneyDeposited':
            self.balance += event.data['amount']
        elif event.event_type == 'MoneyWithdrawn':
            self.balance -= event.data['amount']
        
        self.version += 1

    def get_state(self) -> Dict[str, Any]:
        return {
            'account_id': self.account_id,
            'balance': self.balance,
            'version': self.version
        }

class BankAccountAggregate:
    def __init__(self, account_id: str, event_store: EventStore):
        self.account_id = account_id
        self.event_store = event_store
        self.account = BankAccount(account_id)
        self._load_from_history()

    def _load_from_history(self):
        """Load aggregate state from event history"""
        events = self.event_store.get_events(self.account_id)
        for event in events:
            self.account.apply_event(event)

    def create_account(self, initial_balance: float):
        """Create a new bank account"""
        if self.account.version > 0:
            raise ValueError("Account already exists")
        
        event = BankAccountCreated(self.account_id, initial_balance)
        self.event_store.append_event(event)
        self.account.apply_event(event)

    def deposit(self, amount: float):
        """Deposit money to account"""
        if amount <= 0:
            raise ValueError("Deposit amount must be positive")
        
        event = MoneyDeposited(self.account_id, amount)
        self.event_store.append_event(event)
        self.account.apply_event(event)

    def withdraw(self, amount: float):
        """Withdraw money from account"""
        if amount <= 0:
            raise ValueError("Withdrawal amount must be positive")
        
        if self.account.balance < amount:
            raise ValueError("Insufficient funds")
        
        event = MoneyWithdrawn(self.account_id, amount)
        self.event_store.append_event(event)
        self.account.apply_event(event)

    def get_balance(self) -> float:
        return self.account.balance

# Demonstration
def demonstrate_event_sourcing():
    print("🏦 Event Sourcing Bank Account Demo")
    print("=" * 50)
    
    # Create event store
    event_store = EventStore()
    
    # Create bank account aggregate
    account = BankAccountAggregate("ACC-123", event_store)
    
    # Perform operations
    account.create_account(1000.0)
    print(f"💰 Initial balance: ${account.get_balance()}")
    
    account.deposit(500.0)
    print(f"💰 After deposit: ${account.get_balance()}")
    
    account.withdraw(200.0)
    print(f"💰 After withdrawal: ${account.get_balance()}")
    
    # Show event history
    print("\n📜 Event History:")
    events = event_store.get_events("ACC-123")
    for i, event in enumerate(events, 1):
        print(f"{i}. {event.event_type}: {event.data}")
    
    # Demonstrate state reconstruction
    print("\n🔄 Reconstructing account state from events:")
    new_account = BankAccount("ACC-123")
    for event in events:
        new_account.apply_event(event)
        print(f"After {event.event_type}: ${new_account.balance}")

if __name__ == "__main__":
    demonstrate_event_sourcing()
```

---

## CQRS with Event Store

### Command Query Responsibility Segregation

CQRS separates read and write operations into different models, optimizing each for their specific use case.

```python
from typing import Dict, List, Any
from dataclasses import dataclass
from abc import ABC, abstractmethod

@dataclass
class Command(ABC):
    """Base class for all commands"""
    pass

@dataclass
class CreateOrderCommand(Command):
    order_id: str
    customer_id: str
    items: List[Dict[str, Any]]
    total_amount: float

@dataclass
class AddOrderItemCommand(Command):
    order_id: str
    item_id: str
    quantity: int
    price: float

@dataclass
class Query(ABC):
    """Base class for all queries"""
    pass

@dataclass
class GetOrderQuery(Query):
    order_id: str

@dataclass
class GetCustomerOrdersQuery(Query):
    customer_id: str

class CommandHandler(ABC):
    @abstractmethod
    def handle(self, command: Command):
        pass

class QueryHandler(ABC):
    @abstractmethod
    def handle(self, query: Query):
        pass

# Write Side - Command Model
class OrderCommandHandler(CommandHandler):
    def __init__(self, event_store: EventStore):
        self.event_store = event_store

    def handle(self, command: Command):
        if isinstance(command, CreateOrderCommand):
            self._handle_create_order(command)
        elif isinstance(command, AddOrderItemCommand):
            self._handle_add_item(command)

    def _handle_create_order(self, command: CreateOrderCommand):
        # Validate business rules
        if command.total_amount <= 0:
            raise ValueError("Order total must be positive")
        
        # Create and store event
        event = OrderCreated(
            command.order_id,
            command.customer_id,
            command.items,
            command.total_amount
        )
        self.event_store.append_event(event)

    def _handle_add_item(self, command: AddOrderItemCommand):
        event = OrderItemAdded(
            command.order_id,
            command.item_id,
            command.quantity,
            command.price
        )
        self.event_store.append_event(event)

# Read Side - Query Model
class OrderReadModel:
    def __init__(self):
        self.orders: Dict[str, Dict[str, Any]] = {}
        self.customer_orders: Dict[str, List[str]] = {}

    def update_from_event(self, event: Event):
        """Update read model based on events"""
        if event.event_type == 'OrderCreated':
            self._handle_order_created(event)
        elif event.event_type == 'OrderItemAdded':
            self._handle_item_added(event)

    def _handle_order_created(self, event: Event):
        order_id = event.aggregate_id
        customer_id = event.data['customer_id']
        
        self.orders[order_id] = {
            'order_id': order_id,
            'customer_id': customer_id,
            'items': event.data['items'],
            'total_amount': event.data['total_amount'],
            'status': 'created'
        }
        
        if customer_id not in self.customer_orders:
            self.customer_orders[customer_id] = []
        self.customer_orders[customer_id].append(order_id)

    def _handle_item_added(self, event: Event):
        order_id = event.aggregate_id
        if order_id in self.orders:
            new_item = {
                'item_id': event.data['item_id'],
                'quantity': event.data['quantity'],
                'price': event.data['price']
            }
            self.orders[order_id]['items'].append(new_item)
            self.orders[order_id]['total_amount'] += event.data['price'] * event.data['quantity']

class OrderQueryHandler(QueryHandler):
    def __init__(self, read_model: OrderReadModel):
        self.read_model = read_model

    def handle(self, query: Query):
        if isinstance(query, GetOrderQuery):
            return self._handle_get_order(query)
        elif isinstance(query, GetCustomerOrdersQuery):
            return self._handle_get_customer_orders(query)

    def _handle_get_order(self, query: GetOrderQuery):
        return self.read_model.orders.get(query.order_id)

    def _handle_get_customer_orders(self, query: GetCustomerOrdersQuery):
        order_ids = self.read_model.customer_orders.get(query.customer_id, [])
        return [self.read_model.orders[oid] for oid in order_ids if oid in self.read_model.orders]

# Event definitions for Order domain
class OrderCreated(Event):
    def __init__(self, order_id: str, customer_id: str, items: List[Dict], total_amount: float):
        super().__init__(order_id, 'OrderCreated', {
            'customer_id': customer_id,
            'items': items,
            'total_amount': total_amount
        })

class OrderItemAdded(Event):
    def __init__(self, order_id: str, item_id: str, quantity: int, price: float):
        super().__init__(order_id, 'OrderItemAdded', {
            'item_id': item_id,
            'quantity': quantity,
            'price': price
        })

def demonstrate_cqrs():
    print("🛒 CQRS Order Management Demo")
    print("=" * 50)
    
    # Setup
    event_store = EventStore()
    read_model = OrderReadModel()
    command_handler = OrderCommandHandler(event_store)
    query_handler = OrderQueryHandler(read_model)
    
    # Execute commands
    create_command = CreateOrderCommand(
        order_id="ORD-001",
        customer_id="CUST-123",
        items=[{"item_id": "ITEM-1", "quantity": 2, "price": 50.0}],
        total_amount=100.0
    )
    command_handler.handle(create_command)
    
    # Update read model from events
    for event in event_store.get_events("ORD-001"):
        read_model.update_from_event(event)
    
    # Execute queries
    order_query = GetOrderQuery("ORD-001")
    order = query_handler.handle(order_query)
    print(f"📋 Order found: {order}")
    
    customer_query = GetCustomerOrdersQuery("CUST-123")
    customer_orders = query_handler.handle(customer_query)
    print(f"🛍️ Customer orders: {len(customer_orders)} orders found")

if __name__ == "__main__":
    demonstrate_cqrs()
```

---

## Key Benefits of Advanced Patterns

### Event Sourcing
1. **Audit Trail**: Complete history of all changes
2. **Debugging**: Understand how current state was reached
3. **Temporal Queries**: Query state at any point in time
4. **Resilience**: Rebuild state from events

### CQRS
1. **Performance**: Optimize reads and writes separately
2. **Scalability**: Scale read and write sides independently
3. **Complexity Management**: Separate complex read logic from write logic
4. **Multiple Views**: Create different projections for different needs

### When to Use
- **High-volume systems** with different read/write patterns
- **Audit requirements** with complete traceability
- **Complex business logic** that benefits from event modeling
- **Multiple views** of the same data

### Trade-offs
- **Complexity**: More moving parts and concepts
- **Eventual Consistency**: Read side may lag behind writes
- **Storage**: Events require more storage than state
- **Learning Curve**: Team needs to understand event-driven thinking

---

## Real-World Applications

### Financial Services
- Transaction processing with complete audit trail
- Regulatory compliance and reporting
- Risk management and fraud detection

### E-commerce
- Order processing with complex workflows
- Inventory management across multiple channels
- Customer behavior analytics

### IoT and Monitoring
- Sensor data processing and analysis
- System state reconstruction from events
- Real-time dashboards and alerting

---

*These advanced patterns represent the cutting edge of system design. Master them to build truly scalable and maintainable systems!*
