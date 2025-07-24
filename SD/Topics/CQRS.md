# CQRS (Command Query Responsibility Segregation)

## Overview
CQRS is an architectural pattern that separates read and write operations for a data store. It proposes using different models to update information (Commands) and to read information (Queries), allowing for optimized data access patterns and improved scalability.

## Key Concepts
- **Command Model**: Handles write operations and business logic
- **Query Model**: Optimized for read operations and data presentation
- **Separation of Concerns**: Different models for different responsibilities
- **Event Sourcing Integration**: Natural fit with event-driven architectures

## Advanced Topics

### 1. CQRS Architecture Pattern

#### Traditional CRUD Model
```
[Client] → [Application] → [Single Model] → [Database]
            ↓                ↓              ↓
        [Business Logic] [Entity Model] [Shared Schema]
```

#### CQRS Model
```
[Client Commands] → [Command Handler] → [Write Model] → [Event Store]
                                           ↓
                                      [Events] → [Event Handler] → [Read Model]
                                                      ↓
[Client Queries] ← [Query Handler] ← [Optimized Views] ← [Projections]
```

### 2. Command Side Implementation

#### Command Definition
```python
from dataclasses import dataclass
from typing import Optional
import uuid

@dataclass
class CreateOrderCommand:
    customer_id: str
    items: list
    shipping_address: dict
    payment_method: str
    order_id: Optional[str] = None
    
    def __post_init__(self):
        if not self.order_id:
            self.order_id = str(uuid.uuid4())

@dataclass
class UpdateOrderStatusCommand:
    order_id: str
    status: str
    updated_by: str
    reason: Optional[str] = None
```

#### Command Handler
```python
class OrderCommandHandler:
    def __init__(self, order_repository, event_bus):
        self.order_repository = order_repository
        self.event_bus = event_bus
    
    def handle_create_order(self, command: CreateOrderCommand):
        """Handle order creation command"""
        # Validate command
        self._validate_create_order(command)
        
        # Create domain entity
        order = Order.create(
            order_id=command.order_id,
            customer_id=command.customer_id,
            items=command.items,
            shipping_address=command.shipping_address,
            payment_method=command.payment_method
        )
        
        # Persist to write model
        self.order_repository.save(order)
        
        # Publish domain events
        events = order.get_uncommitted_events()
        for event in events:
            self.event_bus.publish(event)
        
        order.mark_events_as_committed()
        
        return order.order_id
    
    def handle_update_status(self, command: UpdateOrderStatusCommand):
        """Handle order status update"""
        order = self.order_repository.get_by_id(command.order_id)
        if not order:
            raise OrderNotFoundError(command.order_id)
        
        order.update_status(command.status, command.updated_by, command.reason)
        self.order_repository.save(order)
        
        events = order.get_uncommitted_events()
        for event in events:
            self.event_bus.publish(event)
        
        order.mark_events_as_committed()
```

### 3. Query Side Implementation

#### Query Models
```python
@dataclass
class OrderSummaryView:
    order_id: str
    customer_name: str
    order_date: datetime
    total_amount: decimal.Decimal
    status: str
    item_count: int

@dataclass
class OrderDetailsView:
    order_id: str
    customer_id: str
    customer_name: str
    customer_email: str
    order_date: datetime
    items: List[dict]
    shipping_address: dict
    billing_address: dict
    payment_method: str
    status: str
    status_history: List[dict]
    total_amount: decimal.Decimal
```

#### Query Handler
```python
class OrderQueryHandler:
    def __init__(self, read_db):
        self.read_db = read_db
    
    def get_orders_by_customer(self, customer_id: str, page: int = 1, size: int = 10):
        """Get customer orders with pagination"""
        offset = (page - 1) * size
        
        query = """
        SELECT order_id, customer_name, order_date, total_amount, status, item_count
        FROM order_summary_view 
        WHERE customer_id = %s 
        ORDER BY order_date DESC 
        LIMIT %s OFFSET %s
        """
        
        results = self.read_db.execute(query, (customer_id, size, offset))
        return [OrderSummaryView(**row) for row in results]
    
    def get_order_details(self, order_id: str):
        """Get detailed order information"""
        query = """
        SELECT * FROM order_details_view WHERE order_id = %s
        """
        
        result = self.read_db.execute_one(query, (order_id,))
        if not result:
            return None
        
        return OrderDetailsView(**result)
    
    def search_orders(self, filters: dict):
        """Search orders with complex filters"""
        conditions = []
        params = []
        
        if filters.get('status'):
            conditions.append("status = %s")
            params.append(filters['status'])
        
        if filters.get('date_from'):
            conditions.append("order_date >= %s")
            params.append(filters['date_from'])
        
        if filters.get('date_to'):
            conditions.append("order_date <= %s")
            params.append(filters['date_to'])
        
        if filters.get('min_amount'):
            conditions.append("total_amount >= %s")
            params.append(filters['min_amount'])
        
        where_clause = " AND ".join(conditions) if conditions else "1=1"
        
        query = f"""
        SELECT order_id, customer_name, order_date, total_amount, status, item_count
        FROM order_summary_view 
        WHERE {where_clause}
        ORDER BY order_date DESC
        """
        
        results = self.read_db.execute(query, params)
        return [OrderSummaryView(**row) for row in results]
```

### 4. Event-Driven Projections

#### Event Handlers for Projections
```python
class OrderProjectionHandler:
    def __init__(self, read_db):
        self.read_db = read_db
    
    def handle_order_created(self, event):
        """Update read model when order is created"""
        # Insert into order summary view
        self.read_db.execute("""
            INSERT INTO order_summary_view 
            (order_id, customer_id, customer_name, order_date, total_amount, status, item_count)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (
            event.order_id,
            event.customer_id,
            event.customer_name,
            event.order_date,
            event.total_amount,
            'pending',
            len(event.items)
        ))
        
        # Insert into order details view
        self.read_db.execute("""
            INSERT INTO order_details_view 
            (order_id, customer_id, customer_name, customer_email, order_date, 
             items, shipping_address, payment_method, status, total_amount)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (
            event.order_id,
            event.customer_id,
            event.customer_name,
            event.customer_email,
            event.order_date,
            json.dumps(event.items),
            json.dumps(event.shipping_address),
            event.payment_method,
            'pending',
            event.total_amount
        ))
    
    def handle_order_status_updated(self, event):
        """Update read model when order status changes"""
        # Update summary view
        self.read_db.execute("""
            UPDATE order_summary_view 
            SET status = %s 
            WHERE order_id = %s
        """, (event.new_status, event.order_id))
        
        # Update details view
        self.read_db.execute("""
            UPDATE order_details_view 
            SET status = %s 
            WHERE order_id = %s
        """, (event.new_status, event.order_id))
        
        # Add to status history
        self.read_db.execute("""
            INSERT INTO order_status_history 
            (order_id, status, changed_by, changed_at, reason)
            VALUES (%s, %s, %s, %s, %s)
        """, (
            event.order_id,
            event.new_status,
            event.changed_by,
            event.changed_at,
            event.reason
        ))
```

### 5. Database Design for CQRS

#### Write Model Schema (Normalized)
```sql
-- Write model optimized for consistency and transactions
CREATE TABLE orders (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE order_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSON NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Read Model Schema (Denormalized)
```sql
-- Read model optimized for queries
CREATE TABLE order_summary_view (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    item_count INT NOT NULL,
    INDEX idx_customer_date (customer_id, order_date),
    INDEX idx_status_date (status, order_date),
    INDEX idx_amount (total_amount)
);

CREATE TABLE order_details_view (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(100) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    items JSON NOT NULL,
    shipping_address JSON NOT NULL,
    billing_address JSON,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    status_history JSON,
    total_amount DECIMAL(10,2) NOT NULL
);
```

### 6. CQRS with Event Sourcing

#### Event Store Integration
```python
class EventSourcedOrderRepository:
    def __init__(self, event_store):
        self.event_store = event_store
    
    def save(self, order):
        """Save order by storing events"""
        events = order.get_uncommitted_events()
        self.event_store.save_events(order.order_id, events, order.version)
    
    def get_by_id(self, order_id):
        """Reconstruct order from events"""
        events = self.event_store.get_events(order_id)
        if not events:
            return None
        
        order = Order()
        for event in events:
            order.apply_event(event)
        
        return order

class Order:
    def __init__(self):
        self.order_id = None
        self.customer_id = None
        self.items = []
        self.status = 'draft'
        self.version = 0
        self.uncommitted_events = []
    
    @classmethod
    def create(cls, order_id, customer_id, items, shipping_address, payment_method):
        """Create new order"""
        order = cls()
        
        event = OrderCreatedEvent(
            order_id=order_id,
            customer_id=customer_id,
            items=items,
            shipping_address=shipping_address,
            payment_method=payment_method,
            created_at=datetime.utcnow()
        )
        
        order.apply_event(event)
        order.uncommitted_events.append(event)
        
        return order
    
    def apply_event(self, event):
        """Apply event to update order state"""
        if isinstance(event, OrderCreatedEvent):
            self.order_id = event.order_id
            self.customer_id = event.customer_id
            self.items = event.items
            self.status = 'pending'
        
        elif isinstance(event, OrderStatusUpdatedEvent):
            self.status = event.new_status
        
        self.version += 1
```

### 7. Performance Benefits

#### Read Optimization
```python
class AdvancedQueryHandler:
    def __init__(self, read_db, cache):
        self.read_db = read_db
        self.cache = cache
    
    def get_popular_products(self, days=30):
        """Get popular products with caching"""
        cache_key = f"popular_products:{days}"
        
        # Try cache first
        result = self.cache.get(cache_key)
        if result:
            return result
        
        # Query optimized read model
        query = """
        SELECT product_id, product_name, total_quantity, total_orders
        FROM product_sales_summary 
        WHERE sale_date >= DATE_SUB(NOW(), INTERVAL %s DAY)
        ORDER BY total_quantity DESC 
        LIMIT 10
        """
        
        result = self.read_db.execute(query, (days,))
        
        # Cache for 1 hour
        self.cache.set(cache_key, result, ttl=3600)
        
        return result
    
    def get_customer_analytics(self, customer_id):
        """Get customer analytics from materialized view"""
        query = """
        SELECT 
            total_orders,
            total_spent,
            average_order_value,
            favorite_category,
            last_order_date,
            customer_segment
        FROM customer_analytics_view 
        WHERE customer_id = %s
        """
        
        return self.read_db.execute_one(query, (customer_id,))
```

### 8. Benefits
- **Scalability**: Independent scaling of read and write sides
- **Performance**: Optimized data models for specific use cases
- **Flexibility**: Different technologies for different needs
- **Complexity Management**: Clear separation of concerns

### 9. Challenges
- **Complexity**: More complex than simple CRUD
- **Eventual Consistency**: Read models may lag behind writes
- **Data Synchronization**: Keeping projections up to date
- **Debugging**: Harder to trace data flow

### 10. When to Use CQRS
- **High read/write ratio**: Many more reads than writes
- **Complex queries**: Reporting and analytics requirements
- **Different scaling needs**: Read and write sides scale differently
- **Event-driven architecture**: Natural fit with event sourcing

### 11. Interview Questions
- Explain the difference between CQRS and traditional CRUD
- How do you handle eventual consistency in CQRS?
- What are the benefits and drawbacks of CQRS?
- How does CQRS integrate with Event Sourcing?

---
Continue to the next topic for deeper mastery!
