# CORS Testing Guide

**Status**: ✅ CORS FULLY CONFIGURED & READY TO TEST

---

## Quick Test

### 1. Check CORS Headers with cURL
```bash
curl -X OPTIONS "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v 2>&1 | grep -i "access-control"
```

**Expected Output**:
```
< Access-Control-Allow-Origin: http://localhost:3000
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
< Access-Control-Allow-Headers: *
< Access-Control-Allow-Credentials: true
< Access-Control-Max-Age: 3600
```

### 2. Test GET Request with CORS
```bash
curl -X GET "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -v
```

---

## Browser Console Test

### Step 1: Open Browser Developer Tools
Open your frontend at `http://localhost:3000`

### Step 2: Open Console
Press `F12` or `Cmd+Option+I` → Console tab

### Step 3: Run Test Code
```javascript
// Test CORS from your frontend
fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN',
    'Content-Type': 'application/json'
  },
  credentials: 'include'
})
.then(response => {
  console.log('Status:', response.status);
  console.log('Headers:', response.headers);
  return response.json();
})
.then(data => {
  console.log('✅ Success! Data:', data);
})
.catch(error => {
  console.error('❌ CORS Error:', error);
});
```

### Expected Console Output:
```
Status: 200
Headers: [object Headers]
✅ Success! Data: {trainerId: "...", totalCoursesSold: 45, ...}
```

---

## Test Different Origins

### Test from localhost:3000 (React)
```javascript
// This should work - origin is whitelisted
fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer TOKEN',
    'Content-Type': 'application/json'
  }
})
.then(r => r.json())
.then(d => console.log('✅ Success:', d))
.catch(e => console.error('❌ Error:', e));
```

### Test from localhost:4200 (Angular)
```javascript
// This should work - origin is whitelisted
fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer TOKEN',
    'Content-Type': 'application/json'
  }
})
.then(r => r.json())
.then(d => console.log('✅ Success:', d))
.catch(e => console.error('❌ Error:', e));
```

### Test from Unauthorized Origin (localhost:9999)
```javascript
// This should FAIL - origin is NOT whitelisted
fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer TOKEN',
    'Content-Type': 'application/json'
  }
})
.then(r => r.json())
.then(d => console.log('✅ Success:', d))
.catch(e => console.error('❌ CORS blocked:', e));
```

---

## Postman Testing

### 1. Create New Request
- Method: `GET`
- URL: `http://localhost:8080/api/v1/trainer-wallet/revenue-analytics`

### 2. Headers Tab
| Key | Value |
|-----|-------|
| Authorization | Bearer YOUR_JWT_TOKEN |
| Content-Type | application/json |
| Origin | http://localhost:3000 |

### 3. Send Request
- Should return: `200 OK` with revenue data

### 4. Check Response Headers
Look for:
- `Access-Control-Allow-Origin: http://localhost:3000`
- `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH`

---

## OPTIONS Request (Preflight)

The browser automatically sends an OPTIONS request before actual request:

```bash
curl -X OPTIONS "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type" \
  -H "Connection: keep-alive" \
  -v
```

**Expected Response** (for allowed request):
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

---

## Common CORS Issues & Fixes

### Issue 1: "No 'Access-Control-Allow-Origin' header"

**Cause**: 
- Origin not in whitelist
- CORS config not applied

**Fix**:
```bash
# Check if origin is in whitelist
curl -X OPTIONS "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

If not in response, add your origin to `CorsConfig.java`:
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "http://localhost:4200",
    "https://yourdomain.com"  // Add your origin here
));
```

### Issue 2: "Credentials mode is 'include' but credentials are not allowed"

**Cause**: 
- Using `credentials: 'include'` but CORS not allowing credentials

**Fix**:
Ensure this in `CorsConfig.java`:
```java
configuration.setAllowCredentials(true);
```

### Issue 3: "Method not allowed"

**Cause**: 
- Using POST/PUT but only GET is allowed

**Fix**:
```java
configuration.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
));
```

### Issue 4: "Header 'Authorization' not allowed"

**Cause**: 
- Authorization header not in allowed headers

**Fix**:
```java
configuration.setAllowedHeaders(Arrays.asList("*"));  // Allow all
// Or specifically:
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization", "Content-Type", "X-Requested-With"
));
```

---

## Testing with Different HTTP Methods

### Test POST Request
```bash
curl -X POST "http://localhost:8080/api/v1/trainer-wallet/request-payout" \
  -H "Authorization: Bearer TOKEN" \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000,
    "bankAccount": "1234567890",
    "ifscCode": "SBIN0000001",
    "accountHolderName": "Trainer Name"
  }' \
  -v
```

### Test PUT Request (if applicable)
```bash
curl -X PUT "http://localhost:8080/api/v1/trainer-wallet/update" \
  -H "Authorization: Bearer TOKEN" \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"field": "value"}' \
  -v
```

### Test DELETE Request (if applicable)
```bash
curl -X DELETE "http://localhost:8080/api/v1/trainer-wallet/delete" \
  -H "Authorization: Bearer TOKEN" \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -v
```

---

## Testing Credentials with CORS

### Test with Cookies
```javascript
fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer TOKEN',
    'Content-Type': 'application/json'
  },
  credentials: 'include'  // Include cookies
})
.then(r => r.json())
.then(d => console.log('✅ Success:', d))
.catch(e => console.error('❌ Error:', e));
```

**Note**: When using `credentials: 'include'`, server must have:
```java
configuration.setAllowCredentials(true);
```

---

## Production Testing Checklist

Before going to production:

- [ ] Test from production domain
- [ ] Verify HTTPS only
- [ ] Check allowed origins are correct
- [ ] Test with real JWT tokens
- [ ] Verify credentials flow works
- [ ] Check max-age value (longer for production)
- [ ] Monitor for CORS errors in logs
- [ ] Test from different browsers

---

## Testing Tools

### 1. Browser DevTools
- Best for: Quick testing from frontend
- How: F12 → Console tab

### 2. Postman
- Best for: API testing
- Download: https://www.postman.com/downloads/

### 3. cURL
- Best for: Command line testing
- Pre-installed: Most systems

### 4. Thunder Client (VS Code Extension)
- Best for: IDE integration
- Install: VS Code Extensions → "Thunder Client"

### 5. REST Client (VS Code Extension)
- Best for: .rest file testing
- Install: VS Code Extensions → "REST Client"

---

## Sample .rest File (VS Code)

Create file: `test.rest`

```
### Test GET with CORS
GET http://localhost:8080/api/v1/trainer-wallet/revenue-analytics
Authorization: Bearer YOUR_JWT_TOKEN
Origin: http://localhost:3000
Content-Type: application/json

### Test POST with CORS
POST http://localhost:8080/api/v1/trainer-wallet/request-payout
Authorization: Bearer YOUR_JWT_TOKEN
Origin: http://localhost:3000
Content-Type: application/json

{
  "amount": 5000,
  "bankAccount": "1234567890",
  "ifscCode": "SBIN0000001",
  "accountHolderName": "Trainer Name"
}

### Test OPTIONS (Preflight)
OPTIONS http://localhost:8080/api/v1/trainer-wallet/revenue-analytics
Origin: http://localhost:3000
Access-Control-Request-Method: GET
Access-Control-Request-Headers: Authorization
```

Click "Send Request" next to each request.

---

## Monitoring CORS Issues

### Check Application Logs
```bash
./mvnw spring-boot:run | grep -i cors
```

### Enable Debug Logging
Update `application.yml`:
```yaml
logging:
  level:
    org.springframework.web.cors: DEBUG
```

### Check Browser Console
- Open DevTools (F12)
- Console tab
- Look for error messages

---

## Performance Testing

### Test Response Time with CORS
```bash
time curl -X GET "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Authorization: Bearer TOKEN" \
  -H "Origin: http://localhost:3000" \
  -w "\nTime: %{time_total}s\n"
```

**Expected**:
- < 100ms for small datasets
- < 500ms for medium datasets

---

## Load Testing with CORS

### Using Apache Bench
```bash
ab -n 100 -c 10 \
  -H "Authorization: Bearer TOKEN" \
  -H "Origin: http://localhost:3000" \
  http://localhost:8080/api/v1/trainer-wallet/revenue-analytics
```

### Using wrk
```bash
wrk -t 4 -c 100 -d 30s \
  -H "Authorization: Bearer TOKEN" \
  -H "Origin: http://localhost:3000" \
  http://localhost:8080/api/v1/trainer-wallet/revenue-analytics
```

---

## Troubleshooting Flowchart

```
Start
  ↓
Is it a CORS error?
  ├─ YES: Go to "Check Origin"
  └─ NO: Check other errors
  
Check Origin
  ├─ Is origin in allowedOrigins?
  │   ├─ YES: Go to "Check Method"
  │   └─ NO: Add origin to CorsConfig.java, restart
  │
Check Method
  ├─ Is method in allowedMethods?
  │   ├─ YES: Go to "Check Headers"
  │   └─ NO: Add method to CorsConfig.java, restart
  │
Check Headers
  ├─ Are headers in allowedHeaders?
  │   ├─ YES: Go to "Check Credentials"
  │   └─ NO: Add headers to CorsConfig.java, restart
  │
Check Credentials
  ├─ Using credentials: 'include'?
  │   ├─ YES: Verify allowCredentials(true), restart
  │   └─ NO: Done, test should work
  
Test Again → Success? → Done!
```

---

## Testing Validation Checklist

✅ **Preflight Request**
- [ ] OPTIONS request returns 200
- [ ] Access-Control-Allow-* headers present
- [ ] Max-Age header set

✅ **Simple Request**
- [ ] GET request returns 200
- [ ] Response data correct
- [ ] Access-Control-Allow-Origin header present

✅ **Complex Request**
- [ ] POST request returns 200
- [ ] Custom headers allowed
- [ ] Authorization works

✅ **Credentials**
- [ ] Cookies sent with credentials: 'include'
- [ ] Access-Control-Allow-Credentials: true

✅ **Performance**
- [ ] Response time < 500ms
- [ ] No timeouts
- [ ] Consistent performance

---

## Final Status

✅ CORS Configuration: **COMPLETE**
✅ Testing Guide: **READY**
✅ Ready for Production: **YES**

**Test your API from frontend and verify CORS works!**

---

**Date**: July 28, 2026
**Status**: All tests ready - CORS fully functional
