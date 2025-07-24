# Distributed Transactions

## Overview

Distributed transactions are operations that span multiple databases or services and must maintain ACID properties across all participants. Managing transactions in distributed systems is one of the most challenging aspects of system design due to network failures, service availability, and the need for consensus.

## The Challenge

In a monolithic application with a single database, transactions are straightforward:

```sql
BEGIN TRANSACTION;
UPDATE accounts SET balance = balance - 100 WHERE user_id = 1;
UPDATE accounts SET balance = balance + 100 WHERE user_id = 2;
COMMIT;
```

In distributed systems, this becomes complex when the operations span multiple services/databases:

```
Service A: Debit account (Database A)
Service B: Credit account (Database B)
Service C: Update audit log (Database C)
```

## ACID Properties in Distributed Systems

### Atomicity
All operations succeed or all fail together, even across multiple systems.

### Consistency
System remains in a valid state before and after the transaction.

### Isolation
Concurrent transactions don't interfere with each other.

### Durability
Once committed, changes are permanent even in case of failures.

## Distributed Transaction Patterns

### 1. Two-Phase Commit (2PC)

The classic approach with a coordinator and participants.

**Phase 1: Prepare**
```python
class TwoPhaseCommitCoordinator:
    def __init__(self, participants):
        self.participants = participants
        self.transaction_id = None
    
    def begin_transaction(self, operations):
        self.transaction_id = str(uuid.uuid4())
        
        # Phase 1: Prepare
        prepare_responses = []
        for participant, operation in zip(self.participants, operations):
            response = participant.prepare(self.transaction_id, operation)
            prepare_responses.append(response)
        
        # Check if all participants are ready
        all_prepared = all(response.vote == "YES" for response in prepare_responses)
        
        if all_prepared:
            # Phase 2: Commit
            return self.commit_phase()
        else:
            # Phase 2: Abort
            return self.abort_phase()
    
    def commit_phase(self):
        commit_results = []
        for participant in self.participants:
            result = participant.commit(self.transaction_id)
            commit_results.append(result)
        
        return all(result.success for result in commit_results)
    
    def abort_phase(self):
        for participant in self.participants:
            participant.abort(self.transaction_id)
        return False

class TwoPhaseCommitParticipant:
    def __init__(self, database):
        self.database = database
        self.prepared_transactions = {}
    
    def prepare(self, transaction_id, operation):
        try:
            # Execute operation but don't commit
            self.database.begin_transaction()
            self.database.execute(operation)
            
            # Hold locks and prepare to commit
            self.prepared_transactions[transaction_id] = {
                "operation": operation,
                "prepared_at": time.time()
            }
            
            return PrepareResponse(vote="YES")
        except Exception as e:
            self.database.rollback()
            return PrepareResponse(vote="NO", error=str(e))
    
    def commit(self, transaction_id):
        try:
            if transaction_id in self.prepared_transactions:
                self.database.commit()
                del self.prepared_transactions[transaction_id]
                return CommitResponse(success=True)
        except Exception as e:
            return CommitResponse(success=False, error=str(e))
    
    def abort(self, transaction_id):
        if transaction_id in self.prepared_transactions:
            self.database.rollback()
            del self.prepared_transactions[transaction_id]
```

**Problems with 2PC:**
- Blocking: Participants wait for coordinator
- Single point of failure
- Network partition issues
- Performance overhead

### 2. Three-Phase Commit (3PC)

Adds a "pre-commit" phase to reduce blocking.

```python
class ThreePhaseCommitCoordinator:
    def begin_transaction(self, operations):
        transaction_id = str(uuid.uuid4())
        
        # Phase 1: Prepare
        if not self.prepare_phase(transaction_id, operations):
            return self.abort_transaction(transaction_id)
        
        # Phase 2: Pre-commit
        if not self.pre_commit_phase(transaction_id):
            return self.abort_transaction(transaction_id)
        
        # Phase 3: Commit
        return self.commit_phase(transaction_id)
    
    def prepare_phase(self, transaction_id, operations):
        prepare_responses = []
        for participant, operation in zip(self.participants, operations):
            response = participant.prepare(transaction_id, operation)
            prepare_responses.append(response)
        
        return all(response.vote == "YES" for response in prepare_responses)
    
    def pre_commit_phase(self, transaction_id):
        pre_commit_responses = []
        for participant in self.participants:
            response = participant.pre_commit(transaction_id)
            pre_commit_responses.append(response)
        
        return all(response.success for response in pre_commit_responses)
    
    def commit_phase(self, transaction_id):
        commit_responses = []
        for participant in self.participants:
            response = participant.commit(transaction_id)
            commit_responses.append(response)
        
        return all(response.success for response in commit_responses)
```

### 3. Saga Pattern

Break down transactions into a series of local transactions with compensating actions.

```python
class SagaOrchestrator:
    def __init__(self):
        self.steps = []
        self.compensation_steps = []
    
    def add_step(self, action, compensation):
        self.steps.append(action)
        self.compensation_steps.append(compensation)
    
    def execute(self):
        completed_steps = []
        
        try:
            for i, step in enumerate(self.steps):
                result = step.execute()
                if not result.success:
                    raise SagaExecutionException(f"Step {i} failed: {result.error}")
                completed_steps.append(i)
            
            return SagaResult(success=True)
        
        except SagaExecutionException:
            # Compensate in reverse order
            self.compensate(completed_steps)
            return SagaResult(success=False)
    
    def compensate(self, completed_steps):
        for step_index in reversed(completed_steps):
            compensation = self.compensation_steps[step_index]
            try:
                compensation.execute()
            except Exception as e:
                # Log compensation failure
                logger.error(f"Compensation failed for step {step_index}: {e}")

# Example: E-commerce order processing
class OrderProcessingSaga:
    def __init__(self, order):
        self.order = order
        self.saga = SagaOrchestrator()
        self.setup_saga_steps()
    
    def setup_saga_steps(self):
        # Step 1: Reserve inventory
        self.saga.add_step(
            action=ReserveInventoryAction(self.order),
            compensation=ReleaseInventoryAction(self.order)
        )
        
        # Step 2: Process payment
        self.saga.add_step(
            action=ProcessPaymentAction(self.order),
            compensation=RefundPaymentAction(self.order)
        )
        
        # Step 3: Create shipment
        self.saga.add_step(
            action=CreateShipmentAction(self.order),
            compensation=CancelShipmentAction(self.order)
        )
        
        # Step 4: Update order status
        self.saga.add_step(
            action=UpdateOrderStatusAction(self.order, "CONFIRMED"),
            compensation=UpdateOrderStatusAction(self.order, "CANCELLED")
        )
    
    def process_order(self):
        return self.saga.execute()

class ReserveInventoryAction:
    def __init__(self, order):
        self.order = order
        self.inventory_service = InventoryService()
    
    def execute(self):
        try:
            for item in self.order.items:
                reserved = self.inventory_service.reserve_item(
                    item.product_id, 
                    item.quantity,
                    self.order.order_id
                )
                if not reserved:
                    return ActionResult(success=False, error=f"Could not reserve {item.product_id}")
            
            return ActionResult(success=True)
        except Exception as e:
            return ActionResult(success=False, error=str(e))

class ReleaseInventoryAction:
    def __init__(self, order):
        self.order = order
        self.inventory_service = InventoryService()
    
    def execute(self):
        for item in self.order.items:
            self.inventory_service.release_reservation(
                item.product_id,
                item.quantity,
                self.order.order_id
            )
        return ActionResult(success=True)
```

### 4. Event Sourcing with Outbox Pattern

Ensure consistency between database changes and event publishing.

```python
class OutboxPattern:
    def __init__(self, database, event_publisher):
        self.database = database
        self.event_publisher = event_publisher
    
    def execute_transaction_with_events(self, business_operation, events):
        with self.database.transaction():
            # Execute business operation
            business_result = business_operation.execute()
            
            # Store events in outbox table
            for event in events:
                self.store_event_in_outbox(event)
            
            # Commit transaction (business data + outbox events)
            self.database.commit()
        
        # Publish events asynchronously
        self.publish_outbox_events()
        
        return business_result
    
    def store_event_in_outbox(self, event):
        outbox_entry = {
            "event_id": str(uuid.uuid4()),
            "event_type": event.type,
            "event_data": json.dumps(event.data),
            "created_at": datetime.now(),
            "published": False
        }
        self.database.insert("outbox", outbox_entry)
    
    def publish_outbox_events(self):
        unpublished_events = self.database.query(
            "SELECT * FROM outbox WHERE published = false ORDER BY created_at"
        )
        
        for event_record in unpublished_events:
            try:
                event = Event(
                    id=event_record["event_id"],
                    type=event_record["event_type"],
                    data=json.loads(event_record["event_data"])
                )
                
                self.event_publisher.publish(event)
                
                # Mark as published
                self.database.update(
                    "outbox",
                    {"published": True},
                    f"event_id = '{event_record['event_id']}'"
                )
            
            except Exception as e:
                logger.error(f"Failed to publish event {event_record['event_id']}: {e}")

# Example usage
class OrderService:
    def __init__(self):
        self.database = Database()
        self.event_publisher = EventPublisher()
        self.outbox = OutboxPattern(self.database, self.event_publisher)
    
    def create_order(self, order_data):
        def create_order_operation():
            # Create order in database
            order_id = self.database.insert("orders", order_data)
            return {"order_id": order_id}
        
        # Events to publish
        events = [
            Event("OrderCreated", {"order_id": order_data["order_id"]}),
            Event("InventoryReservationRequested", {"order_id": order_data["order_id"], "items": order_data["items"]})
        ]
        
        return self.outbox.execute_transaction_with_events(create_order_operation, events)
```

## Distributed Transaction Challenges

### 1. CAP Theorem Implications
Can't have Consistency, Availability, and Partition tolerance simultaneously.

### 2. Network Failures
```python
class NetworkAwareTransactionManager:
    def __init__(self):
        self.timeout_configs = {
            "prepare_timeout": 30,  # seconds
            "commit_timeout": 60,
            "abort_timeout": 30
        }
    
    def execute_with_timeout(self, operation, timeout_key):
        timeout = self.timeout_configs[timeout_key]
        
        try:
            return asyncio.wait_for(operation(), timeout=timeout)
        except asyncio.TimeoutError:
            raise TransactionTimeoutException(f"Operation timed out after {timeout}s")
```

### 3. Partial Failures
```python
class PartialFailureHandler:
    def __init__(self):
        self.retry_policy = RetryPolicy(max_attempts=3, backoff_factor=2)
    
    def handle_participant_failure(self, participant_id, transaction_id):
        # Try to recover participant
        recovery_result = self.attempt_recovery(participant_id)
        
        if recovery_result.success:
            # Resume transaction
            return self.resume_transaction(transaction_id)
        else:
            # Abort transaction
            return self.abort_transaction(transaction_id)
    
    def attempt_recovery(self, participant_id):
        for attempt in range(self.retry_policy.max_attempts):
            try:
                participant = self.get_participant(participant_id)
                health_check = participant.health_check()
                
                if health_check.healthy:
                    return RecoveryResult(success=True)
                
            except Exception as e:
                if attempt == self.retry_policy.max_attempts - 1:
                    return RecoveryResult(success=False, error=str(e))
                
                time.sleep(self.retry_policy.backoff_factor ** attempt)
```

## Best Practices

### 1. Design for Failure
```python
class ResilientTransactionManager:
    def __init__(self):
        self.circuit_breaker = CircuitBreaker()
        self.retry_handler = RetryHandler()
    
    def execute_transaction(self, transaction):
        # Use circuit breaker to fail fast
        if not self.circuit_breaker.can_execute():
            raise CircuitBreakerOpenException("Transaction service unavailable")
        
        try:
            result = self.retry_handler.execute_with_retry(
                lambda: self._execute_transaction_internal(transaction)
            )
            self.circuit_breaker.record_success()
            return result
        
        except Exception as e:
            self.circuit_breaker.record_failure()
            raise e
```

### 2. Idempotency
```python
class IdempotentTransactionProcessor:
    def __init__(self):
        self.processed_transactions = set()
    
    def process_transaction(self, transaction_id, transaction):
        if transaction_id in self.processed_transactions:
            # Return cached result for idempotency
            return self.get_cached_result(transaction_id)
        
        result = self.execute_transaction(transaction)
        self.processed_transactions.add(transaction_id)
        self.cache_result(transaction_id, result)
        
        return result
```

### 3. Monitoring and Observability
```python
class TransactionMonitor:
    def __init__(self):
        self.metrics = MetricsCollector()
    
    def monitor_transaction(self, transaction_id, participants):
        start_time = time.time()
        
        try:
            result = self.execute_monitored_transaction(transaction_id, participants)
            
            # Record success metrics
            duration = time.time() - start_time
            self.metrics.record_transaction_success(duration, len(participants))
            
            return result
        
        except Exception as e:
            # Record failure metrics
            duration = time.time() - start_time
            self.metrics.record_transaction_failure(duration, str(type(e).__name__))
            raise e
    
    def record_participant_metrics(self, participant_id, operation, duration, success):
        tags = {
            "participant_id": participant_id,
            "operation": operation,
            "success": success
        }
        self.metrics.histogram("participant.operation.duration", duration, tags=tags)
```

## When to Use Each Pattern

### Use 2PC/3PC When:
- Strong consistency is required
- Small number of participants
- Network is reliable
- Can tolerate blocking

### Use Saga Pattern When:
- Long-running transactions
- Many participants
- Network unreliability
- Can tolerate eventual consistency

### Use Event Sourcing When:
- Need audit trail
- Complex business logic
- High read/write ratio differences
- Microservices architecture

## Real-World Examples

### Banking Transfer
```python
class BankTransferSaga:
    def transfer_money(self, from_account, to_account, amount):
        saga = SagaOrchestrator()
        
        # Debit source account
        saga.add_step(
            DebitAccountAction(from_account, amount),
            CreditAccountAction(from_account, amount)  # Compensation
        )
        
        # Credit destination account
        saga.add_step(
            CreditAccountAction(to_account, amount),
            DebitAccountAction(to_account, amount)  # Compensation
        )
        
        # Record transaction
        saga.add_step(
            RecordTransactionAction(from_account, to_account, amount),
            DeleteTransactionAction()  # Compensation
        )
        
        return saga.execute()
```

### E-commerce Checkout
```python
class CheckoutSaga:
    def process_checkout(self, cart, payment_info):
        saga = SagaOrchestrator()
        
        # Reserve inventory
        saga.add_step(
            ReserveInventoryAction(cart),
            ReleaseInventoryAction(cart)
        )
        
        # Process payment
        saga.add_step(
            ChargePaymentAction(payment_info),
            RefundPaymentAction(payment_info)
        )
        
        # Create order
        saga.add_step(
            CreateOrderAction(cart),
            CancelOrderAction()
        )
        
        # Send confirmation email
        saga.add_step(
            SendConfirmationEmailAction(),
            SendCancellationEmailAction()
        )
        
        return saga.execute()
```

## Conclusion

Distributed transactions are complex but necessary for maintaining data consistency across multiple services. The choice of pattern depends on your consistency requirements, network reliability, and tolerance for complexity. Modern systems often favor eventually consistent approaches like Saga patterns over traditional 2PC for better availability and resilience.
