# 🧪 API Testing: Types, Explanations & Code Examples

API testing is a critical part of the software development lifecycle. It ensures APIs are reliable, secure, performant, and behave as expected. Here's a breakdown of all major types of API testing, explained clearly, with examples and code snippets.

---

## 1. ✅ **Functional Testing**
### **Goal:** Ensure the API behaves according to the functional requirements.

### **Example Scenario:** Validate a POST `/login` API returns a 200 status and a token.

```python
import requests

response = requests.post("http://api.example.com/login", json={"username": "admin", "password": "admin123"})
assert response.status_code == 200
assert "token" in response.json()
```

---

## 2. 🔒 **Security Testing**
### **Goal:** Verify that the API is secure from external threats like SQL injection, XSS, etc.

### **Example Scenario:** Check for SQL injection vulnerability.

```python
malicious_input = "admin' OR '1'='1"
response = requests.post("http://api.example.com/login", json={"username": malicious_input, "password": "pass"})
assert response.status_code == 401  # Should not allow login
```

---

## 3. ⚡ **Performance Testing**
### **Goal:** Measure the speed, scalability, and responsiveness of the API.

### **Example Tool:** `locust`, `k6`, `Apache JMeter`

```python
# Example: k6 script
import http from 'k6/http';
import { sleep } from 'k6';

export default function () {
  http.get('http://api.example.com/users');
  sleep(1);
}
```

---

## 4. 🔄 **Load Testing**
### **Goal:** Determine how the API handles heavy traffic.

### **Tool:** Postman, Apache JMeter, k6

```javascript
// Example: Simulate 100 virtual users in k6
export let options = {
  vus: 100,
  duration: '30s',
};
```

---

## 5. 📦 **Validation Testing**
### **Goal:** Ensure the API returns correct status codes, headers, and payload formats.

```python
response = requests.get("http://api.example.com/user/123")
assert response.status_code == 200
assert response.headers['Content-Type'] == 'application/json'
assert "id" in response.json()
```

---

## 6. 🧪 **Unit Testing (API Layer)**
### **Goal:** Test individual functions or services independently.

```python
# test_utils.py
from utils import get_user_name

def test_get_user_name():
    assert get_user_name(5) == "John Doe"
```

---

## 7. 🧭 **UI Integration Testing**
### **Goal:** Test API response behavior via UI

```javascript
// Cypress example
cy.request("GET", "/api/products").then((response) => {
  expect(response.status).to.eq(200);
});
```

---

## 8. 🔁 **End-to-End Testing (E2E)**
### **Goal:** Test a complete flow involving multiple APIs

```python
# Create user → Login → Fetch profile

response = requests.post("/create_user", json={"name": "Ajay", "email": "ajay@example.com"})
user_id = response.json()['id']

login = requests.post("/login", json={"email": "ajay@example.com", "password": "12345"})
token = login.json()['token']

profile = requests.get(f"/profile/{user_id}", headers={"Authorization": f"Bearer {token}"})
assert profile.status_code == 200
```

---

## 9. 🔄 **Regression Testing**
### **Goal:** Ensure existing functionalities aren’t broken by new code.

Often automated through CI pipelines:

```yaml
# GitHub Action Example
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run Tests
        run: pytest
```

---

## 10. 🧰 **Mock Testing**
### **Goal:** Simulate the API when the actual service is down or incomplete.

### **Tools:** Postman Mock Server, WireMock, MSW

```json
// Postman mock response
{
  "id": "123",
  "name": "Mock User"
}
```

---

## 🧠 Summary Table

| Type              | Focus Area              | Tool/Example               |
|-------------------|--------------------------|----------------------------|
| Functional        | Business Logic           | Requests / Postman         |
| Security          | Vulnerability Checks     | OWASP ZAP / Manual         |
| Performance       | Speed Metrics            | k6 / JMeter                |
| Load              | Stress under Load        | k6 / Apache Bench          |
| Validation        | Schema, Headers          | Postman / SuperTest        |
| Unit              | Module Logic             | PyTest / Mocha             |
| UI Integration    | Frontend-Backend Sync    | Cypress                    |
| End-to-End        | Full Workflow            | Playwright / Python Script |
| Regression        | Continuous Verification  | CI/CD (GitHub Actions)     |
| Mock              | Simulated Dependencies   | Postman / WireMock         |

---

> 🧭 Keep this file as your API Testing Bible. Update it as you grow. Want to integrate these tests into a CI pipeline or build a test framework? Just ping me!

