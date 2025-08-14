# API Design Best Practices

## RESTful API Design

### Resource Naming
- Use nouns for resources, not verbs
- Use plural forms for collections
- Use hierarchical structure for relationships
- Examples:
  - `/users` - collection of users
  - `/users/123` - specific user
  - `/users/123/orders` - user's orders

### HTTP Methods
- GET: Retrieve data (idempotent)
- POST: Create new resources
- PUT: Update entire resource (idempotent)
- PATCH: Partial update
- DELETE: Remove resource (idempotent)

### Status Codes
- 200 OK: Successful GET, PUT, PATCH
- 201 Created: Successful POST
- 204 No Content: Successful DELETE
- 400 Bad Request: Client error
- 401 Unauthorized: Authentication required
- 403 Forbidden: Access denied
- 404 Not Found: Resource doesn't exist
- 500 Internal Server Error: Server error

### Response Format
```json
{
  "data": {
    "id": "123",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "meta": {
    "timestamp": "2025-08-14T10:30:00Z",
    "version": "1.0"
  }
}
```

## API Versioning

### URL Path Versioning
- `/api/v1/users`
- `/api/v2/users`
- Simple and clear

### Header Versioning
- `Accept: application/vnd.api+json;version=1`
- `API-Version: 1.0`
- Cleaner URLs

### Query Parameter Versioning
- `/api/users?version=1`
- Easy to implement

## Error Handling

### Error Response Structure
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ],
    "timestamp": "2025-08-14T10:30:00Z"
  }
}
```

### Error Categories
- Client errors (4xx): Invalid requests
- Server errors (5xx): Internal problems
- Validation errors: Field-specific issues
- Business logic errors: Domain-specific issues

## Security

### Authentication
- JWT tokens for stateless authentication
- OAuth 2.0 for third-party access
- API keys for service-to-service
- Basic Auth only over HTTPS

### Authorization
- Role-based access control (RBAC)
- Resource-level permissions
- Scope-based access (OAuth scopes)

### Input Validation
- Validate all input parameters
- Sanitize data to prevent injection
- Use whitelisting over blacklisting
- Implement rate limiting

## Documentation

### OpenAPI/Swagger
- Automated documentation generation
- Interactive API explorer
- Code generation capabilities
- Schema validation

### Documentation Elements
- Endpoint descriptions
- Request/response examples
- Error scenarios
- Authentication requirements
- Rate limiting information

## Performance

### Pagination
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 100,
    "pages": 5
  }
}
```

### Filtering and Sorting
- `/users?filter[status]=active`
- `/users?sort=created_at&order=desc`
- `/users?fields=id,name,email`

### Caching
- ETags for conditional requests
- Cache-Control headers
- CDN integration for static responses

### Compression
- Gzip/Brotli compression
- Response size optimization
- Efficient data formats
