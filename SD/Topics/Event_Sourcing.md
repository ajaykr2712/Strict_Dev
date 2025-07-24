# Event Sourcing

## Overview
Event Sourcing is an architectural pattern where changes to application state are stored as a sequence of events. Instead of storing just the current state of data, Event Sourcing stores all changes as events, allowing you to rebuild current state by replaying these events and provides a complete audit trail of all changes.

## Key Concepts
- **Events**: Immutable records of state changes
- **Event Store**: Append-only database for events
- **Event Replay**: Reconstructing state from events
- **Snapshots**: Periodic state captures for performance

## Advanced Topics

### 1. Event Design Principles

#### Event Structure
```python
from dataclasses import dataclass
from datetime import datetime
from typing import Optional
import uuid

@dataclass
class DomainEvent:
    """Base class for all domain events"""
    event_id: str
    aggregate_id: str
    event_type: str
    event_version: int
    occurred_at: datetime
    correlation_id: Optional[str] = None
    causation_id: Optional[str] = None
    
    def __post_init__(self):
        if not self.event_id:
            self.event_id = str(uuid.uuid4())
        if not self.occurred_at:
            self.occurred_at = datetime.utcnow()

@dataclass
class AccountCreatedEvent(DomainEvent):
    account_id: str
    account_holder: str
    initial_balance: float
    account_type: str
    
    def __post_init__(self):
        super().__post_init__()
        self.event_type = "AccountCreated"

@dataclass
class MoneyDepositedEvent(DomainEvent):
    account_id: str
    amount: float
    deposit_method: str
    reference: str
    
    def __post_init__(self):
        super().__post_init__()
        self.event_type = "MoneyDeposited"

@dataclass
class MoneyWithdrawnEvent(DomainEvent):
    account_id: str
    amount: float
    withdrawal_method: str
    reference: str
    
    def __post_init__(self):
        super().__post_init__()
        self.event_type = "MoneyWithdrawn"
```

### 2. Event Store Implementation

#### Event Store Interface
```python
from abc import ABC, abstractmethod
from typing import List, Optional

class EventStore(ABC):
    @abstractmethod
    def save_events(self, aggregate_id: str, events: List[DomainEvent], expected_version: int):
        """Save events for an aggregate"""
        pass
    
    @abstractmethod
    def get_events(self, aggregate_id: str, from_version: int = 0) -> List[DomainEvent]:
        """Get events for an aggregate"""
        pass
    
    @abstractmethod
    def get_all_events(self, from_position: int = 0) -> List[DomainEvent]:
        """Get all events from a position"""
        pass

class InMemoryEventStore(EventStore):
    def __init__(self):
        self.events = {}  # aggregate_id -> List[events]
        self.global_events = []  # All events in order
        self.lock = threading.Lock()
    
    def save_events(self, aggregate_id: str, events: List[DomainEvent], expected_version: int):
        with self.lock:
            current_events = self.events.get(aggregate_id, [])
            
            # Optimistic concurrency check
            if len(current_events) != expected_version:
                raise ConcurrencyError(
                    f"Expected version {expected_version}, but current version is {len(current_events)}"
                )
            
            # Assign version numbers
            for i, event in enumerate(events):
                event.event_version = expected_version + i + 1
            
            # Store events
            if aggregate_id not in self.events:
                self.events[aggregate_id] = []
            
            self.events[aggregate_id].extend(events)
            self.global_events.extend(events)
    
    def get_events(self, aggregate_id: str, from_version: int = 0) -> List[DomainEvent]:
        events = self.events.get(aggregate_id, [])
        return [e for e in events if e.event_version > from_version]
    
    def get_all_events(self, from_position: int = 0) -> List[DomainEvent]:
        return self.global_events[from_position:]

class SqlEventStore(EventStore):
    def __init__(self, connection):
        self.connection = connection
        self._create_tables()
    
    def _create_tables(self):
        """Create event store tables"""
        cursor = self.connection.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS events (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_id VARCHAR(36) UNIQUE NOT NULL,
                aggregate_id VARCHAR(36) NOT NULL,
                event_type VARCHAR(100) NOT NULL,
                event_version INT NOT NULL,
                event_data JSON NOT NULL,
                occurred_at TIMESTAMP NOT NULL,
                correlation_id VARCHAR(36),
                causation_id VARCHAR(36),
                INDEX idx_aggregate (aggregate_id, event_version),
                INDEX idx_type_time (event_type, occurred_at)
            )
        """)
        self.connection.commit()
    
    def save_events(self, aggregate_id: str, events: List[DomainEvent], expected_version: int):
        cursor = self.connection.cursor()
        
        try:
            # Check current version
            cursor.execute(
                "SELECT MAX(event_version) FROM events WHERE aggregate_id = %s",
                (aggregate_id,)
            )
            result = cursor.fetchone()
            current_version = result[0] if result[0] is not None else 0
            
            if current_version != expected_version:
                raise ConcurrencyError(
                    f"Expected version {expected_version}, but current version is {current_version}"
                )
            
            # Insert events
            for i, event in enumerate(events):
                event.event_version = expected_version + i + 1
                event_data = self._serialize_event(event)
                
                cursor.execute("""
                    INSERT INTO events 
                    (event_id, aggregate_id, event_type, event_version, event_data, 
                     occurred_at, correlation_id, causation_id)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """, (
                    event.event_id,
                    event.aggregate_id,
                    event.event_type,
                    event.event_version,
                    event_data,
                    event.occurred_at,
                    event.correlation_id,
                    event.causation_id
                ))
            
            self.connection.commit()
            
        except Exception as e:
            self.connection.rollback()
            raise e
    
    def get_events(self, aggregate_id: str, from_version: int = 0) -> List[DomainEvent]:
        cursor = self.connection.cursor()
        cursor.execute("""
            SELECT event_data FROM events 
            WHERE aggregate_id = %s AND event_version > %s 
            ORDER BY event_version
        """, (aggregate_id, from_version))
        
        events = []
        for (event_data,) in cursor.fetchall():
            event = self._deserialize_event(event_data)
            events.append(event)
        
        return events
```

### 3. Aggregate Root with Event Sourcing

#### Event-Sourced Aggregate
```python
class BankAccount:
    def __init__(self, account_id: str = None):
        self.account_id = account_id
        self.account_holder = None
        self.balance = 0.0
        self.account_type = None
        self.is_active = False
        self.version = 0
        self.uncommitted_events = []
    
    @classmethod
    def create(cls, account_id: str, account_holder: str, initial_balance: float, account_type: str):
        """Create new bank account"""
        account = cls(account_id)
        
        event = AccountCreatedEvent(
            event_id=str(uuid.uuid4()),
            aggregate_id=account_id,
            account_id=account_id,
            account_holder=account_holder,
            initial_balance=initial_balance,
            account_type=account_type,
            event_version=0,
            occurred_at=datetime.utcnow()
        )
        
        account._apply_event(event)
        account.uncommitted_events.append(event)
        
        return account
    
    def deposit(self, amount: float, deposit_method: str, reference: str):
        """Deposit money to account"""
        if not self.is_active:
            raise AccountNotActiveError("Account is not active")
        
        if amount <= 0:
            raise InvalidAmountError("Deposit amount must be positive")
        
        event = MoneyDepositedEvent(
            event_id=str(uuid.uuid4()),
            aggregate_id=self.account_id,
            account_id=self.account_id,
            amount=amount,
            deposit_method=deposit_method,
            reference=reference,
            event_version=0,  # Will be set by event store
            occurred_at=datetime.utcnow()
        )
        
        self._apply_event(event)
        self.uncommitted_events.append(event)
    
    def withdraw(self, amount: float, withdrawal_method: str, reference: str):
        """Withdraw money from account"""
        if not self.is_active:
            raise AccountNotActiveError("Account is not active")
        
        if amount <= 0:
            raise InvalidAmountError("Withdrawal amount must be positive")
        
        if self.balance < amount:
            raise InsufficientFundsError("Insufficient funds")
        
        event = MoneyWithdrawnEvent(
            event_id=str(uuid.uuid4()),
            aggregate_id=self.account_id,
            account_id=self.account_id,
            amount=amount,
            withdrawal_method=withdrawal_method,
            reference=reference,
            event_version=0,
            occurred_at=datetime.utcnow()
        )
        
        self._apply_event(event)
        self.uncommitted_events.append(event)
    
    def _apply_event(self, event: DomainEvent):
        """Apply event to update aggregate state"""
        if isinstance(event, AccountCreatedEvent):
            self.account_id = event.account_id
            self.account_holder = event.account_holder
            self.balance = event.initial_balance
            self.account_type = event.account_type
            self.is_active = True
        
        elif isinstance(event, MoneyDepositedEvent):
            self.balance += event.amount
        
        elif isinstance(event, MoneyWithdrawnEvent):
            self.balance -= event.amount
        
        self.version = event.event_version
    
    def get_uncommitted_events(self) -> List[DomainEvent]:
        """Get events that haven't been persisted"""
        return self.uncommitted_events.copy()
    
    def mark_events_as_committed(self):
        """Mark events as committed after persistence"""
        self.uncommitted_events.clear()
    
    @classmethod
    def from_events(cls, events: List[DomainEvent]):
        """Reconstruct aggregate from events"""
        if not events:
            return None
        
        account = cls()
        for event in events:
            account._apply_event(event)
        
        return account
```

### 4. Repository Pattern with Event Sourcing

#### Event-Sourced Repository
```python
class EventSourcedBankAccountRepository:
    def __init__(self, event_store: EventStore):
        self.event_store = event_store
    
    def save(self, account: BankAccount):
        """Save account by storing events"""
        events = account.get_uncommitted_events()
        if events:
            self.event_store.save_events(account.account_id, events, account.version - len(events))
            account.mark_events_as_committed()
    
    def get_by_id(self, account_id: str) -> Optional[BankAccount]:
        """Get account by replaying events"""
        events = self.event_store.get_events(account_id)
        if not events:
            return None
        
        return BankAccount.from_events(events)
    
    def get_version(self, account_id: str) -> int:
        """Get current version of aggregate"""
        events = self.event_store.get_events(account_id)
        return len(events)
```

### 5. Snapshots for Performance

#### Snapshot Implementation
```python
@dataclass
class Snapshot:
    aggregate_id: str
    aggregate_type: str
    version: int
    data: dict
    created_at: datetime

class SnapshotStore:
    def __init__(self):
        self.snapshots = {}  # aggregate_id -> Snapshot
    
    def save_snapshot(self, aggregate_id: str, aggregate, version: int):
        """Save aggregate snapshot"""
        snapshot = Snapshot(
            aggregate_id=aggregate_id,
            aggregate_type=type(aggregate).__name__,
            version=version,
            data=self._serialize_aggregate(aggregate),
            created_at=datetime.utcnow()
        )
        
        self.snapshots[aggregate_id] = snapshot
    
    def get_snapshot(self, aggregate_id: str) -> Optional[Snapshot]:
        """Get latest snapshot for aggregate"""
        return self.snapshots.get(aggregate_id)
    
    def _serialize_aggregate(self, aggregate) -> dict:
        """Serialize aggregate to dictionary"""
        if isinstance(aggregate, BankAccount):
            return {
                'account_id': aggregate.account_id,
                'account_holder': aggregate.account_holder,
                'balance': aggregate.balance,
                'account_type': aggregate.account_type,
                'is_active': aggregate.is_active,
                'version': aggregate.version
            }
        
        raise ValueError(f"Unknown aggregate type: {type(aggregate)}")

class SnapshotBankAccountRepository(EventSourcedBankAccountRepository):
    def __init__(self, event_store: EventStore, snapshot_store: SnapshotStore, snapshot_frequency: int = 10):
        super().__init__(event_store)
        self.snapshot_store = snapshot_store
        self.snapshot_frequency = snapshot_frequency
    
    def save(self, account: BankAccount):
        """Save account and create snapshot if needed"""
        super().save(account)
        
        # Create snapshot every N events
        if account.version % self.snapshot_frequency == 0:
            self.snapshot_store.save_snapshot(account.account_id, account, account.version)
    
    def get_by_id(self, account_id: str) -> Optional[BankAccount]:
        """Get account using snapshot + events"""
        # Try to get latest snapshot
        snapshot = self.snapshot_store.get_snapshot(account_id)
        
        if snapshot:
            # Reconstruct from snapshot
            account = self._deserialize_aggregate(snapshot.data)
            
            # Apply events since snapshot
            events = self.event_store.get_events(account_id, snapshot.version)
            for event in events:
                account._apply_event(event)
            
            return account
        else:
            # Fall back to full event replay
            return super().get_by_id(account_id)
```

### 6. Event Projections and Read Models

#### Projection Handler
```python
class AccountProjectionHandler:
    def __init__(self, read_db):
        self.read_db = read_db
    
    def handle_account_created(self, event: AccountCreatedEvent):
        """Create read model when account is created"""
        self.read_db.execute("""
            INSERT INTO account_summary 
            (account_id, account_holder, balance, account_type, is_active, created_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (
            event.account_id,
            event.account_holder,
            event.initial_balance,
            event.account_type,
            True,
            event.occurred_at
        ))
    
    def handle_money_deposited(self, event: MoneyDepositedEvent):
        """Update balance when money is deposited"""
        self.read_db.execute("""
            UPDATE account_summary 
            SET balance = balance + %s, last_transaction_at = %s
            WHERE account_id = %s
        """, (event.amount, event.occurred_at, event.account_id))
        
        # Record transaction history
        self.read_db.execute("""
            INSERT INTO transaction_history 
            (account_id, transaction_type, amount, method, reference, occurred_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (
            event.account_id,
            'deposit',
            event.amount,
            event.deposit_method,
            event.reference,
            event.occurred_at
        ))
    
    def handle_money_withdrawn(self, event: MoneyWithdrawnEvent):
        """Update balance when money is withdrawn"""
        self.read_db.execute("""
            UPDATE account_summary 
            SET balance = balance - %s, last_transaction_at = %s
            WHERE account_id = %s
        """, (event.amount, event.occurred_at, event.account_id))
        
        # Record transaction history
        self.read_db.execute("""
            INSERT INTO transaction_history 
            (account_id, transaction_type, amount, method, reference, occurred_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (
            event.account_id,
            'withdrawal',
            event.amount,
            event.withdrawal_method,
            event.reference,
            event.occurred_at
        ))
```

### 7. Event Replay and Migration

#### Event Replay Service
```python
class EventReplayService:
    def __init__(self, event_store: EventStore):
        self.event_store = event_store
    
    def rebuild_projection(self, projection_handler, from_beginning: bool = True):
        """Rebuild projection from events"""
        start_position = 0 if from_beginning else self._get_last_processed_position()
        
        events = self.event_store.get_all_events(start_position)
        
        for event in events:
            self._handle_event(projection_handler, event)
            self._update_last_processed_position(event.event_id)
    
    def _handle_event(self, projection_handler, event):
        """Route event to appropriate handler"""
        handler_method = f"handle_{self._to_snake_case(event.event_type)}"
        
        if hasattr(projection_handler, handler_method):
            getattr(projection_handler, handler_method)(event)
    
    def replay_for_aggregate(self, aggregate_id: str, to_version: int = None):
        """Replay events for specific aggregate"""
        events = self.event_store.get_events(aggregate_id)
        
        if to_version:
            events = [e for e in events if e.event_version <= to_version]
        
        if not events:
            return None
        
        return BankAccount.from_events(events)
```

### 8. Benefits of Event Sourcing
- **Complete Audit Trail**: Every change is recorded
- **Temporal Queries**: Query state at any point in time
- **Debugging**: Replay events to understand system behavior
- **Business Intelligence**: Rich event data for analytics

### 9. Challenges
- **Complexity**: More complex than traditional CRUD
- **Storage**: Events accumulate over time
- **Performance**: Event replay can be slow
- **Schema Evolution**: Handling event structure changes

### 10. Interview Questions
- What are the benefits and drawbacks of Event Sourcing?
- How do you handle performance issues with event replay?
- Explain the relationship between Event Sourcing and CQRS
- How do you handle event schema evolution?

---
Continue to the next topic for deeper mastery!
