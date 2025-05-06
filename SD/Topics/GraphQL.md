# GraphQL

## Overview
GraphQL is a query language and runtime for APIs developed by Facebook. It enables clients to request exactly the data they need, making APIs more flexible and efficient compared to traditional REST.

## Key Concepts
- **Schema:** Strongly-typed contract defining types, queries, mutations, and subscriptions.
- **Query:** Client specifies the shape and fields of the data required.
- **Mutation:** Used to modify server-side data.
- **Subscription:** Real-time updates via persistent connections (usually WebSockets).
- **Resolvers:** Functions that resolve data for each field in the schema.

## Advanced Topics
### 1. Schema Design
- Modularize schemas using schema stitching or federation.
- Use interfaces and unions for polymorphic data.
- Apply custom scalars for complex data types (e.g., DateTime, JSON).

### 2. Query Optimization
- **N+1 Problem:** Occurs when fetching nested data; solve using batching (e.g., DataLoader).
- **Query Complexity Analysis:** Limit query depth and cost to prevent abuse.
- **Caching:** Use persisted queries and response caching for performance.

### 3. Security
- **Authorization:** Field-level and type-level access control.
- **Validation:** Enforce query depth, complexity, and whitelisting.
- **Introspection Control:** Disable in production to prevent schema leakage.

### 4. Real-World Usage
- **BFF (Backend for Frontend):** Tailor APIs for different clients (web, mobile).
- **Microservices:** Use Apollo Federation or schema stitching to compose distributed schemas.
- **Developer Tooling:** GraphiQL, Apollo Studio, and Playground for interactive exploration.

### 5. Best Practices
- Keep schemas intuitive and well-documented.
- Version using deprecation, not breaking changes.
- Monitor and log query usage patterns.
- Use persisted queries to mitigate DoS risks.

### 6. Interview Questions
- How does GraphQL differ from REST?
- What is the N+1 problem and how do you solve it?
- How do you secure a GraphQL API?

### 7. Diagram
```
[Client] --(GraphQL Query)--> [GraphQL Server] --(Resolvers)--> [Data Sources (DB, REST, etc.)]
         <--(JSON Response)--
```

---
Continue to the next topic for deeper mastery!