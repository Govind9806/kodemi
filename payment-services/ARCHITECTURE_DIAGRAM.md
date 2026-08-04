# Trainer Revenue Analytics - Architecture Diagram

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          FRONTEND / CLIENT APPLICATION                      │
│  (Web Dashboard / Mobile App)                                               │
│  - Trainer Dashboard                                                        │
│  - Revenue Charts & Analytics                                              │
└──────────────────────────────────────────────┬──────────────────────────────┘
                                               │
                                    HTTP GET Request
                                               │
                    Authorization: Bearer {JWT_TOKEN}
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      API GATEWAY / LOAD BALANCER                            │
│  - Rate Limiting (Resilience4j)                                            │
│  - Request Validation                                                       │
└──────────────────────────────────────────────┬──────────────────────────────┘
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│              REST CONTROLLER LAYER                                          │
│  ┌──────────────────────────────────────────────────────────────┐           │
│  │ TrainerWalletController                                      │           │
│  │ @GetMapping("/revenue-analytics")                            │           │
│  │ - Extracts trainerId from JWT token                          │           │
│  │ - Calls WalletService.getTrainerRevenueAnalytics()           │           │
│  │ - Returns TrainerRevenueResponse                             │           │
│  └──────────────────────────────────────────────────────────────┘           │
└──────────────────────────────────────────────┬──────────────────────────────┘
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│              SERVICE LAYER                                                  │
│  ┌──────────────────────────────────────────────────────────────┐           │
│  │ WalletService                                                │           │
│  │ + getTrainerRevenueAnalytics(trainerId: String)             │           │
│  │   ├─ Fetch trainer wallet                                   │           │
│  │   ├─ Get earning transactions                               │           │
│  │   ├─ Analyze course purchases                               │           │
│  │   ├─ Analyze resource purchases                             │           │
│  │   ├─ Aggregate by month                                     │           │
│  │   ├─ Build course-wise breakdown                            │           │
│  │   ├─ Build resource-wise breakdown                          │           │
│  │   └─ Return TrainerRevenueResponse                          │           │
│  └──────────────────────────────────────────────────────────────┘           │
└──────────────────────────┬──────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ REPOSITORY LAYER │ │ REPOSITORY LAYER │ │ REPOSITORY LAYER │
├──────────────────┤ ├──────────────────┤ ├──────────────────┤
│ WalletRepository │ │ WalletTransaction│ │ PaymentRepository│
│                  │ │ Repository       │ │                  │
│ findByUserId()   │ │                  │ │ findByUserId()   │
│  └─ Returns      │ │ findByUserId...  │ │  └─ Returns all  │
│    single        │ │ TransactionTypes │ │    payments      │
│    wallet        │ │  └─ Returns list │ │    by user       │
│                  │ │    of earnings   │ │                  │
└────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    DYNAMODB - NOSQL DATABASE                                │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Table: wallets                                                      │   │
│  │ ├─ PK: userId                                                       │   │
│  │ ├─ Attributes: balance, totalEarned, totalSpent, pendingPayout      │   │
│  │ └─ GSI: userType-index                                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Table: wallet_transactions                                          │   │
│  │ ├─ PK: transactionId                                                │   │
│  │ ├─ Attributes: userId, transactionType, amount, createdAt, ...      │   │
│  │ └─ GSI: userId-index (for filtering earnings)                       │   │
│  │         Status: SUCCESS/PENDING/FAILED                              │   │
│  │         Types: EARNING, BUY_COURSE, BUY_RESOURCE, ADD_FUNDS, ...    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Table: payments                                                     │   │
│  │ ├─ PK: orderId                                                      │   │
│  │ ├─ Attributes: userId, targetId, targetType, amount, status, ...    │   │
│  │ └─ GSI: userId-index (for filtering trainer purchases)              │   │
│  │         targetType: RECORDED_COURSE, LIVE_COURSE, RESOURCE, ...     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

```
REQUEST
   │
   ├─ GET /api/v1/trainer-wallet/revenue-analytics
   ├─ Authorization: Bearer {JWT_TOKEN}
   │
   ▼
CONTROLLER
   │
   ├─ Extract trainerId from token (jwtUtil.extractUserId)
   │
   ▼
SERVICE
   │
   ├─ Query 1: Get Wallet
   │  └─ walletRepository.findByUserId(trainerId)
   │     └─ DynamoDB: wallets table (PK: userId)
   │
   ├─ Query 2: Get Earning Transactions
   │  └─ transactionRepository.findByUserIdAndTransactionTypes()
   │     └─ DynamoDB: wallet_transactions table (GSI: userId-index)
   │        └─ Filters: transactionType IN [EARNING, BUY_COURSE, BUY_RESOURCE]
   │        └─ Filters: status = SUCCESS
   │
   ├─ Query 3: Get All Payments for Trainer
   │  └─ paymentRepository.findByUserId(trainerId)
   │     └─ DynamoDB: payments table (GSI: userId-index)
   │
   ├─ IN-MEMORY AGGREGATION
   │  ├─ Count courses sold
   │  ├─ Count resources sold
   │  ├─ Group by courseId → calculate course revenue
   │  ├─ Group by resourceId → calculate resource revenue
   │  ├─ Group by month (YYYY-MM) → calculate monthly revenue
   │  └─ Extract wallet metrics (balance, pending, etc.)
   │
   ▼
RESPONSE
   │
   ├─ TrainerRevenueResponse
   ├─ ├─ trainerId
   ├─ ├─ totalCoursesSold
   ├─ ├─ totalResourcesSold
   ├─ ├─ totalTransactions
   ├─ ├─ totalRevenue
   ├─ ├─ balance
   ├─ ├─ pendingPayout
   ├─ ├─ totalWithdrawn
   ├─ ├─ lastUpdated
   ├─ ├─ courseRevenue[] (array of course breakdowns)
   ├─ ├─ resourceRevenue[] (array of resource breakdowns)
   ├─ └─ monthlyBreakdown[] (array of monthly breakdowns)
   │
   ▼
HTTP 200 OK (JSON)
```

---

## Class Diagram

```
┌────────────────────────────────────────────┐
│     TrainerWalletController                │
├────────────────────────────────────────────┤
│ - walletService: WalletService             │
│ - jwtUtil: JwtUtil                         │
├────────────────────────────────────────────┤
│ + getTrainerRevenueAnalytics()             │
│   : TrainerRevenueResponse                 │
└────────────┬─────────────────────────────┘
             │ calls
             ▼
┌────────────────────────────────────────────┐
│     WalletService                          │
├────────────────────────────────────────────┤
│ - walletRepository                         │
│ - transactionRepository                    │
│ - paymentRepository                        │
├────────────────────────────────────────────┤
│ + getTrainerRevenueAnalytics()             │
│   (trainerId: String)                      │
│   : TrainerRevenueResponse                 │
└────────────┬─────────────────────────────┘
             │ uses
        ┌────┼────┬──────────────────┐
        │    │    │                  │
        ▼    ▼    ▼                  ▼
    ┌────────────────────────────────────────┐
    │  TrainerRevenueResponse (DTO)          │
    ├────────────────────────────────────────┤
    │ - trainerId: String                    │
    │ - totalCoursesSold: Long               │
    │ - totalResourcesSold: Long             │
    │ - totalTransactions: Long              │
    │ - totalRevenue: BigDecimal             │
    │ - balance: BigDecimal                  │
    │ - pendingPayout: BigDecimal            │
    │ - totalWithdrawn: BigDecimal           │
    │ - lastUpdated: LocalDateTime           │
    │ - courseRevenue: List<RevenueByCourse> │
    │ - resourceRevenue: List<RevenueByRes.> │
    │ - monthlyBreakdown: List<MonthlyRev.>  │
    └────────────────────────────────────────┘
         │                 │                  │
         ▼                 ▼                  ▼
    ┌─────────────┐  ┌──────────────┐  ┌─────────────┐
    │RevenueByCo. │  │RevenueByRes. │  │MonthlyRev.  │
    ├─────────────┤  ├──────────────┤  ├─────────────┤
    │- courseId   │  │- resourceId  │  │- month      │
    │- courseName │  │- resourceNm. │  │- amount     │
    │- unitsSold  │  │- unitsSold   │  │- txnCount   │
    │- revenue    │  │- revenue     │  └─────────────┘
    └─────────────┘  └──────────────┘
```

---

## Database Entity Relationships

```
┌─────────────────────────┐
│      Wallet             │
├─────────────────────────┤
│ userId (PK)             │──────┐
│ userType                │      │
│ balance                 │      │
│ totalEarned             │      │
│ totalSpent              │      │
│ pendingPayout           │      │
│ status                  │      │
│ createdAt               │      │
└─────────────────────────┘      │
                                 │ owns
                                 │ one-to-many
                                 ▼
        ┌────────────────────────────────────────┐
        │  WalletTransaction                     │
        ├────────────────────────────────────────┤
        │ transactionId (PK)                     │
        │ userId (GSI) ◄─────────────────────────┤
        │ transactionType                        │
        │ amount                                 │
        │ description                            │
        │ referenceId                            │
        │ status                                 │
        │ createdAt                              │
        │ balanceBefore                          │
        │ balanceAfter                           │
        └────────────────────────────────────────┘
                         │
                         │ references
                         ▼
        ┌────────────────────────────────────────┐
        │  Payment                               │
        ├────────────────────────────────────────┤
        │ orderId (PK)                           │
        │ userId (GSI) ◄─── trainer receives    │
        │ targetId (courseId/resourceId)         │
        │ targetType (COURSE/RESOURCE/etc)       │
        │ amount                                 │
        │ status                                 │
        │ createdAt                              │
        └────────────────────────────────────────┘
```

---

## Transaction Flow for Revenue Calculation

```
TRAINER MAKES A SALE
        │
        ▼
User (Learner) purchases course/resource from Trainer
        │
        ├─ Payment record created
        │  └─ userId: learner_id
        │     targetId: course_id / resource_id
        │     targetType: RECORDED_COURSE / LIVE_COURSE / RESOURCE / RECORDING
        │     amount: ₹100 (full price)
        │     status: SUCCESS
        │
        ├─ Learner wallet debited
        │  └─ BUY_COURSE / BUY_RESOURCE transaction created
        │     amount: ₹100
        │
        └─ Trainer wallet credited
           └─ EARNING transaction created
              amount: ₹80 (80% of ₹100)
              trainerId: trainer_id
              transactionType: EARNING
              status: SUCCESS
              description: "Earning from course/resource sale"

WHEN TRAINER QUERIES ANALYTICS
        │
        ├─ Get all EARNING transactions where userId = trainer_id
        │  └─ Sum amounts to get totalRevenue
        │     Group by month to get monthly breakdown
        │
        ├─ Get all RECORDED_COURSE/LIVE_COURSE payments by trainer
        │  └─ Count unique combinations to get totalCoursesSold
        │     Group by courseId for course-wise breakdown
        │
        ├─ Get all RESOURCE/RECORDING payments by trainer
        │  └─ Count unique combinations to get totalResourcesSold
        │     Group by resourceId for resource-wise breakdown
        │
        └─ Return aggregated response
```

---

## Performance Optimization

```
OPTIMIZATION STRATEGY
        │
        ├─ Index Usage
        │  ├─ wallets: PK(userId) → O(1) lookup
        │  ├─ wallet_transactions: GSI(userId) → O(n) scan with index
        │  └─ payments: GSI(userId) → O(m) scan with index
        │
        ├─ Query Filtering
        │  ├─ transactionType IN [EARNING, BUY_COURSE, BUY_RESOURCE]
        │  ├─ status = SUCCESS
        │  └─ Reduces memory footprint
        │
        ├─ In-Memory Aggregation
        │  ├─ Sort by month (O(k log k) where k = months)
        │  ├─ Group by ID (O(n) linear aggregation)
        │  └─ Build response objects (O(unique items))
        │
        └─ Response Size
           └─ Includes only relevant data (no full transaction history)
```

---

## Error Handling Flow

```
REQUEST RECEIVED
        │
        ├─ Validate JWT Token
        │  ├─ ✓ Valid → Extract trainerId → Continue
        │  └─ ✗ Invalid → Return 401 Unauthorized
        │
        ├─ Fetch Trainer Wallet
        │  ├─ ✓ Found → Continue
        │  └─ ✗ Not Found → Return 404 WalletNotFoundException
        │
        ├─ Validate trainerId
        │  ├─ ✓ Not Null/Empty → Continue
        │  └─ ✗ Null/Empty → Return 400 PaymentException
        │
        ├─ Query Repositories
        │  ├─ ✓ Success → Continue
        │  └─ ✗ Error → Return 500 InternalServerError
        │
        ├─ Build Response
        │  ├─ ✓ Success → Return 200 OK with TrainerRevenueResponse
        │  └─ ✗ Error → Return 500 InternalServerError
        │
        └─ Rate Limit Check
           ├─ ✓ Within Limit → Response sent
           └─ ✗ Exceeded → Return 429 Too Many Requests
```

---

## Integration Points with Existing System

```
EXISTING SYSTEMS                NEW ENDPOINT
        │                             │
        ├─ PaymentService ◄───────────┤─ Uses Payment table
        ├─ WalletService  ◄───────────┤─ Uses Wallet table
        ├─ RefundService  ◄───────────┤─ Reads from repositories
        ├─ NotificationService        │─ Potential future integration
        │                             │
        └─ JwtUtil ◄───────────────────┤─ Extracts trainerId
```

---

**Architecture Version**: 1.0
**Last Updated**: July 28, 2026
