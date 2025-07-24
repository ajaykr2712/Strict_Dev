# Database Design Patterns

## Overview

Database design patterns are proven solutions to common database design problems. These patterns help ensure scalability, maintainability, and performance in data-intensive applications.

## CRUD Patterns

### 1. Active Record Pattern

The Active Record pattern encapsulates database access logic within the model itself.

```python
class User:
    def __init__(self, id=None, username=None, email=None):
        self.id = id
        self.username = username
        self.email = email
    
    def save(self):
        if self.id:
            return self.update()
        else:
            return self.create()
    
    def create(self):
        query = """
        INSERT INTO users (username, email) 
        VALUES (%s, %s) RETURNING id
        """
        result = db.execute(query, (self.username, self.email))
        self.id = result.fetchone()[0]
        return self
    
    def update(self):
        query = """
        UPDATE users SET username = %s, email = %s 
        WHERE id = %s
        """
        db.execute(query, (self.username, self.email, self.id))
        return self
    
    def delete(self):
        query = "DELETE FROM users WHERE id = %s"
        db.execute(query, (self.id,))
    
    @classmethod
    def find_by_id(cls, user_id):
        query = "SELECT * FROM users WHERE id = %s"
        result = db.execute(query, (user_id,))
        row = result.fetchone()
        if row:
            return cls(id=row[0], username=row[1], email=row[2])
        return None
    
    @classmethod
    def find_all(cls):
        query = "SELECT * FROM users"
        result = db.execute(query)
        return [cls(id=row[0], username=row[1], email=row[2]) 
                for row in result.fetchall()]
```

### 2. Data Mapper Pattern

Separates domain objects from database access logic.

```python
class User:
    def __init__(self, id=None, username=None, email=None):
        self.id = id
        self.username = username
        self.email = email

class UserMapper:
    def __init__(self, database):
        self.db = database
    
    def save(self, user):
        if user.id:
            return self.update(user)
        else:
            return self.insert(user)
    
    def insert(self, user):
        query = """
        INSERT INTO users (username, email) 
        VALUES (%s, %s) RETURNING id
        """
        result = self.db.execute(query, (user.username, user.email))
        user.id = result.fetchone()[0]
        return user
    
    def update(self, user):
        query = """
        UPDATE users SET username = %s, email = %s 
        WHERE id = %s
        """
        self.db.execute(query, (user.username, user.email, user.id))
        return user
    
    def delete(self, user):
        query = "DELETE FROM users WHERE id = %s"
        self.db.execute(query, (user.id,))
    
    def find_by_id(self, user_id):
        query = "SELECT * FROM users WHERE id = %s"
        result = self.db.execute(query, (user_id,))
        row = result.fetchone()
        if row:
            return User(id=row[0], username=row[1], email=row[2])
        return None
    
    def find_by_email(self, email):
        query = "SELECT * FROM users WHERE email = %s"
        result = self.db.execute(query, (email,))
        row = result.fetchone()
        if row:
            return User(id=row[0], username=row[1], email=row[2])
        return None

# Usage
user_mapper = UserMapper(database)
user = User(username="john_doe", email="john@example.com")
user_mapper.save(user)
```

### 3. Repository Pattern

Encapsulates data access logic and provides a more object-oriented view.

```python
from abc import ABC, abstractmethod
from typing import List, Optional

class UserRepository(ABC):
    @abstractmethod
    def save(self, user: User) -> User:
        pass
    
    @abstractmethod
    def find_by_id(self, user_id: int) -> Optional[User]:
        pass
    
    @abstractmethod
    def find_by_email(self, email: str) -> Optional[User]:
        pass
    
    @abstractmethod
    def find_all(self) -> List[User]:
        pass
    
    @abstractmethod
    def delete(self, user: User) -> None:
        pass

class DatabaseUserRepository(UserRepository):
    def __init__(self, database):
        self.db = database
    
    def save(self, user: User) -> User:
        if user.id:
            query = """
            UPDATE users SET username = %s, email = %s 
            WHERE id = %s RETURNING *
            """
            result = self.db.execute(query, (user.username, user.email, user.id))
        else:
            query = """
            INSERT INTO users (username, email) 
            VALUES (%s, %s) RETURNING *
            """
            result = self.db.execute(query, (user.username, user.email))
        
        row = result.fetchone()
        return User(id=row[0], username=row[1], email=row[2])
    
    def find_by_id(self, user_id: int) -> Optional[User]:
        query = "SELECT * FROM users WHERE id = %s"
        result = self.db.execute(query, (user_id,))
        row = result.fetchone()
        if row:
            return User(id=row[0], username=row[1], email=row[2])
        return None
    
    def find_by_email(self, email: str) -> Optional[User]:
        query = "SELECT * FROM users WHERE email = %s"
        result = self.db.execute(query, (email,))
        row = result.fetchone()
        if row:
            return User(id=row[0], username=row[1], email=row[2])
        return None
    
    def find_all(self) -> List[User]:
        query = "SELECT * FROM users"
        result = self.db.execute(query)
        return [User(id=row[0], username=row[1], email=row[2]) 
                for row in result.fetchall()]
    
    def delete(self, user: User) -> None:
        query = "DELETE FROM users WHERE id = %s"
        self.db.execute(query, (user.id,))

# In-memory implementation for testing
class InMemoryUserRepository(UserRepository):
    def __init__(self):
        self.users = {}
        self.next_id = 1
    
    def save(self, user: User) -> User:
        if not user.id:
            user.id = self.next_id
            self.next_id += 1
        
        self.users[user.id] = user
        return user
    
    def find_by_id(self, user_id: int) -> Optional[User]:
        return self.users.get(user_id)
    
    def find_by_email(self, email: str) -> Optional[User]:
        for user in self.users.values():
            if user.email == email:
                return user
        return None
    
    def find_all(self) -> List[User]:
        return list(self.users.values())
    
    def delete(self, user: User) -> None:
        if user.id in self.users:
            del self.users[user.id]
```

## Query Patterns

### 1. Query Object Pattern

Encapsulates complex queries in objects.

```python
class QueryObject:
    def __init__(self, database):
        self.db = database
        self.where_conditions = []
        self.order_by = []
        self.limit_value = None
        self.offset_value = None
    
    def where(self, condition, *params):
        self.where_conditions.append((condition, params))
        return self
    
    def order(self, column, direction="ASC"):
        self.order_by.append(f"{column} {direction}")
        return self
    
    def limit(self, count):
        self.limit_value = count
        return self
    
    def offset(self, count):
        self.offset_value = count
        return self
    
    def execute(self):
        query_parts = ["SELECT * FROM users"]
        params = []
        
        if self.where_conditions:
            where_clauses = []
            for condition, condition_params in self.where_conditions:
                where_clauses.append(condition)
                params.extend(condition_params)
            
            query_parts.append("WHERE " + " AND ".join(where_clauses))
        
        if self.order_by:
            query_parts.append("ORDER BY " + ", ".join(self.order_by))
        
        if self.limit_value:
            query_parts.append(f"LIMIT {self.limit_value}")
        
        if self.offset_value:
            query_parts.append(f"OFFSET {self.offset_value}")
        
        query = " ".join(query_parts)
        result = self.db.execute(query, params)
        
        return [User(id=row[0], username=row[1], email=row[2]) 
                for row in result.fetchall()]

# Usage
query = QueryObject(database)
active_users = query.where("created_at > %s", "2023-01-01") \
                   .where("is_active = %s", True) \
                   .order("username") \
                   .limit(10) \
                   .execute()
```

### 2. Specification Pattern

Encapsulates business rules in reusable specifications.

```python
class Specification(ABC):
    @abstractmethod
    def is_satisfied_by(self, candidate) -> bool:
        pass
    
    @abstractmethod
    def to_sql(self) -> tuple:
        """Returns (WHERE clause, parameters)"""
        pass
    
    def and_(self, other):
        return AndSpecification(self, other)
    
    def or_(self, other):
        return OrSpecification(self, other)
    
    def not_(self):
        return NotSpecification(self)

class ActiveUserSpecification(Specification):
    def is_satisfied_by(self, user) -> bool:
        return user.is_active
    
    def to_sql(self) -> tuple:
        return ("is_active = %s", [True])

class RecentUserSpecification(Specification):
    def __init__(self, days_ago):
        self.days_ago = days_ago
    
    def is_satisfied_by(self, user) -> bool:
        from datetime import datetime, timedelta
        cutoff = datetime.now() - timedelta(days=self.days_ago)
        return user.created_at > cutoff
    
    def to_sql(self) -> tuple:
        from datetime import datetime, timedelta
        cutoff = datetime.now() - timedelta(days=self.days_ago)
        return ("created_at > %s", [cutoff])

class AndSpecification(Specification):
    def __init__(self, left, right):
        self.left = left
        self.right = right
    
    def is_satisfied_by(self, candidate) -> bool:
        return (self.left.is_satisfied_by(candidate) and 
                self.right.is_satisfied_by(candidate))
    
    def to_sql(self) -> tuple:
        left_clause, left_params = self.left.to_sql()
        right_clause, right_params = self.right.to_sql()
        
        clause = f"({left_clause}) AND ({right_clause})"
        params = left_params + right_params
        
        return (clause, params)

class UserService:
    def __init__(self, repository):
        self.repository = repository
    
    def find_users_by_specification(self, specification):
        where_clause, params = specification.to_sql()
        query = f"SELECT * FROM users WHERE {where_clause}"
        
        result = self.repository.db.execute(query, params)
        return [User(id=row[0], username=row[1], email=row[2]) 
                for row in result.fetchall()]

# Usage
active_spec = ActiveUserSpecification()
recent_spec = RecentUserSpecification(30)
active_recent_spec = active_spec.and_(recent_spec)

user_service = UserService(user_repository)
users = user_service.find_users_by_specification(active_recent_spec)
```

## Database Connection Patterns

### 1. Connection Pool Pattern

```python
import threading
import queue
from contextlib import contextmanager

class ConnectionPool:
    def __init__(self, create_connection_func, max_connections=10, min_connections=2):
        self.create_connection = create_connection_func
        self.max_connections = max_connections
        self.min_connections = min_connections
        self.pool = queue.Queue(maxsize=max_connections)
        self.all_connections = set()
        self.lock = threading.Lock()
        
        # Create initial connections
        for _ in range(min_connections):
            self._create_and_add_connection()
    
    def _create_and_add_connection(self):
        connection = self.create_connection()
        self.pool.put(connection)
        self.all_connections.add(connection)
    
    @contextmanager
    def get_connection(self, timeout=30):
        connection = None
        try:
            # Try to get existing connection
            try:
                connection = self.pool.get(timeout=timeout)
            except queue.Empty:
                with self.lock:
                    if len(self.all_connections) < self.max_connections:
                        self._create_and_add_connection()
                        connection = self.pool.get_nowait()
                    else:
                        raise Exception("Connection pool exhausted")
            
            yield connection
        
        finally:
            if connection:
                # Return connection to pool
                if self._is_connection_healthy(connection):
                    self.pool.put(connection)
                else:
                    self._replace_connection(connection)
    
    def _is_connection_healthy(self, connection):
        try:
            # Implement health check
            connection.ping()
            return True
        except:
            return False
    
    def _replace_connection(self, old_connection):
        with self.lock:
            self.all_connections.discard(old_connection)
            try:
                old_connection.close()
            except:
                pass
            
            if len(self.all_connections) < self.min_connections:
                self._create_and_add_connection()
    
    def close_all(self):
        with self.lock:
            for connection in self.all_connections:
                try:
                    connection.close()
                except:
                    pass
            self.all_connections.clear()

# Usage
def create_db_connection():
    # Your database connection creation logic
    pass

pool = ConnectionPool(create_db_connection, max_connections=20)

with pool.get_connection() as conn:
    result = conn.execute("SELECT * FROM users")
```

### 2. Unit of Work Pattern

```python
class UnitOfWork:
    def __init__(self, session_factory):
        self.session_factory = session_factory
        self._new_objects = set()
        self._dirty_objects = set()
        self._removed_objects = set()
    
    def __enter__(self):
        self.session = self.session_factory()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type:
            self.rollback()
        else:
            self.commit()
        self.session.close()
    
    def register_new(self, obj):
        self._new_objects.add(obj)
    
    def register_dirty(self, obj):
        self._dirty_objects.add(obj)
    
    def register_removed(self, obj):
        self._removed_objects.add(obj)
    
    def commit(self):
        try:
            self.session.begin()
            
            # Insert new objects
            for obj in self._new_objects:
                self._insert(obj)
            
            # Update dirty objects
            for obj in self._dirty_objects:
                self._update(obj)
            
            # Delete removed objects
            for obj in self._removed_objects:
                self._delete(obj)
            
            self.session.commit()
            self._clear_tracking()
        
        except Exception as e:
            self.session.rollback()
            raise e
    
    def rollback(self):
        self.session.rollback()
        self._clear_tracking()
    
    def _clear_tracking(self):
        self._new_objects.clear()
        self._dirty_objects.clear()
        self._removed_objects.clear()
    
    def _insert(self, obj):
        # Implementation specific to your ORM/database
        pass
    
    def _update(self, obj):
        # Implementation specific to your ORM/database
        pass
    
    def _delete(self, obj):
        # Implementation specific to your ORM/database
        pass

# Usage
with UnitOfWork(session_factory) as uow:
    user1 = User(username="alice", email="alice@example.com")
    user2 = User(username="bob", email="bob@example.com")
    
    uow.register_new(user1)
    uow.register_new(user2)
    
    # Changes are committed when exiting the context
```

## Caching Patterns

### 1. Cache-Aside Pattern

```python
class CacheAsideRepository:
    def __init__(self, cache, database):
        self.cache = cache
        self.db = database
    
    def get_user(self, user_id):
        # Try cache first
        cache_key = f"user:{user_id}"
        user_data = self.cache.get(cache_key)
        
        if user_data:
            return User.from_dict(user_data)
        
        # Cache miss - get from database
        user = self._get_user_from_db(user_id)
        if user:
            # Store in cache for future requests
            self.cache.set(cache_key, user.to_dict(), expire=3600)
        
        return user
    
    def save_user(self, user):
        # Update database first
        user = self._save_user_to_db(user)
        
        # Invalidate cache
        cache_key = f"user:{user.id}"
        self.cache.delete(cache_key)
        
        return user
```

### 2. Write-Through Cache Pattern

```python
class WriteThroughRepository:
    def __init__(self, cache, database):
        self.cache = cache
        self.db = database
    
    def save_user(self, user):
        # Write to database
        user = self._save_user_to_db(user)
        
        # Write to cache
        cache_key = f"user:{user.id}"
        self.cache.set(cache_key, user.to_dict(), expire=3600)
        
        return user
    
    def get_user(self, user_id):
        cache_key = f"user:{user_id}"
        user_data = self.cache.get(cache_key)
        
        if user_data:
            return User.from_dict(user_data)
        
        # If not in cache, load from database and cache it
        user = self._get_user_from_db(user_id)
        if user:
            self.cache.set(cache_key, user.to_dict(), expire=3600)
        
        return user
```

## Transaction Patterns

### 1. Transaction Script Pattern

```python
class UserRegistrationService:
    def __init__(self, database):
        self.db = database
    
    def register_user(self, username, email, password):
        with self.db.transaction() as tx:
            # Check if user already exists
            existing_user = tx.execute(
                "SELECT id FROM users WHERE email = %s", 
                (email,)
            ).fetchone()
            
            if existing_user:
                raise ValueError("User already exists")
            
            # Create user
            user_id = tx.execute(
                """
                INSERT INTO users (username, email, password_hash, created_at) 
                VALUES (%s, %s, %s, NOW()) RETURNING id
                """,
                (username, email, self._hash_password(password))
            ).fetchone()[0]
            
            # Create user profile
            tx.execute(
                """
                INSERT INTO user_profiles (user_id, display_name) 
                VALUES (%s, %s)
                """,
                (user_id, username)
            )
            
            # Send welcome email
            self._queue_welcome_email(email, username)
            
            # Audit log
            tx.execute(
                """
                INSERT INTO audit_logs (action, user_id, created_at) 
                VALUES ('user_registered', %s, NOW())
                """,
                (user_id,)
            )
            
            return user_id
    
    def _hash_password(self, password):
        # Implementation
        pass
    
    def _queue_welcome_email(self, email, username):
        # Implementation
        pass
```

## Best Practices

### 1. Separation of Concerns
- Keep business logic separate from data access
- Use abstractions and interfaces
- Implement dependency injection

### 2. Error Handling
```python
class DatabaseError(Exception):
    pass

class UserNotFoundError(DatabaseError):
    pass

class DuplicateUserError(DatabaseError):
    pass

class UserRepository:
    def find_by_id(self, user_id):
        try:
            result = self.db.execute("SELECT * FROM users WHERE id = %s", (user_id,))
            row = result.fetchone()
            
            if not row:
                raise UserNotFoundError(f"User with id {user_id} not found")
            
            return User.from_row(row)
        
        except DatabaseConnectionError as e:
            logger.error(f"Database connection failed: {e}")
            raise DatabaseError("Unable to connect to database")
        
        except Exception as e:
            logger.error(f"Unexpected error in find_by_id: {e}")
            raise DatabaseError("Database operation failed")
```

### 3. Performance Considerations
- Use connection pooling
- Implement proper indexing
- Consider read replicas for scaling
- Use appropriate caching strategies
- Monitor query performance

### 4. Testing Patterns
```python
class TestUserRepository(unittest.TestCase):
    def setUp(self):
        self.repository = InMemoryUserRepository()
    
    def test_save_new_user(self):
        user = User(username="test_user", email="test@example.com")
        saved_user = self.repository.save(user)
        
        self.assertIsNotNone(saved_user.id)
        self.assertEqual(saved_user.username, "test_user")
    
    def test_find_by_email(self):
        user = User(username="test_user", email="test@example.com")
        self.repository.save(user)
        
        found_user = self.repository.find_by_email("test@example.com")
        self.assertIsNotNone(found_user)
        self.assertEqual(found_user.username, "test_user")
```

These patterns provide a solid foundation for building maintainable and scalable database layers in your applications. Choose the patterns that best fit your specific use case and requirements.
