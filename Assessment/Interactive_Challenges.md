# Interactive System Design Challenges

## Table of Contents
1. [Design Challenge Generator](#design-challenge-generator)
2. [Progressive Difficulty Levels](#progressive-difficulty-levels)
3. [Time-bound Challenges](#time-bound-challenges)
4. [Peer Review Framework](#peer-review-framework)

---

## Design Challenge Generator

### Challenge Categories

#### 🌐 Web Applications
- **URL Shortener** (e.g., bit.ly)
- **Social Media Platform** (e.g., Twitter)
- **Video Streaming** (e.g., YouTube)
- **E-commerce Platform** (e.g., Amazon)
- **Online Marketplace** (e.g., eBay)

#### 📱 Mobile Applications
- **Messaging App** (e.g., WhatsApp)
- **Ride Sharing** (e.g., Uber)
- **Food Delivery** (e.g., DoorDash)
- **Navigation System** (e.g., Google Maps)
- **Photo Sharing** (e.g., Instagram)

#### 🏢 Enterprise Systems
- **Customer Relationship Management**
- **Enterprise Resource Planning**
- **Human Resources Management**
- **Supply Chain Management**
- **Business Intelligence Platform**

#### 🎮 Real-time Systems
- **Online Gaming Platform**
- **Live Streaming Platform**
- **Collaborative Editing** (e.g., Google Docs)
- **Video Conferencing** (e.g., Zoom)
- **Trading Platform**

---

## Progressive Difficulty Levels

### Level 1: Foundation (Beginner)
**Time Allocation:** 1-2 hours
**Focus:** Basic architecture and core components

#### Challenge 1A: URL Shortener
**Requirements:**
- Shorten long URLs to short ones
- Redirect short URLs to original URLs
- Basic analytics (click count)
- Handle 100 URLs/second

**Expected Solution Components:**
- [ ] High-level architecture diagram
- [ ] API endpoints design
- [ ] Database schema
- [ ] URL encoding algorithm
- [ ] Basic error handling

**Sample Starter Questions:**
1. How would you generate unique short URLs?
2. What database would you choose and why?
3. How would you handle URL collisions?
4. What caching strategy would you implement?

#### Challenge 1B: Task Management System
**Requirements:**
- Create, update, delete tasks
- Assign tasks to users
- Set due dates and priorities
- Basic search functionality

**Expected Solution Components:**
- [ ] User authentication system
- [ ] Task CRUD operations
- [ ] Database relationships
- [ ] API design
- [ ] Basic authorization

---

### Level 2: Intermediate (System Architect)
**Time Allocation:** 3-4 hours
**Focus:** Scalability, fault tolerance, and optimization

#### Challenge 2A: Social Media Feed
**Requirements:**
- User posts and follows
- Timeline generation
- Real-time notifications
- Handle 1M users, 10K posts/second
- Global distribution

**Expected Solution Components:**
- [ ] Microservices architecture
- [ ] Database sharding strategy
- [ ] Caching layers (Redis, CDN)
- [ ] Message queue system
- [ ] Load balancing
- [ ] API rate limiting

**Advanced Considerations:**
- [ ] Feed generation algorithms (push vs pull)
- [ ] Content ranking algorithms
- [ ] Image/video storage and CDN
- [ ] Spam detection
- [ ] Privacy controls

#### Challenge 2B: E-commerce Platform
**Requirements:**
- Product catalog with search
- Shopping cart and checkout
- Order management
- Inventory tracking
- Payment processing

**Expected Solution Components:**
- [ ] Microservices breakdown
- [ ] Database design for products/orders
- [ ] Search engine integration
- [ ] Payment gateway integration
- [ ] Inventory management system
- [ ] Fraud detection

---

### Level 3: Advanced (Senior Engineer)
**Time Allocation:** 5-6 hours
**Focus:** Complex distributed systems, performance optimization

#### Challenge 3A: Video Streaming Platform
**Requirements:**
- Upload and process videos
- Stream to millions of users
- Multiple quality options
- Global content delivery
- Real-time analytics

**Expected Solution Components:**
- [ ] Video processing pipeline
- [ ] Content delivery network design
- [ ] Adaptive bitrate streaming
- [ ] Global infrastructure
- [ ] Analytics and monitoring
- [ ] Recommendation engine

**Expert-Level Considerations:**
- [ ] Video encoding optimization
- [ ] Edge computing for low latency
- [ ] ML-based content recommendations
- [ ] Advanced analytics and A/B testing
- [ ] Cost optimization strategies

#### Challenge 3B: Distributed Database System
**Requirements:**
- ACID compliance
- Horizontal scaling
- Multi-region support
- Automatic failover
- Consistent backups

**Expected Solution Components:**
- [ ] Consensus algorithm (Raft/Paxos)
- [ ] Sharding and replication strategy
- [ ] Transaction management
- [ ] Backup and recovery
- [ ] Monitoring and alerting

---

### Level 4: Expert (Principal Architect)
**Time Allocation:** 8+ hours
**Focus:** Organization-scale systems, business impact

#### Challenge 4A: Multi-tenant Cloud Platform
**Requirements:**
- Support multiple organizations
- Resource isolation and security
- Billing and metering
- Auto-scaling
- Compliance (SOC2, GDPR)

**Expected Solution Components:**
- [ ] Multi-tenancy architecture
- [ ] Identity and access management
- [ ] Resource isolation strategies
- [ ] Billing and usage tracking
- [ ] Compliance framework
- [ ] Security architecture

#### Challenge 4B: Global Payment System
**Requirements:**
- Process millions of transactions/day
- Multiple currencies and payment methods
- Fraud detection and prevention
- Regulatory compliance
- 99.99% uptime

**Expected Solution Components:**
- [ ] High-availability architecture
- [ ] Real-time fraud detection
- [ ] Currency conversion system
- [ ] Compliance and audit trails
- [ ] Disaster recovery
- [ ] Performance monitoring

---

## Time-bound Challenges

### Sprint Challenges (30 minutes)
**Focus:** Quick decision making and high-level design

#### Sprint 1: Chat System Architecture
**Requirements:**
- Real-time messaging
- Group chats
- Message history
- Online presence

**Deliverables:**
- [ ] Architecture diagram (5 min)
- [ ] Technology choices (10 min)
- [ ] Scaling strategy (10 min)
- [ ] Key challenges identification (5 min)

#### Sprint 2: Notification System
**Requirements:**
- Push notifications
- Email notifications
- SMS notifications
- Preference management

**Deliverables:**
- [ ] Component diagram
- [ ] API design
- [ ] Delivery guarantees
- [ ] Rate limiting strategy

### Interview Simulations (45 minutes)
**Format:** Standard tech company interview

#### Interview Simulation 1: Design Twitter
**Interviewer Script:**
1. **Requirements Gathering (10 min):**
   - Ask about scale, features, constraints
   - Clarify functional and non-functional requirements

2. **High-Level Design (15 min):**
   - Draw system architecture
   - Identify major components
   - Data flow explanation

3. **Deep Dive (15 min):**
   - Database schema design
   - Feed generation algorithm
   - Caching strategy

4. **Scale and Optimize (5 min):**
   - Bottleneck identification
   - Scaling solutions
   - Performance optimization

**Evaluation Criteria:**
- [ ] Clear problem understanding
- [ ] Systematic approach
- [ ] Component reasoning
- [ ] Scalability considerations
- [ ] Communication clarity

---

## Peer Review Framework

### Review Template

#### Basic Information
- **Reviewer:** [Name]
- **Design Challenge:** [Challenge Name]
- **Review Date:** [Date]
- **Time Spent on Review:** [Minutes]

#### Architecture Review (Score: /25)
**Strengths:**
- [ ] Clear component separation
- [ ] Appropriate technology choices
- [ ] Well-defined interfaces
- [ ] Scalability considerations

**Areas for Improvement:**
- [ ] Missing components
- [ ] Over-engineering
- [ ] Technology misalignment
- [ ] Scalability gaps

**Score Breakdown:**
- Component design: ___/5
- Technology choices: ___/5
- Scalability: ___/5
- Interface design: ___/5
- Overall architecture: ___/5

#### Database Design (Score: /20)
**Strengths:**
- [ ] Appropriate schema design
- [ ] Proper indexing strategy
- [ ] Data consistency approach
- [ ] Performance considerations

**Areas for Improvement:**
- [ ] Schema optimization
- [ ] Missing relationships
- [ ] Consistency model
- [ ] Query performance

#### System Scalability (Score: /25)
**Evaluation Points:**
- [ ] Load balancing strategy
- [ ] Caching layers
- [ ] Database scaling
- [ ] Bottleneck identification
- [ ] Performance optimization

#### Security & Reliability (Score: /15)
**Security:**
- [ ] Authentication/Authorization
- [ ] Data encryption
- [ ] Input validation
- [ ] Security best practices

**Reliability:**
- [ ] Fault tolerance
- [ ] Error handling
- [ ] Monitoring strategy
- [ ] Recovery procedures

#### Communication & Documentation (Score: /15)
**Evaluation:**
- [ ] Clear diagrams
- [ ] Well-explained decisions
- [ ] Trade-off analysis
- [ ] Implementation details

### Review Process

#### Step 1: Self-Review Checklist
Before peer review, complete this checklist:

**Architecture:**
- [ ] All major components identified
- [ ] Data flow clearly defined
- [ ] Technology stack justified
- [ ] Interfaces well-defined

**Scalability:**
- [ ] Performance requirements addressed
- [ ] Scaling strategy defined
- [ ] Bottlenecks identified
- [ ] Optimization opportunities noted

**Implementation:**
- [ ] Database schema designed
- [ ] API endpoints defined
- [ ] Error handling considered
- [ ] Security measures included

#### Step 2: Peer Review
**Time Allocation:** 45-60 minutes per review

**Review Process:**
1. **Initial Reading (10 min):** Understand the overall approach
2. **Detailed Analysis (30 min):** Go through each component
3. **Scoring (10 min):** Use the rubric to assign scores
4. **Feedback Writing (10 min):** Constructive feedback and suggestions

#### Step 3: Collaborative Discussion
**Format:** 30-minute discussion session

**Agenda:**
1. **Strengths Highlight (5 min):** What worked well
2. **Improvement Areas (15 min):** Detailed discussion of gaps
3. **Alternative Approaches (5 min):** Different solution strategies
4. **Learning Points (5 min):** Key takeaways for both parties

### Sample Peer Review

#### Design Challenge: URL Shortener
**Reviewer:** Alice Johnson
**Reviewee:** Bob Smith
**Challenge Level:** Beginner

**Architecture Review (Score: 22/25)**
**Strengths:**
- ✅ Clear separation between API layer, business logic, and data layer
- ✅ Appropriate choice of Redis for caching frequently accessed URLs
- ✅ Well-designed REST API with proper HTTP methods
- ✅ Good consideration of database indexing for performance

**Areas for Improvement:**
- ⚠️ Missing discussion of URL collision handling
- ⚠️ No mention of rate limiting to prevent abuse
- ⚠️ Could benefit from discussing load balancing strategies

**Database Design (Score: 18/20)**
**Strengths:**
- ✅ Efficient schema with appropriate data types
- ✅ Good indexing strategy on short_url column
- ✅ Proper normalization level

**Areas for Improvement:**
- ⚠️ Consider adding TTL for URL expiration
- ⚠️ Missing soft delete mechanism

**Overall Score: 85/100**
**Recommendation:** Strong foundation with room for production-ready enhancements

---

## Challenge Progression Tracker

### Individual Progress Dashboard
```
System Design Challenge Tracker
===============================

Challenges Completed: 8/20
Current Level: Intermediate (Level 2)

Level 1 (Foundation): 
[████████████████████] 100% (4/4 completed)

Level 2 (Intermediate): 
[████████████░░░░░░░░] 60% (3/5 completed)

Level 3 (Advanced): 
[████░░░░░░░░░░░░░░░░] 20% (1/5 completed)

Level 4 (Expert): 
[░░░░░░░░░░░░░░░░░░░░] 0% (0/6 completed)

Recent Achievements:
🎯 Completed "Social Media Feed" challenge
⭐ Scored 85+ on "E-commerce Platform" 
📈 Improved scalability design skills
👥 Received excellent peer review feedback

Next Recommended: Advanced Database System Challenge
Estimated Time to Next Level: 3-4 weeks
```

### Team/Organization Dashboard
```
Team System Design Competency
==============================

Team Members: 12
Average Level: 2.3 (Intermediate)

Skill Distribution:
Foundation (Level 1):    ██░░ 2 members (17%)
Intermediate (Level 2):  ████████ 7 members (58%)
Advanced (Level 3):      ███░ 3 members (25%)
Expert (Level 4):        ░░░░ 0 members (0%)

Team Strengths:
✅ Strong foundation in basic patterns
✅ Good collaboration and peer review
✅ Consistent practice and improvement

Growth Opportunities:
📈 Advanced distributed systems knowledge
📈 Performance optimization expertise
📈 Security architecture skills

Upcoming Milestones:
🎯 3 members approaching Level 3
🎯 Team average target: Level 2.5 by Q2
🎯 First Level 4 expert by end of year
```

---

**Remember:** The best way to learn system design is through practice, feedback, and iteration. Use these challenges to build your skills progressively and don't be afraid to make mistakes – they're valuable learning opportunities!

*Happy designing! 🚀*
