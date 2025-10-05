# System Design Interview Preparation Guide

## Overview

System design interviews assess your ability to design large-scale distributed systems. This guide provides a structured approach to tackle any system design question with confidence.

## Interview Process

### Typical Interview Structure (45-60 minutes)
1. **Problem Clarification** (5-10 minutes)
2. **Requirements Gathering** (5-10 minutes)  
3. **Capacity Estimation** (5-10 minutes)
4. **High-Level Design** (15-20 minutes)
5. **Detailed Design** (10-15 minutes)
6. **Scale & Optimization** (5-10 minutes)

## Step-by-Step Framework

### 1. Problem Clarification (5-10 minutes)

**Ask clarifying questions to understand the scope:**

#### Core Functionality Questions
- What are the main features we need to support?
- Who are the users of this system?
- What are the core use cases?
- Are there any specific features that are out of scope?

#### Example for "Design Twitter":
```
Interviewer: "Design a Twitter-like social media platform"

Your Questions:
- Should we support posting tweets, following users, and viewing feeds?
- Do we need to support media uploads (images, videos)?
- Should we include features like direct messaging, notifications?
- Are we focusing on the core feed functionality or the entire platform?
- Do we need to support trending topics, hashtags, mentions?
```

### 2. Requirements Gathering (5-10 minutes)

**Define functional and non-functional requirements:**

#### Functional Requirements
- What the system should do
- Core features and user interactions
- APIs and user workflows

#### Non-Functional Requirements
- **Scale**: How many users, requests per second?
- **Performance**: Latency requirements (< 200ms for reads?)
- **Availability**: Uptime requirements (99.9%?)
- **Consistency**: Strong vs. eventual consistency needs
- **Reliability**: Data durability and backup needs

#### Example for Twitter:
```
Functional Requirements:
✅ Users can post tweets (280 characters)
✅ Users can follow other users  
✅ Users can view their timeline (following + own tweets)
✅ Users can view user profiles
❌ Direct messaging (out of scope)
❌ Media uploads (out of scope)

Non-Functional Requirements:
- 500M daily active users
- 100M tweets per day
- Read-heavy system (10:1 read/write ratio)
- Timeline should load within 200ms
- 99.9% availability
- Eventual consistency acceptable for feeds
```

### 3. Capacity Estimation (5-10 minutes)

**Calculate system scale and resources needed:**

#### Traffic Estimates
```
Daily Active Users (DAU): 500M
Average tweets per user per day: 0.2 (100M tweets / 500M users)
Average timeline views per user per day: 20

Write QPS: 100M tweets / 86400 seconds ≈ 1,200 QPS
Read QPS: 500M users × 20 views / 86400 seconds ≈ 115,000 QPS

Peak QPS (3x average):
- Write: 3,600 QPS  
- Read: 345,000 QPS
```

#### Storage Estimates
```
Tweet Storage:
- Average tweet size: 280 chars × 2 bytes = 560 bytes
- Metadata (user_id, timestamp, etc.): 100 bytes
- Total per tweet: ~700 bytes
- Daily: 100M tweets × 700 bytes = 70 GB/day
- Yearly: 70 GB × 365 = ~25 TB/year

User Data:
- 500M users × 1KB per user = 500 GB

Total Storage (3 years): 25 TB × 3 + 500 GB ≈ 75 TB
```

#### Bandwidth Estimates
```
Write Bandwidth: 1,200 QPS × 700 bytes = 840 KB/s
Read Bandwidth: 115,000 QPS × 700 bytes = 80 MB/s
```

### 4. High-Level Design (15-20 minutes)

**Create the overall system architecture:**

#### Basic Architecture Components
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client    │───▶│Load Balancer│───▶│Web Servers  │
│   Apps      │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                   ┌─────────────────────────────┼─────────────────────────────┐
                   │                             │                             │
                   ▼                             ▼                             ▼
            ┌─────────────┐              ┌─────────────┐              ┌─────────────┐
            │ Tweet       │              │ Timeline    │              │ User        │
            │ Service     │              │ Service     │              │ Service     │
            └─────────────┘              └─────────────┘              └─────────────┘
                   │                             │                             │
                   ▼                             ▼                             ▼
            ┌─────────────┐              ┌─────────────┐              ┌─────────────┐
            │ Tweet DB    │              │ Timeline    │              │ User DB     │
            │ (MySQL)     │              │ Cache       │              │ (MySQL)     │
            └─────────────┘              │ (Redis)     │              └─────────────┘
                                         └─────────────┘
```

#### API Design
```
POST /api/v1/tweets
{
  "user_id": 12345,
  "content": "Hello World!",
  "timestamp": 1640995200
}

GET /api/v1/timeline/{user_id}?limit=20&offset=0
Response: {
  "tweets": [...],
  "next_page_token": "abc123"
}

POST /api/v1/follow
{
  "follower_id": 12345,
  "followee_id": 67890
}
```

### 5. Detailed Design (10-15 minutes)

**Deep dive into critical components:**

#### Database Schema
```sql
-- Users table
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP,
    INDEX idx_username (username)
);

-- Tweets table  
CREATE TABLE tweets (
    tweet_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    content TEXT,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_user_created (user_id, created_at)
);

-- Follows table
CREATE TABLE follows (
    follower_id BIGINT,
    followee_id BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY (follower_id, followee_id),
    INDEX idx_follower (follower_id),
    INDEX idx_followee (followee_id)
);
```

#### Timeline Generation
```
Option 1: Pull Model (Timeline on Read)
- Generate timeline when user requests it
- Query tweets from all users they follow
- Pros: Less storage, works for users with many follows
- Cons: Slow for active users, high read latency

Option 2: Push Model (Timeline on Write)  
- Pre-compute timeline when tweet is posted
- Store timeline in cache/database
- Pros: Fast reads, good for most users
- Cons: High storage, expensive for celebrity users

Option 3: Hybrid Model
- Push for normal users
- Pull for celebrity users (>1M followers)
- Best of both approaches
```

#### Caching Strategy
```
Timeline Cache (Redis):
- Key: user_id
- Value: List of tweet_ids (sorted by timestamp)
- TTL: 1 hour
- Cache hit ratio target: 80%

Tweet Cache (Redis):
- Key: tweet_id  
- Value: Tweet object (content, user_id, timestamp)
- TTL: 24 hours
- Cache hit ratio target: 95%
```

### 6. Scale & Optimization (5-10 minutes)

**Address bottlenecks and scaling challenges:**

#### Database Scaling
```
Horizontal Scaling:
- Shard users database by user_id
- Shard tweets database by user_id or timestamp
- Use consistent hashing for even distribution

Master-Slave Replication:
- Master for writes
- Multiple read replicas for reads
- Route reads to nearest replica
```

#### Caching Improvements
```
CDN for Static Content:
- Profile images, static assets
- Reduce server load and improve global latency

Multi-level Caching:
- L1: Application cache (in-memory)
- L2: Redis cluster
- L3: Database query cache
```

#### Additional Optimizations
```
Message Queues:
- Async processing of timeline updates
- Handle traffic spikes gracefully

Search & Analytics:
- Elasticsearch for tweet search
- Data pipeline for analytics and ML

Monitoring & Alerting:
- Track key metrics (latency, error rates, QPS)
- Set up alerts for SLA violations
```

## Common System Design Questions

### 1. Design URL Shortener (like bit.ly)
**Key Focus**: Encoding algorithms, caching, analytics

### 2. Design Chat System (like WhatsApp)
**Key Focus**: Real-time messaging, WebSockets, message ordering

### 3. Design News Feed (like Facebook)
**Key Focus**: Timeline generation, content ranking, personalization

### 4. Design Video Streaming (like YouTube)
**Key Focus**: CDN, video encoding, storage optimization

### 5. Design Search Engine (like Google)
**Key Focus**: Web crawling, indexing, ranking algorithms

### 6. Design Ride Sharing (like Uber)
**Key Focus**: Geolocation, matching algorithms, real-time updates

### 7. Design Gaming Leaderboard
**Key Focus**: Real-time rankings, data consistency, high throughput

### 8. Design Notification System
**Key Focus**: Push notifications, delivery guarantees, rate limiting

## Key Design Patterns & Concepts

### Scalability Patterns
- **Load Balancing**: Distribute traffic across servers
- **Horizontal Scaling**: Add more servers vs. upgrading hardware  
- **Database Sharding**: Split data across multiple databases
- **Caching**: Store frequently accessed data in memory
- **CDN**: Geographically distributed content delivery

### Reliability Patterns
- **Replication**: Keep multiple copies of data
- **Circuit Breaker**: Prevent cascading failures
- **Bulkhead**: Isolate critical resources
- **Rate Limiting**: Control request rates to prevent overload
- **Health Checks**: Monitor service availability

### Performance Patterns
- **Connection Pooling**: Reuse database connections
- **Batch Processing**: Process data in groups
- **Async Processing**: Non-blocking operations
- **Read Replicas**: Separate read and write operations
- **Materialized Views**: Pre-computed query results

## Common Pitfalls to Avoid

### ❌ Don't Start Coding Immediately
- Always clarify requirements first
- Understand the problem scope
- Ask questions to guide your design

### ❌ Don't Over-Engineer
- Start simple, then add complexity
- Focus on core requirements first
- Don't add features not asked for

### ❌ Don't Ignore Non-Functional Requirements
- Consider scale, performance, availability
- Discuss trade-offs explicitly
- Validate your estimates

### ❌ Don't Design in Isolation
- Consider client applications
- Think about operational concerns
- Discuss monitoring and maintenance

## Interview Tips

### ✅ Communication
- Think out loud
- Explain your reasoning
- Ask for feedback regularly
- Draw diagrams to illustrate concepts

### ✅ Structure
- Follow the framework consistently
- Manage your time well
- Prioritize core components
- Be ready to deep dive into any part

### ✅ Technical Depth
- Know when to use SQL vs NoSQL
- Understand caching strategies
- Be familiar with message queues
- Know basic capacity planning

### ✅ Business Understanding
- Consider user experience
- Think about cost implications
- Understand operational complexity
- Discuss monitoring and metrics

## Study Resources

### Books
- "Designing Data-Intensive Applications" by Martin Kleppmann
- "System Design Interview" by Alex Xu
- "Building Microservices" by Sam Newman

### Online Resources
- High Scalability blog
- Engineering blogs (Netflix, Uber, LinkedIn)
- System design primer on GitHub
- Distributed systems papers

### Practice Platforms
- Pramp (mock interviews)
- LeetCode system design
- InterviewBit system design
- Educative.io system design course

## Final Advice

1. **Practice Regularly**: Do mock interviews and draw architectures
2. **Learn from Real Systems**: Study how companies build at scale  
3. **Understand Trade-offs**: Every design decision has pros and cons
4. **Stay Current**: Follow engineering blogs and new technologies
5. **Focus on Fundamentals**: Master the basics before advanced topics

Remember: System design interviews test your ability to think through complex problems systematically. Focus on the process, communicate clearly, and show your thought process. Good luck!
