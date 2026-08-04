# Trainer Revenue Analytics API Documentation

## Overview
The Trainer Revenue Analytics API provides trainers with comprehensive insights into their earnings, including total courses sold, resources sold, revenue breakdown by course/resource, and monthly revenue trends.

---

## API Endpoint

### Get Trainer Revenue Analytics

**Endpoint:** `GET /api/v1/trainer-wallet/revenue-analytics`

**Authentication:** Required (Bearer Token via Authorization header)

**Rate Limiting:** Standard Endpoint (Resilience4j)

**HTTP Method:** GET

---

## Request

### Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### Path Parameters
None (Trainer ID is extracted from JWT token)

### Query Parameters
None

### Request Body
No body required

---

## Response

### Success Response (200 OK)
```json
{
  "trainerId": "trainer-123",
  "totalCoursesSold": 45,
  "totalResourcesSold": 28,
  "totalTransactions": 73,
  "totalRevenue": 45000.00,
  "balance": 25000.00,
  "pendingPayout": 10000.00,
  "totalWithdrawn": 10000.00,
  "lastUpdated": "2026-07-28T10:30:45",
  "courseRevenue": [
    {
      "courseId": "course-001",
      "courseName": "Course course-001",
      "unitsSold": 15,
      "revenue": 15000.00
    },
    {
      "courseId": "course-002",
      "courseName": "Course course-002",
      "unitsSold": 30,
      "revenue": 30000.00
    }
  ],
  "resourceRevenue": [
    {
      "resourceId": "resource-001",
      "resourceName": "Resource resource-001",
      "unitsSold": 20,
      "revenue": 8000.00
    },
    {
      "resourceId": "resource-002",
      "resourceName": "Resource resource-002",
      "unitsSold": 8,
      "revenue": 3200.00
    }
  ],
  "monthlyBreakdown": [
    {
      "month": "2026-07",
      "amount": 12000.00,
      "transactionCount": 15
    },
    {
      "month": "2026-06",
      "amount": 18000.00,
      "transactionCount": 22
    },
    {
      "month": "2026-05",
      "amount": 15000.00,
      "transactionCount": 18
    }
  ]
}
```

### Error Response (401 Unauthorized)
```json
{
  "error": "Invalid or expired token"
}
```

### Error Response (404 Not Found)
```json
{
  "error": "Wallet not found for trainer: trainer-123"
}
```

---

## Response Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `trainerId` | String | Unique identifier of the trainer |
| `totalCoursesSold` | Long | Total number of courses sold by the trainer |
| `totalResourcesSold` | Long | Total number of resources sold by the trainer |
| `totalTransactions` | Long | Total number of earning transactions |
| `totalRevenue` | BigDecimal | Total revenue earned (80% of sales after platform fee) |
| `balance` | BigDecimal | Current wallet balance available for payout |
| `pendingPayout` | BigDecimal | Amount in pending payout requests |
| `totalWithdrawn` | BigDecimal | Total amount already withdrawn via approved payouts |
| `lastUpdated` | LocalDateTime | Last time the wallet was updated |
| `courseRevenue[]` | Array | Array of course-wise revenue breakdown |
| `courseRevenue[].courseId` | String | Course identifier |
| `courseRevenue[].courseName` | String | Course name |
| `courseRevenue[].unitsSold` | Long | Number of times this course was purchased |
| `courseRevenue[].revenue` | BigDecimal | Total revenue from this course |
| `resourceRevenue[]` | Array | Array of resource-wise revenue breakdown |
| `resourceRevenue[].resourceId` | String | Resource identifier |
| `resourceRevenue[].resourceName` | String | Resource name |
| `resourceRevenue[].unitsSold` | Long | Number of times this resource was purchased |
| `resourceRevenue[].revenue` | BigDecimal | Total revenue from this resource |
| `monthlyBreakdown[]` | Array | Array of monthly revenue trends |
| `monthlyBreakdown[].month` | String | Month in YYYY-MM format |
| `monthlyBreakdown[].amount` | BigDecimal | Revenue earned in that month |
| `monthlyBreakdown[].transactionCount` | Long | Number of transactions in that month |

---

## Usage Examples

### cURL Example
```bash
curl -X GET "http://localhost:8080/api/v1/trainer-wallet/revenue-analytics" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### JavaScript/Fetch Example
```javascript
const response = await fetch('http://localhost:8080/api/v1/trainer-wallet/revenue-analytics', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const revenueData = await response.json();
console.log(revenueData);
```

### Python/Requests Example
```python
import requests

headers = {
    'Authorization': f'Bearer {token}',
    'Content-Type': 'application/json'
}

response = requests.get(
    'http://localhost:8080/api/v1/trainer-wallet/revenue-analytics',
    headers=headers
)

revenue_data = response.json()
print(revenue_data)
```

### React Example
```javascript
import React, { useEffect, useState } from 'react';

function TrainerRevenueAnalytics({ token }) {
  const [revenue, setRevenue] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRevenueAnalytics = async () => {
      try {
        const response = await fetch(
          'http://localhost:8080/api/v1/trainer-wallet/revenue-analytics',
          {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          }
        );
        
        if (!response.ok) {
          throw new Error('Failed to fetch revenue analytics');
        }
        
        const data = await response.json();
        setRevenue(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchRevenueAnalytics();
  }, [token]);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2>Trainer Revenue Dashboard</h2>
      <div>
        <p>Total Revenue: ₹{revenue.totalRevenue}</p>
        <p>Courses Sold: {revenue.totalCoursesSold}</p>
        <p>Resources Sold: {revenue.totalResourcesSold}</p>
        <p>Current Balance: ₹{revenue.balance}</p>
        <p>Pending Payout: ₹{revenue.pendingPayout}</p>
      </div>
      
      <h3>Course-wise Revenue</h3>
      {revenue.courseRevenue.map(course => (
        <div key={course.courseId}>
          <p>{course.courseName}: ₹{course.revenue} ({course.unitsSold} units)</p>
        </div>
      ))}
      
      <h3>Monthly Breakdown</h3>
      {revenue.monthlyBreakdown.map(month => (
        <div key={month.month}>
          <p>{month.month}: ₹{month.amount} ({month.transactionCount} transactions)</p>
        </div>
      ))}
    </div>
  );
}

export default TrainerRevenueAnalytics;
```

---

## Related Endpoints

### Get Trainer Wallet Dashboard
**GET** `/api/v1/trainer-wallet/dashboard/{trainerId}`
- Returns basic wallet info: balance, totalEarned, totalSpent, pendingPayout

### Get Trainer Transaction History
**GET** `/api/v1/trainer-wallet/transactions/{trainerId}`
- Returns list of all transactions (payments in/out) for the trainer

### Request Payout
**POST** `/api/v1/trainer-wallet/request-payout`
- Trainer requests withdrawal of available balance
- Request body: `{ amount, bankAccount, ifscCode, accountHolderName }`

---

## Key Features

✅ **Total Course Sales Count** - Shows how many courses have been sold
✅ **Total Resource Sales Count** - Shows how many resources have been sold
✅ **Revenue Breakdown by Course** - Detailed revenue for each course
✅ **Revenue Breakdown by Resource** - Detailed revenue for each resource
✅ **Monthly Revenue Trends** - Track earnings month-over-month
✅ **Current Balance** - Available balance in wallet
✅ **Pending Payout** - Amount in pending withdrawal requests
✅ **Withdrawal History** - Total amount already withdrawn

---

## Implementation Details

### Data Sources
1. **WalletTransaction Table** - Tracks all earning transactions (BUY_COURSE, BUY_RESOURCE, EARNING)
2. **Payment Table** - Records all successful course/resource purchases with trainer association
3. **Wallet Table** - Stores trainer wallet balance, totalEarned, and pendingPayout

### Revenue Calculation Logic
- Platform takes 20% commission, trainer gets 80% of each sale
- Monthly breakdown aggregates all earnings transactions for each calendar month
- Course-wise revenue groups payments by courseId and aggregates amounts
- Resource-wise revenue groups payments by resourceId and aggregates amounts

### Performance Optimization
- Uses DynamoDB Global Secondary Indexes (GSI) for efficient querying
- Queries are optimized with userId-index for transaction lookups
- Results sorted by creation date (most recent first)

---

## Error Handling

| Status Code | Error | Description |
|-------------|-------|-------------|
| 200 | - | Successful response |
| 400 | PaymentException | Invalid input data |
| 401 | Unauthorized | Invalid or expired JWT token |
| 404 | WalletNotFoundException | Trainer wallet does not exist |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server-side error |

---

## Rate Limiting

This endpoint is protected by Resilience4j rate limiter with the configuration name `standardEndpoint`. Default limits:
- Requests per second: Configurable via application properties
- Timeout: Standard timeout applied

---

## Notes for Integration

1. **JWT Token Required**: Always include a valid Bearer token in the Authorization header
2. **Trainer-Only**: This endpoint extracts the trainer ID from the JWT token automatically
3. **Monthly Breakdown**: Sorted from most recent month descending
4. **Revenue vs Balance**: 
   - `totalRevenue`: Total earnings since account creation
   - `balance`: Amount available to withdraw
   - `pendingPayout`: Amount in pending withdrawal requests
   - Formula: `totalRevenue = balance + pendingPayout + totalWithdrawn`
5. **Last Updated**: The `lastUpdated` timestamp reflects when the wallet was last modified

---

## Version History

- **v1.0.0** (2026-07-28): Initial release with course, resource, and monthly revenue analytics
