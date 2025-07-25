# Hexagonal Architecture (Ports and Adapters)

## Overview

Hexagonal Architecture, also known as Ports and Adapters pattern, is an architectural pattern that aims to create loosely coupled application components that can be easily connected to their software environment by means of ports and adapters.

## Core Concepts

### 1. The Hexagon
- Represents the application core (business logic)
- Contains the domain model and business rules
- Independent of external concerns

### 2. Ports
- Define interfaces for communication with the outside world
- Input ports: Interfaces for driving the application (API endpoints)
- Output ports: Interfaces for driven adapters (databases, external services)

### 3. Adapters
- Implement the ports
- Primary adapters: Drive the application (web controllers, CLI)
- Secondary adapters: Driven by the application (database repositories, email services)

## Implementation Example

### Domain Layer
```python
# Domain Entity
class User:
    def __init__(self, user_id, email, name):
        self.user_id = user_id
        self.email = email
        self.name = name
        self.is_active = True
        
    def deactivate(self):
        if not self.is_active:
            raise ValueError("User is already deactivated")
        self.is_active = False
        
    def update_profile(self, name=None, email=None):
        if name:
            self.name = name
        if email:
            self.email = email

# Domain Service
class UserDomainService:
    def is_email_unique(self, email, user_repository):
        existing_user = user_repository.find_by_email(email)
        return existing_user is None
```

### Ports (Interfaces)
```python
from abc import ABC, abstractmethod

# Input Port (Primary Port)
class UserManagementUseCase(ABC):
    @abstractmethod
    def create_user(self, email, name):
        pass
        
    @abstractmethod
    def get_user(self, user_id):
        pass
        
    @abstractmethod
    def update_user(self, user_id, name=None, email=None):
        pass
        
    @abstractmethod
    def deactivate_user(self, user_id):
        pass

# Output Port (Secondary Port)
class UserRepository(ABC):
    @abstractmethod
    def save(self, user):
        pass
        
    @abstractmethod
    def find_by_id(self, user_id):
        pass
        
    @abstractmethod
    def find_by_email(self, email):
        pass
        
    @abstractmethod
    def delete(self, user_id):
        pass

class EmailService(ABC):
    @abstractmethod
    def send_welcome_email(self, user):
        pass
        
    @abstractmethod
    def send_deactivation_email(self, user):
        pass
```

### Application Layer (Use Cases)
```python
import uuid

class UserManagementService(UserManagementUseCase):
    def __init__(self, user_repository: UserRepository, 
                 email_service: EmailService,
                 user_domain_service: UserDomainService):
        self.user_repository = user_repository
        self.email_service = email_service
        self.user_domain_service = user_domain_service
        
    def create_user(self, email, name):
        # Validate business rules
        if not self.user_domain_service.is_email_unique(email, self.user_repository):
            raise ValueError("Email already exists")
            
        # Create domain entity
        user_id = str(uuid.uuid4())
        user = User(user_id, email, name)
        
        # Save through repository
        self.user_repository.save(user)
        
        # Send welcome email
        self.email_service.send_welcome_email(user)
        
        return user
        
    def get_user(self, user_id):
        user = self.user_repository.find_by_id(user_id)
        if not user:
            raise ValueError("User not found")
        return user
        
    def update_user(self, user_id, name=None, email=None):
        user = self.get_user(user_id)
        
        if email and not self.user_domain_service.is_email_unique(email, self.user_repository):
            raise ValueError("Email already exists")
            
        user.update_profile(name, email)
        self.user_repository.save(user)
        return user
        
    def deactivate_user(self, user_id):
        user = self.get_user(user_id)
        user.deactivate()
        self.user_repository.save(user)
        self.email_service.send_deactivation_email(user)
```

### Primary Adapters
```python
from flask import Flask, request, jsonify

# REST API Adapter
class UserController:
    def __init__(self, user_service: UserManagementUseCase):
        self.user_service = user_service
        
    def create_user_endpoint(self):
        data = request.get_json()
        try:
            user = self.user_service.create_user(
                email=data['email'],
                name=data['name']
            )
            return jsonify({
                'user_id': user.user_id,
                'email': user.email,
                'name': user.name
            }), 201
        except ValueError as e:
            return jsonify({'error': str(e)}), 400
            
    def get_user_endpoint(self, user_id):
        try:
            user = self.user_service.get_user(user_id)
            return jsonify({
                'user_id': user.user_id,
                'email': user.email,
                'name': user.name,
                'is_active': user.is_active
            })
        except ValueError as e:
            return jsonify({'error': str(e)}), 404

# CLI Adapter
class UserCLI:
    def __init__(self, user_service: UserManagementUseCase):
        self.user_service = user_service
        
    def create_user_command(self, email, name):
        try:
            user = self.user_service.create_user(email, name)
            print(f"User created successfully: {user.user_id}")
        except ValueError as e:
            print(f"Error: {e}")
            
    def list_user_command(self, user_id):
        try:
            user = self.user_service.get_user(user_id)
            print(f"User: {user.name} ({user.email})")
        except ValueError as e:
            print(f"Error: {e}")
```

### Secondary Adapters
```python
import sqlite3
import smtplib
from email.mime.text import MimeText

# Database Adapter
class SQLiteUserRepository(UserRepository):
    def __init__(self, db_path):
        self.db_path = db_path
        self._create_table()
        
    def _create_table(self):
        conn = sqlite3.connect(self.db_path)
        conn.execute('''
            CREATE TABLE IF NOT EXISTS users (
                user_id TEXT PRIMARY KEY,
                email TEXT UNIQUE,
                name TEXT,
                is_active BOOLEAN
            )
        ''')
        conn.commit()
        conn.close()
        
    def save(self, user):
        conn = sqlite3.connect(self.db_path)
        conn.execute('''
            INSERT OR REPLACE INTO users (user_id, email, name, is_active)
            VALUES (?, ?, ?, ?)
        ''', (user.user_id, user.email, user.name, user.is_active))
        conn.commit()
        conn.close()
        
    def find_by_id(self, user_id):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.execute(
            'SELECT user_id, email, name, is_active FROM users WHERE user_id = ?',
            (user_id,)
        )
        row = cursor.fetchone()
        conn.close()
        
        if row:
            user = User(row[0], row[1], row[2])
            user.is_active = row[3]
            return user
        return None
        
    def find_by_email(self, email):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.execute(
            'SELECT user_id, email, name, is_active FROM users WHERE email = ?',
            (email,)
        )
        row = cursor.fetchone()
        conn.close()
        
        if row:
            user = User(row[0], row[1], row[2])
            user.is_active = row[3]
            return user
        return None

# Email Service Adapter
class SMTPEmailService(EmailService):
    def __init__(self, smtp_server, smtp_port, username, password):
        self.smtp_server = smtp_server
        self.smtp_port = smtp_port
        self.username = username
        self.password = password
        
    def send_welcome_email(self, user):
        subject = "Welcome to Our Platform"
        body = f"Hello {user.name}, welcome to our platform!"
        self._send_email(user.email, subject, body)
        
    def send_deactivation_email(self, user):
        subject = "Account Deactivated"
        body = f"Hello {user.name}, your account has been deactivated."
        self._send_email(user.email, subject, body)
        
    def _send_email(self, to_email, subject, body):
        msg = MimeText(body)
        msg['Subject'] = subject
        msg['From'] = self.username
        msg['To'] = to_email
        
        with smtplib.SMTP(self.smtp_server, self.smtp_port) as server:
            server.starttls()
            server.login(self.username, self.password)
            server.send_message(msg)
```

### Dependency Injection and Wiring
```python
# Configuration and dependency injection
class ApplicationConfig:
    def __init__(self):
        # Initialize adapters
        self.user_repository = SQLiteUserRepository('users.db')
        self.email_service = SMTPEmailService(
            smtp_server='smtp.gmail.com',
            smtp_port=587,
            username='your-email@gmail.com',
            password='your-password'
        )
        self.user_domain_service = UserDomainService()
        
        # Initialize use case
        self.user_service = UserManagementService(
            self.user_repository,
            self.email_service,
            self.user_domain_service
        )
        
        # Initialize primary adapters
        self.user_controller = UserController(self.user_service)
        self.user_cli = UserCLI(self.user_service)

# Flask application setup
def create_app():
    app = Flask(__name__)
    config = ApplicationConfig()
    
    @app.route('/users', methods=['POST'])
    def create_user():
        return config.user_controller.create_user_endpoint()
        
    @app.route('/users/<user_id>')
    def get_user(user_id):
        return config.user_controller.get_user_endpoint(user_id)
        
    return app
```

## Benefits

### 1. Testability
- Easy to test business logic in isolation
- Mock external dependencies through ports
- Independent testing of adapters

### 2. Flexibility
- Easy to swap implementations (different databases, APIs)
- Support multiple input/output methods
- Technology-agnostic core business logic

### 3. Maintainability
- Clear separation of concerns
- Changes to external systems don't affect business logic
- Easier to understand and modify

### 4. Technology Independence
- Business logic not tied to specific frameworks
- Can evolve infrastructure without changing core logic
- Framework migration becomes easier

## Testing Strategy

### Unit Testing the Core
```python
import unittest
from unittest.mock import Mock

class TestUserManagementService(unittest.TestCase):
    def setUp(self):
        self.user_repository = Mock(spec=UserRepository)
        self.email_service = Mock(spec=EmailService)
        self.user_domain_service = Mock(spec=UserDomainService)
        
        self.user_service = UserManagementService(
            self.user_repository,
            self.email_service,
            self.user_domain_service
        )
        
    def test_create_user_success(self):
        # Arrange
        self.user_domain_service.is_email_unique.return_value = True
        
        # Act
        user = self.user_service.create_user('test@example.com', 'Test User')
        
        # Assert
        self.assertEqual(user.email, 'test@example.com')
        self.assertEqual(user.name, 'Test User')
        self.user_repository.save.assert_called_once()
        self.email_service.send_welcome_email.assert_called_once()
        
    def test_create_user_duplicate_email(self):
        # Arrange
        self.user_domain_service.is_email_unique.return_value = False
        
        # Act & Assert
        with self.assertRaises(ValueError):
            self.user_service.create_user('test@example.com', 'Test User')
```

### Integration Testing
```python
class TestUserManagementIntegration(unittest.TestCase):
    def setUp(self):
        # Use real implementations for integration tests
        self.user_repository = SQLiteUserRepository(':memory:')
        self.email_service = Mock(spec=EmailService)
        self.user_domain_service = UserDomainService()
        
        self.user_service = UserManagementService(
            self.user_repository,
            self.email_service,
            self.user_domain_service
        )
        
    def test_user_lifecycle(self):
        # Create user
        user = self.user_service.create_user('test@example.com', 'Test User')
        
        # Retrieve user
        retrieved_user = self.user_service.get_user(user.user_id)
        self.assertEqual(retrieved_user.email, 'test@example.com')
        
        # Update user
        updated_user = self.user_service.update_user(
            user.user_id, 
            name='Updated Name'
        )
        self.assertEqual(updated_user.name, 'Updated Name')
        
        # Deactivate user
        self.user_service.deactivate_user(user.user_id)
        deactivated_user = self.user_service.get_user(user.user_id)
        self.assertFalse(deactivated_user.is_active)
```

## Common Pitfalls

### 1. Leaky Abstractions
- Domain entities containing framework-specific code
- Ports exposing implementation details
- Business logic depending on infrastructure concerns

### 2. Over-engineering
- Creating too many layers for simple applications
- Excessive abstraction without clear benefits
- Complex dependency injection setups

### 3. Incorrect Port Design
- Ports that are too fine-grained
- Mixing input and output concerns
- Technology-specific interfaces

## When to Use Hexagonal Architecture

### Good Fit
- Complex business logic
- Multiple input/output channels
- Need for high testability
- Long-term maintainability requirements
- Team with good architectural discipline

### Not Recommended
- Simple CRUD applications
- Rapid prototyping
- Small, short-lived projects
- Teams new to architectural patterns

## Conclusion

Hexagonal Architecture provides a robust foundation for building maintainable, testable, and flexible applications. While it requires more upfront design effort, it pays dividends in terms of code quality, testability, and adaptability to changing requirements. The key is to apply it judiciously and ensure the benefits justify the additional complexity.
