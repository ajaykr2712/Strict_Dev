# Backend Security Best Practices

## Input Validation
- Always validate and sanitize user input to prevent injection attacks.

## Authentication & Authorization
- Use strong authentication (OAuth, JWT, etc.).
- Enforce role-based access control.

## Data Protection
- Encrypt sensitive data at rest and in transit.
- Store secrets securely (e.g., environment variables, vaults).

## Secure Dependencies
- Keep dependencies up to date.
- Monitor for vulnerabilities (e.g., Dependabot, Snyk).

## Logging & Monitoring
- Log security events and monitor for suspicious activity.

## Principle of Least Privilege
- Grant only the permissions necessary for each component.

---
See [Backend_Engineering_Principles.md](Backend_Engineering_Principles.md) for more.
