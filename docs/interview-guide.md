# System Design Interview Guide

## Overview

This comprehensive guide covers everything you need to know for system design interviews, from fundamental concepts to advanced architectural patterns. Use this as a structured approach to tackling any system design problem.

## Interview Process

### Typical Format
1. **Problem Introduction** (5 minutes)
2. **Requirements Gathering** (10 minutes)
3. **High-Level Design** (15 minutes)
4. **Detailed Design** (15 minutes)
5. **Scale & Optimize** (10 minutes)
6. **Q&A and Edge Cases** (5 minutes)

### What Interviewers Look For
- **Structured thinking**: Clear, logical approach
- **Trade-off analysis**: Understanding pros/cons of decisions
- **Scalability awareness**: How to handle growth
- **Practical knowledge**: Real-world experience
- **Communication**: Ability to explain complex concepts

## Step-by-Step Framework

### 1. Clarify Requirements (5-10 minutes)

#### Functional Requirements
- What features must the system support?
- Who are the users (internal/external)?
- What are the core use cases?
- What platforms (mobile, web, API)?

**Example Questions:**
```
- Should users be able to upload videos/images?
- Do we need real-time notifications?
- Should we support user authentication?
- Do we need analytics/reporting?
- What about content moderation?
```

#### Non-Functional Requirements
- **Scale**: How many users? Requests per second?
- **Performance**: Latency requirements? Throughput?
- **Availability**: Uptime requirements (99.9%, 99.99%)?
- **Consistency**: Strong vs eventual consistency?
- **Durability**: Data retention requirements?

**Example Questions:**
```
- How many daily/monthly active users?
- Expected read vs write ratio?
- Geographic distribution of users?
- Peak traffic patterns?
- Data size and growth rate?
```

### 2. Estimate Scale (5 minutes)

#### Calculate Key Numbers
```
Daily Active Users (DAU): 10M
Requests per user per day: 10
Total daily requests: 100M
Requests per second: 100M / (24 * 3600) ≈ 1,200 RPS
Peak traffic (3x average): 3,600 RPS

Storage:
- Text posts: 100 bytes average
- Images: 1MB average  
- Videos: 100MB average
- Daily storage growth: Calculate based on content mix
```

#### Storage Estimates Template
```python
# Example calculation for social media platform
def calculate_storage_requirements():
    daily_active_users = 10_000_000
    posts_per_user_per_day = 0.5
    
    # Text posts
    text_posts_per_day = daily_active_users * posts_per_user_per_day * 0.8
    text_storage_per_day = text_posts_per_day * 100  # bytes
    
    # Images
    image_posts_per_day = daily_active_users * posts_per_user_per_day * 0.15
    image_storage_per_day = image_posts_per_day * 1_000_000  # 1MB each
    
    # Videos
    video_posts_per_day = daily_active_users * posts_per_user_per_day * 0.05
    video_storage_per_day = video_posts_per_day * 100_000_000  # 100MB each
    
    total_daily_storage = text_storage_per_day + image_storage_per_day + video_storage_per_day
    annual_storage = total_daily_storage * 365
    
    return {
        "daily_storage_gb": total_daily_storage / (1024**3),
        "annual_storage_tb": annual_storage / (1024**4)
    }
```

### 3. High-Level Design (10-15 minutes)

#### Start Simple
Begin with a basic 3-tier architecture:

```
[Client] → [Load Balancer] → [Web Servers] → [Database]
```

#### Add Core Components
```
[Mobile App]     [Web Client]
      |               |
      └─────────┬─────┘
                |
    ┌───────────▼───────────┐
    │   Load Balancer       │
    │   (Nginx/HAProxy)     │
    └───────────┬───────────┘
                |
    ┌───────────▼───────────┐
    │   API Gateway         │
    │   (Authentication)    │
    └───────────┬───────────┘
                |
      ┌─────────┼─────────┐
      |         |         |
┌─────▼─┐ ┌─────▼─┐ ┌─────▼─┐
│User   │ │Post   │ │Media  │
│Service│ │Service│ │Service│
└─────┬─┘ └─────┬─┘ └─────┬─┘
      |         |         |
      └─────────┼─────────┘
                |
    ┌───────────▼───────────┐
    │     Databases         │
    │   (MySQL/Postgres)    │
    └───────────────────────┘
```

#### Identify Data Flow
Show how data flows through the system:
1. User makes request
2. Authentication/authorization
3. Business logic processing
4. Data persistence
5. Response generation
6. Client update

### 4. Deep Dive Components (15-20 minutes)

#### Database Design

**SQL Example (Users & Posts):**
```sql
-- Users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Posts table
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content TEXT,
    media_url VARCHAR(512),
    post_type ENUM('text', 'image', 'video'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_created (created_at)
);

-- Relationships table
CREATE TABLE user_relationships (
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id),
    FOREIGN KEY (following_id) REFERENCES users(id)
);
```

**NoSQL Example (MongoDB):**
```javascript
// User document
{
  "_id": ObjectId("..."),
  "username": "john_doe",
  "email": "john@example.com",
  "profile": {
    "name": "John Doe",
    "bio": "Software Engineer",
    "avatar_url": "https://cdn.example.com/avatars/john.jpg"
  },
  "followers_count": 1250,
  "following_count": 500,
  "created_at": ISODate("2023-01-01T00:00:00Z")
}

// Post document
{
  "_id": ObjectId("..."),
  "user_id": ObjectId("..."),
  "content": "Check out this amazing sunset!",
  "media": {
    "type": "image",
    "url": "https://cdn.example.com/images/sunset.jpg",
    "thumbnail": "https://cdn.example.com/thumbs/sunset.jpg"
  },
  "engagement": {
    "likes": 45,
    "comments": 12,
    "shares": 3
  },
  "created_at": ISODate("2023-06-01T18:30:00Z")
}
```

#### API Design

**RESTful API Example:**
```python
# User endpoints
GET    /api/v1/users/{user_id}
POST   /api/v1/users
PUT    /api/v1/users/{user_id}
DELETE /api/v1/users/{user_id}

# Posts endpoints
GET    /api/v1/posts?user_id={user_id}&limit=20&offset=0
POST   /api/v1/posts
GET    /api/v1/posts/{post_id}
PUT    /api/v1/posts/{post_id}
DELETE /api/v1/posts/{post_id}

# Feed endpoints
GET    /api/v1/users/{user_id}/feed?limit=20&cursor=abc123
GET    /api/v1/posts/trending?limit=20

# Social features
POST   /api/v1/users/{user_id}/follow
DELETE /api/v1/users/{user_id}/follow
GET    /api/v1/users/{user_id}/followers
GET    /api/v1/users/{user_id}/following

# Engagement
POST   /api/v1/posts/{post_id}/like
DELETE /api/v1/posts/{post_id}/like
POST   /api/v1/posts/{post_id}/comments
GET    /api/v1/posts/{post_id}/comments
```

#### Caching Strategy

**Multi-Level Caching:**
```python
class CacheStrategy:
    def get_user_profile(self, user_id):
        # L1: Application cache (in-memory)
        if user_id in self.app_cache:
            return self.app_cache[user_id]
        
        # L2: Redis cache
        cached_user = self.redis.get(f"user:{user_id}")
        if cached_user:
            user_data = json.loads(cached_user)
            self.app_cache[user_id] = user_data
            return user_data
        
        # L3: Database
        user_data = self.database.get_user(user_id)
        
        # Cache for future requests
        self.redis.setex(f"user:{user_id}", 3600, json.dumps(user_data))
        self.app_cache[user_id] = user_data
        
        return user_data

    def get_user_feed(self, user_id, limit=20):
        # Use Redis for feed caching
        cache_key = f"feed:{user_id}"
        cached_feed = self.redis.lrange(cache_key, 0, limit-1)
        
        if len(cached_feed) >= limit:
            return [json.loads(post) for post in cached_feed]
        
        # Generate fresh feed
        feed = self.generate_user_feed(user_id, limit)
        
        # Cache the feed
        pipe = self.redis.pipeline()
        pipe.delete(cache_key)
        for post in feed:
            pipe.lpush(cache_key, json.dumps(post))
        pipe.expire(cache_key, 1800)  # 30 minutes
        pipe.execute()
        
        return feed
```

### 5. Scale and Optimize (10-15 minutes)

#### Identify Bottlenecks
- **Database**: Query performance, connection limits
- **Memory**: Caching needs, session storage
- **CPU**: Computation-heavy operations
- **Network**: Bandwidth, latency
- **Storage**: Disk I/O, capacity

#### Scaling Solutions

**Database Scaling:**
```python
# Read replicas
class DatabaseRouter:
    def __init__(self, master_db, read_replicas):
        self.master_db = master_db
        self.read_replicas = read_replicas
        self.replica_index = 0
    
    def execute_read(self, query):
        # Route reads to replicas
        replica = self.read_replicas[self.replica_index]
        self.replica_index = (self.replica_index + 1) % len(self.read_replicas)
        return replica.execute(query)
    
    def execute_write(self, query):
        # Route writes to master
        return self.master_db.execute(query)

# Sharding strategy
def get_user_shard(user_id):
    return f"user_db_{user_id % NUM_SHARDS}"

def get_post_shard(post_id):
    return f"post_db_{post_id % NUM_SHARDS}"
```

**Microservices Architecture:**
```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ User        │  │ Post        │  │ Media       │
│ Service     │  │ Service     │  │ Service     │
└─────────────┘  └─────────────┘  └─────────────┘
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ Feed        │  │ Notification│  │ Analytics   │
│ Service     │  │ Service     │  │ Service     │
└─────────────┘  └─────────────┘  └─────────────┘
```

**CDN and Media Handling:**
```python
class MediaService:
    def upload_media(self, file, user_id):
        # Generate unique filename
        file_id = str(uuid.uuid4())
        file_extension = file.filename.split('.')[-1]
        filename = f"{file_id}.{file_extension}"
        
        # Upload to cloud storage
        storage_url = self.cloud_storage.upload(filename, file.content)
        
        # Generate CDN URL
        cdn_url = f"https://cdn.example.com/{filename}"
        
        # Store metadata
        media_record = {
            "id": file_id,
            "user_id": user_id,
            "filename": filename,
            "storage_url": storage_url,
            "cdn_url": cdn_url,
            "file_size": len(file.content),
            "mime_type": file.content_type,
            "created_at": datetime.now()
        }
        self.database.insert("media", media_record)
        
        return cdn_url
```

## Common Interview Questions

### 1. Design a URL Shortener (Bit.ly)
**Key Points:**
- Base62 encoding for short URLs
- Database schema for URL mappings
- Caching for popular URLs
- Analytics and click tracking
- Custom aliases support

### 2. Design a Chat System (WhatsApp)
**Key Points:**
- WebSocket connections for real-time messaging
- Message queuing and delivery
- Online presence tracking
- Message encryption
- Group chat implementation

### 3. Design a Social Media Feed (Twitter)
**Key Points:**
- Fan-out strategies (push vs pull)
- Timeline generation algorithms
- Celebrity user handling
- Content ranking and recommendation
- Media storage and CDN

### 4. Design a Video Streaming Service (YouTube)
**Key Points:**
- Video upload and encoding pipeline
- CDN and adaptive bitrate streaming
- Metadata storage and search
- Recommendation system
- Analytics and monetization

### 5. Design a Ride-Sharing Service (Uber)
**Key Points:**
- Real-time location tracking
- Driver-rider matching algorithms
- Trip management and state machines
- Dynamic pricing
- Payment processing

## Best Practices

### Do's
- **Start simple** and evolve the design
- **Ask clarifying questions** throughout
- **Explain your reasoning** for design decisions
- **Consider trade-offs** explicitly
- **Think about failure scenarios**
- **Draw diagrams** to visualize architecture
- **Use real numbers** in your calculations

### Don'ts
- **Don't jump into details** immediately
- **Don't design for extreme scale** from the start
- **Don't ignore the interviewer's hints**
- **Don't forget about data consistency**
- **Don't overlook security considerations**
- **Don't overcomplicate** the initial design

### Time Management
- **25%** - Requirements and estimation
- **35%** - High-level design
- **30%** - Detailed design
- **10%** - Scaling and optimization

## Study Resources

### Books
- "Designing Data-Intensive Applications" by Martin Kleppmann
- "System Design Interview" by Alex Xu
- "Building Microservices" by Sam Newman

### Online Resources
- High Scalability blog
- AWS Architecture Center
- Google Cloud Architecture Framework
- System design primer (GitHub)

### Practice Platforms
- Pramp
- InterviewBit
- LeetCode System Design
- Grokking the System Design Interview

## Common Patterns Summary

### Scalability Patterns
- **Load Balancing**: Distribute traffic across multiple servers
- **Horizontal Scaling**: Add more servers to handle increased load
- **Caching**: Store frequently accessed data in fast storage
- **CDN**: Geographically distribute static content
- **Database Sharding**: Partition data across multiple databases

### Reliability Patterns
- **Circuit Breaker**: Prevent cascade failures
- **Retry with Backoff**: Handle transient failures
- **Bulkhead**: Isolate critical resources
- **Health Checks**: Monitor system component health
- **Graceful Degradation**: Maintain core functionality during failures

### Consistency Patterns
- **ACID Transactions**: Strong consistency for critical operations
- **Eventual Consistency**: Accept temporary inconsistency for availability
- **CQRS**: Separate read and write models
- **Event Sourcing**: Store events rather than current state
- **Saga Pattern**: Manage distributed transactions

Remember: System design interviews are about demonstrating your thought process and engineering judgment, not memorizing perfect solutions. Focus on clear communication and structured thinking!
