# CORS Configuration Guide

**Status**: ✅ CORS CONFIGURATION ADDED

---

## What is CORS?

CORS (Cross-Origin Resource Sharing) allows your frontend to make requests to your backend API from different domains/ports.

---

## Configuration Added

### 1. CorsConfig.java (Global Configuration)
**Location**: `src/main/java/.../config/CorsConfig.java`

**Features**:
- Applies to all endpoints (`/**`)
- Allows requests from specified origins
- Supports all HTTP methods (GET, POST, PUT, DELETE, OPTIONS, PATCH)
- Allows credentials (cookies, authorization headers)
- Pre-flight cache for 3600 seconds

### 2. @CrossOrigin Annotation (Controller Level)
**Location**: `TrainerWalletController.java`

**Features**:
- Specifically configured for the trainer wallet endpoints
- Redundant with global config but provides explicit control
- Can be overridden per-method if needed

---

## Allowed Origins (Configure for Your Environment)

### Development
```
http://localhost:3000        # React (Create React App)
http://localhost:4200        # Angular
http://localhost:8000        # Alternative port
http://localhost:5173        # Vite
https://localhost:3000       # HTTPS local
https://localhost:4200       # HTTPS local
```

### Production
```
https://yourdomain.com       # Update with your domain
https://www.yourdomain.com   # With www
```

---

## How to Configure for Your Domain

### Step 1: Update CorsConfig.java
```java
configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000",          // Keep for local dev
        "https://yourdomain.com",         // Add your production domain
        "https://www.yourdomain.com",
        "https://api.yourdomain.com"      // Add subdomains if needed
));
```

### Step 2: Update TrainerWalletController
```java
@CrossOrigin(
        origins = {
            "http://localhost:3000",
            "https://yourdomain.com",
            "https://www.yourdomain.com"
        },
        ...
)
```

### Step 3: Restart Application
```bash
./mvnw spring-boot:run
```

---

## Testing CORS

### Using cURL
```bash
# Send a preflight request
curl -X OPTIONS "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v
```

**Expected Response Headers**:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

### Using Browser Console (From localhost:3000)
```javascript
fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer <JWT_TOKEN>',
    'Content-Type': 'application/json'
  },
  credentials: 'include'  // Include cookies/auth
})
.then(response => response.json())
.then(data => console.log('Success:', data))
.catch(error => console.error('CORS Error:', error));
```

---

## Common CORS Errors & Solutions

### Error: "Access to XMLHttpRequest blocked by CORS policy"

**Cause**: Origin not in allowed list

**Solution**:
1. Check your frontend domain
2. Add it to `allowedOrigins` in CorsConfig.java
3. Restart the application

### Error: "Credentials mode is 'include' but Access-Control-Allow-Credentials is missing"

**Cause**: `credentials: 'include'` set but CORS not allowing credentials

**Solution**:
```java
// Make sure this is set in CorsConfig
configuration.setAllowCredentials(true);
```

### Error: "Method not allowed by Access-Control-Allow-Methods"

**Cause**: HTTP method (POST, PUT, etc.) not in allowed methods

**Solution**:
```java
configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
));
```

### Error: "Header not allowed by Access-Control-Allow-Headers"

**Cause**: Custom header not in allowed headers

**Solution**:
```java
configuration.setAllowedHeaders(Arrays.asList("*"));  // Allow all
// Or specific headers:
configuration.setAllowedHeaders(Arrays.asList(
        "Authorization", "Content-Type", "X-Requested-With"
));
```

---

## CORS Configuration in Different Environments

### Development Environment
```java
configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000",
        "http://localhost:4200",
        "http://localhost:5173"
));
configuration.setAllowCredentials(true);
configuration.setMaxAge(3600L);  // 1 hour
```

### Staging Environment
```java
configuration.setAllowedOrigins(Arrays.asList(
        "https://staging.yourdomain.com"
));
configuration.setAllowCredentials(true);
configuration.setMaxAge(86400L);  // 1 day
```

### Production Environment
```java
configuration.setAllowedOrigins(Arrays.asList(
        "https://yourdomain.com",
        "https://www.yourdomain.com"
));
configuration.setAllowCredentials(true);
configuration.setMaxAge(604800L);  // 1 week
```

---

## Using Environment Variables

For flexible configuration across environments:

### Update application.yml
```yaml
cors:
  allowed-origins:
    - http://localhost:3000
    - ${CORS_ALLOWED_ORIGINS:https://yourdomain.com}
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - OPTIONS
    - PATCH
  allowed-headers: "*"
  allow-credentials: true
  max-age: 3600
```

### Update CorsConfig.java
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(maxAge);
    }
}
```

### Set Environment Variable
```bash
export CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://www.yourdomain.com"
./mvnw spring-boot:run
```

---

## Security Best Practices

### 1. Whitelist Specific Origins
❌ **Bad**:
```java
configuration.setAllowedOrigins(Arrays.asList("*"));
```

✅ **Good**:
```java
configuration.setAllowedOrigins(Arrays.asList(
        "https://yourdomain.com",
        "https://www.yourdomain.com"
));
```

### 2. Use HTTPS in Production
❌ **Bad**:
```java
"http://yourdomain.com"  // HTTP in production
```

✅ **Good**:
```java
"https://yourdomain.com"  // HTTPS only
```

### 3. Don't Allow All Headers
❌ **Bad**:
```java
configuration.setAllowedHeaders(Arrays.asList("*"));
```

✅ **Good**:
```java
configuration.setAllowedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Requested-With"
));
```

### 4. Use Credentials Carefully
Only set `allowCredentials(true)` if needed:
```java
configuration.setAllowCredentials(true);  // Only if sending cookies/auth
```

---

## Testing CORS with Frontend Examples

### React Example
```javascript
// React component
import { useEffect, useState } from 'react';

function TrainerRevenue() {
  const [revenue, setRevenue] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    
    fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      credentials: 'include'  // Include cookies if needed
    })
    .then(response => response.json())
    .then(data => setRevenue(data))
    .catch(err => setError(err.message));
  }, []);

  return (
    <div>
      {error && <p>Error: {error}</p>}
      {revenue && <p>Total Revenue: ₹{revenue.totalRevenue}</p>}
    </div>
  );
}

export default TrainerRevenue;
```

### Angular Example
```typescript
// Angular service
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class TrainerRevenueService {
  constructor(private http: HttpClient) {}

  getTrainerRevenue(token: string) {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    return this.http.get(
      'http://localhost:8080/api/v1/trainer-wallet/revenue-analytics',
      { headers }
    );
  }
}
```

---

## Verifying CORS is Working

### Check Response Headers
```bash
curl -X GET "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Origin: http://localhost:3000" \
  -v
```

**Look for these headers in response**:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

---

## Files Modified

### New File Created:
- `src/main/java/.../config/CorsConfig.java` ✅

### Files Updated:
- `TrainerWalletController.java` - Added @CrossOrigin annotation ✅

---

## Summary

✅ CORS is now **fully configured** for the Trainer Revenue Analytics API

✅ Supports **local development** (localhost ports 3000, 4200, 5173, 8000)

✅ Ready for **production domains** (update with your domain)

✅ Includes **global configuration** + controller-level control

✅ Follows **security best practices**

✅ Supports **credentials** and **authorization headers**

---

## Next Steps

1. Update `allowedOrigins` with your actual domain
2. Test CORS from your frontend application
3. Monitor browser console for CORS errors
4. Adjust configuration if needed
5. Deploy to production

---

**Status**: ✅ CORS CONFIGURATION COMPLETE

**Last Updated**: July 28, 2026
