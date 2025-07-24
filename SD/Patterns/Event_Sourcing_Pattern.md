# Event Sourcing Pattern

## Overview

Event Sourcing is a design pattern where state changes are stored as a sequence of events. Instead of storing the current state of an entity, Event Sourcing stores all the events that led to the current state. The current state is derived by replaying these events.

## Key Concepts

### 1. Events as First-Class Citizens
- Events represent facts that happened in the past
- Events are immutable once stored
- Events contain all information needed to recreate state
- Events are the source of truth

### 2. Event Store
- Append-only database of events
- Events are never modified or deleted
- Provides complete audit trail
- Enables temporal queries

### 3. Event Streams
- Ordered sequence of events for an aggregate
- Each stream represents the history of one entity
- Events are ordered by sequence number or timestamp

### 4. Projections
- Read models derived from events
- Can be rebuilt from event history
- Multiple projections for different use cases

## Implementation Example

```python
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
from datetime import datetime
import json
import uuid

# Base Event
@dataclass
class DomainEvent:
    event_id: str
    aggregate_id: str
    event_type: str
    event_data: Dict[str, Any]
    event_version: int
    timestamp: datetime
    
    def __post_init__(self):
        if not self.event_id:
            self.event_id = str(uuid.uuid4())
        if not self.timestamp:
            self.timestamp = datetime.now()

# Specific Events
@dataclass
class AccountCreated(DomainEvent):
    def __init__(self, aggregate_id: str, account_holder: str, initial_balance: float):
        super().__init__(
            event_id=str(uuid.uuid4()),
            aggregate_id=aggregate_id,
            event_type="AccountCreated",
            event_data={
                "account_holder": account_holder,
                "initial_balance": initial_balance
            },
            event_version=1,
            timestamp=datetime.now()
        )

@dataclass
class MoneyDeposited(DomainEvent):
    def __init__(self, aggregate_id: str, amount: float, description: str):
        super().__init__(
            event_id=str(uuid.uuid4()),
            aggregate_id=aggregate_id,
            event_type="MoneyDeposited",
            event_data={
                "amount": amount,
                "description": description
            },
            event_version=1,
            timestamp=datetime.now()
        )

@dataclass
class MoneyWithdrawn(DomainEvent):
    def __init__(self, aggregate_id: str, amount: float, description: str):
        super().__init__(
            event_id=str(uuid.uuid4()),
            aggregate_id=aggregate_id,
            event_type="MoneyWithdrawn",
            event_data={
                "amount": amount,
                "description": description
            },
            event_version=1,
            timestamp=datetime.now()
        )

# Aggregate Root
class BankAccount:
    def __init__(self, account_id: str):
        self.account_id = account_id
        self.account_holder = ""
        self.balance = 0.0
        self.is_active = False
        self.version = 0
        self.uncommitted_events: List[DomainEvent] = []
    
    def create_account(self, account_holder: str, initial_balance: float):
        if self.is_active:
            raise ValueError("Account already exists")
        
        if initial_balance < 0:
            raise ValueError("Initial balance cannot be negative")
        
        event = AccountCreated(self.account_id, account_holder, initial_balance)
        self._apply_event(event)
        self.uncommitted_events.append(event)
    
    def deposit(self, amount: float, description: str = ""):
        if not self.is_active:
            raise ValueError("Account is not active")
        
        if amount <= 0:
            raise ValueError("Deposit amount must be positive")
        
        event = MoneyDeposited(self.account_id, amount, description)
        self._apply_event(event)
        self.uncommitted_events.append(event)
    
    def withdraw(self, amount: float, description: str = ""):
        if not self.is_active:
            raise ValueError("Account is not active")
        
        if amount <= 0:
            raise ValueError("Withdrawal amount must be positive")
        
        if amount > self.balance:
            raise ValueError("Insufficient funds")
        
        event = MoneyWithdrawn(self.account_id, amount, description)
        self._apply_event(event)
        self.uncommitted_events.append(event)
    
    def _apply_event(self, event: DomainEvent):
        """Apply event to current state"""
        if event.event_type == "AccountCreated":
            self.account_holder = event.event_data["account_holder"]
            self.balance = event.event_data["initial_balance"]
            self.is_active = True
        
        elif event.event_type == "MoneyDeposited":
            self.balance += event.event_data["amount"]
        
        elif event.event_type == "MoneyWithdrawn":
            self.balance -= event.event_data["amount"]
        
        self.version += 1
    
    def load_from_history(self, events: List[DomainEvent]):
        """Rebuild state from event history"""
        for event in sorted(events, key=lambda e: e.timestamp):
            self._apply_event(event)
    
    def get_uncommitted_events(self) -> List[DomainEvent]:
        return self.uncommitted_events.copy()
    
    def mark_events_as_committed(self):
        self.uncommitted_events.clear()

# Event Store Interface
class EventStore(ABC):
    @abstractmethod
    def append_events(self, aggregate_id: str, events: List[DomainEvent], expected_version: int):
        pass
    
    @abstractmethod
    def get_events(self, aggregate_id: str, from_version: int = 0) -> List[DomainEvent]:
        pass
    
    @abstractmethod
    def get_all_events(self, from_timestamp: datetime = None) -> List[DomainEvent]:
        pass

# In-Memory Event Store Implementation
class InMemoryEventStore(EventStore):
    def __init__(self):
        self.events: Dict[str, List[DomainEvent]] = {}
        self.all_events: List[DomainEvent] = []
    
    def append_events(self, aggregate_id: str, events: List[DomainEvent], expected_version: int):
        if aggregate_id not in self.events:
            self.events[aggregate_id] = []
        
        current_version = len(self.events[aggregate_id])
        if current_version != expected_version:
            raise ValueError(f"Concurrency conflict. Expected version {expected_version}, "
                           f"but current version is {current_version}")
        
        self.events[aggregate_id].extend(events)
        self.all_events.extend(events)
    
    def get_events(self, aggregate_id: str, from_version: int = 0) -> List[DomainEvent]:
        if aggregate_id not in self.events:
            return []
        
        return self.events[aggregate_id][from_version:]
    
    def get_all_events(self, from_timestamp: datetime = None) -> List[DomainEvent]:
        if from_timestamp is None:
            return self.all_events.copy()
        
        return [event for event in self.all_events if event.timestamp >= from_timestamp]

# Repository
class BankAccountRepository:
    def __init__(self, event_store: EventStore):
        self.event_store = event_store
    
    def save(self, account: BankAccount):
        uncommitted_events = account.get_uncommitted_events()
        if uncommitted_events:
            expected_version = account.version - len(uncommitted_events)
            self.event_store.append_events(account.account_id, uncommitted_events, expected_version)
            account.mark_events_as_committed()
    
    def get_by_id(self, account_id: str) -> Optional[BankAccount]:
        events = self.event_store.get_events(account_id)
        if not events:
            return None
        
        account = BankAccount(account_id)
        account.load_from_history(events)
        return account

# Projections
class AccountBalanceProjection:
    def __init__(self):
        self.balances: Dict[str, float] = {}
    
    def handle_event(self, event: DomainEvent):
        if event.event_type == "AccountCreated":
            self.balances[event.aggregate_id] = event.event_data["initial_balance"]
        
        elif event.event_type == "MoneyDeposited":
            if event.aggregate_id in self.balances:
                self.balances[event.aggregate_id] += event.event_data["amount"]
        
        elif event.event_type == "MoneyWithdrawn":
            if event.aggregate_id in self.balances:
                self.balances[event.aggregate_id] -= event.event_data["amount"]
    
    def get_balance(self, account_id: str) -> float:
        return self.balances.get(account_id, 0.0)
    
    def get_all_balances(self) -> Dict[str, float]:
        return self.balances.copy()

class TransactionHistoryProjection:
    def __init__(self):
        self.transactions: Dict[str, List[Dict]] = {}
    
    def handle_event(self, event: DomainEvent):
        if event.aggregate_id not in self.transactions:
            self.transactions[event.aggregate_id] = []
        
        if event.event_type in ["MoneyDeposited", "MoneyWithdrawn"]:
            transaction = {
                "type": event.event_type,
                "amount": event.event_data["amount"],
                "description": event.event_data.get("description", ""),
                "timestamp": event.timestamp.isoformat()
            }
            self.transactions[event.aggregate_id].append(transaction)
    
    def get_transactions(self, account_id: str) -> List[Dict]:
        return self.transactions.get(account_id, [])

# Event Bus for Projections
class EventBus:
    def __init__(self):
        self.projections: List = []
    
    def register_projection(self, projection):
        self.projections.append(projection)
    
    def publish_events(self, events: List[DomainEvent]):
        for event in events:
            for projection in self.projections:
                projection.handle_event(event)
```

## Advantages

### 1. Complete Audit Trail
- Every change is recorded
- Perfect audit and compliance support
- Debugging capabilities
- Temporal queries possible

### 2. Event Replay
- Rebuild state from events
- Test different scenarios
- Bug investigation
- Data migration

### 3. Performance
- Append-only writes are fast
- Read models can be optimized
- Horizontal scaling possible

### 4. Integration
- Events can trigger side effects
- Easy integration with external systems
- Publish events to message queues

## Best Practices

### 1. Event Design
```python
# Good: Specific, immutable events
class OrderPlaced(DomainEvent):
    def __init__(self, order_id: str, customer_id: str, items: List[Dict]):
        # Event data should be complete and specific

# Avoid: Generic events
class OrderUpdated(DomainEvent):
    def __init__(self, order_id: str, changes: Dict):
        # Too generic, loses business meaning
```

### 2. Versioning
```python
class EventVersioning:
    def handle_event(self, event: DomainEvent):
        if event.event_version == 1:
            return self._handle_v1(event)
        elif event.event_version == 2:
            return self._handle_v2(event)
        else:
            raise ValueError(f"Unsupported event version: {event.event_version}")
```

### 3. Snapshots
```python
class SnapshotStore:
    def save_snapshot(self, aggregate_id: str, snapshot: Dict, version: int):
        # Save periodic snapshots for performance
        pass
    
    def get_snapshot(self, aggregate_id: str) -> Optional[Dict]:
        # Load from snapshot + events since snapshot
        pass
```

## Common Patterns

### 1. CQRS Integration
- Events update read models
- Commands generate events
- Queries use projections

### 2. Saga Pattern
- Events trigger saga steps
- Compensating actions
- Distributed transactions

### 3. Event Sourcing + Snapshots
- Performance optimization
- Reduce replay time
- Still maintain event history

## When to Use Event Sourcing

### Good Fit
- Audit requirements
- Complex business domains
- Temporal queries needed
- Integration heavy systems

### Poor Fit
- Simple CRUD applications
- Immediate consistency requirements
- Small, simple domains
- Resource-constrained environments

## Conclusion

Event Sourcing provides a powerful way to model state changes as events, offering complete audit trails, debugging capabilities, and integration opportunities. While it adds complexity, it's invaluable for domains where understanding "what happened" is as important as "what is the current state".
