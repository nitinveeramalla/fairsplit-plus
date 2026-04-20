# FairSplit+

A production-grade expense splitting backend built with Java 21 and Spring Boot 3 — inspired by Splitwise, enhanced with AI-powered natural language expense parsing.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Module Structure](#module-structure)
- [Database Schema](#database-schema)
- [System Architecture](#system-architecture)
- [Low-Level Design](#low-level-design)
- [API Reference](#api-reference)
- [Request / Response Examples](#request--response-examples)
- [Debt Simplification Algorithm](#debt-simplification-algorithm)
- [AI Expense Parser](#ai-expense-parser)
- [Security Model](#security-model)
- [Running Locally](#running-locally)
- [Environment Variables](#environment-variables)
- [Flyway Migrations](#flyway-migrations)

---

# 🚀 Live API: https://fair-split-plus-production.up.railway.app

## Project Overview

FairSplit+ allows users to create groups, log shared expenses, automatically split costs between members, and settle debts with minimal transactions using a greedy debt simplification algorithm. An AI layer powered by Claude allows users to log expenses in plain English.

**Key differentiators vs a simple CRUD app:**
- Greedy debt graph simplification — minimizes the number of settlements needed
- Natural language expense parsing via Claude API
- JWT-based stateless auth with Spring Security 6
- Multi-module Maven architecture separating domain, integrations, and API layers

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.0 |
| Database | PostgreSQL 16 |
| Cache | Redis (configured, optional) |
| Migrations | Flyway |
| ORM | Hibernate / Spring Data JPA |
| Auth | JWT (jjwt 0.12.5) + BCrypt |
| AI | Spring AI + Anthropic Claude |
| Build | Maven (multi-module) |

---

## Module Structure

```
fairsplit-plus/
├── fairsplit-core/               # Domain layer
│   ├── entity/                   # JPA entities
│   ├── repository/               # Spring Data repositories
│   ├── service/                  # DebtSimplificationService
│   └── resources/db/migration/   # Flyway SQL migrations
│
├── fairsplit-integrations/       # External integrations
│   └── ai/ExpenseParserService   # Claude AI integration
│
└── fairsplit-api/                # REST layer
    ├── controller/               # HTTP endpoints
    ├── service/                  # Business logic
    ├── security/                 # JWT filter, UserDetails
    ├── config/                   # SecurityConfig
    ├── dto/                      # Request/Response records
    └── utils/                    # UserUtils
```

### Dependency Graph

```
fairsplit-api
    └── depends on → fairsplit-core
    └── depends on → fairsplit-integrations
                          └── depends on → fairsplit-core
```

---

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────┐         ┌──────────────────┐
│     users       │         │     groups       │
│─────────────────│         │──────────────────│
│ id (UUID) PK    │◄────────│ created_by FK    │
│ email UNIQUE    │         │ id (UUID) PK     │
│ display_name    │         │ name             │
│ avatar_url      │         │ type (ENUM)      │
│ password_hash   │         │ avatar_url       │
│ provider (ENUM) │         │ is_archived      │
│ provider_id     │         │ created_at       │
│ created_at      │         │ updated_at       │
│ updated_at      │         └──────────────────┘
└─────────────────┘                  │
         │                           │
         │              ┌────────────▼─────────────┐
         │              │      group_members        │
         │              │──────────────────────────│
         └─────────────►│ group_id FK (PK)          │
                        │ user_id FK (PK)           │
                        │ role (ADMIN | MEMBER)     │
                        │ joined_at                 │
                        └──────────────────────────┘

┌──────────────────────────────────────┐
│              expenses                │
│──────────────────────────────────────│
│ id (UUID) PK                         │
│ group_id FK → groups                 │
│ paid_by FK → users                   │
│ created_by FK → users                │
│ description                          │
│ amount NUMERIC(12,2)                 │
│ currency (default USD)               │
│ amount_usd NUMERIC(12,2)             │
│ category (ENUM)                      │
│ split_type (ENUM)                    │
│ receipt_url                          │
│ notes                                │
│ expense_date                         │
│ is_deleted (soft delete)             │
│ created_at                           │
│ updated_at                           │
└──────────────────────────────────────┘
         │
         │ 1:N
         ▼
┌──────────────────────────┐
│      expense_splits      │
│──────────────────────────│
│ id (UUID) PK             │
│ expense_id FK → expenses │
│ user_id FK → users       │
│ owed_amount NUMERIC(12,2)│
└──────────────────────────┘

┌──────────────────────────────────┐
│          settlements             │
│──────────────────────────────────│
│ id (UUID) PK                     │
│ group_id FK → groups             │
│ paid_by FK → users               │
│ paid_to FK → users               │
│ amount NUMERIC(12,2)             │
│ note TEXT                        │
│ settled_at TIMESTAMPTZ           │
│ created_at TIMESTAMPTZ           │
└──────────────────────────────────┘
```

### ENUM Types

| Entity | Field | Values |
|---|---|---|
| User | provider | `LOCAL`, `GOOGLE` |
| Group | type | `TRIP`, `HOME`, `COUPLE`, `OTHER` |
| GroupMember | role | `ADMIN`, `MEMBER` |
| Expense | split_type | `EQUAL`, `PERCENTAGE`, `EXACT`, `SHARES` |
| Expense | category | `FOOD`, `TRANSPORT`, `ACCOMMODATION`, `UTILITIES`, `ENTERTAINMENT`, `GROCERIES`, `HEALTH`, `SHOPPING`, `OTHER` |

---

## System Architecture

### Request Flow (High Level)

```
Client
  │
  ▼
HTTP Request
  │
  ▼
JwtAuthFilter (OncePerRequestFilter)
  │  ├── Extract Bearer token from Authorization header
  │  ├── Validate JWT signature + expiry
  │  └── Set SecurityContext with authenticated user
  │
  ▼
Spring Security Filter Chain
  │
  ▼
Controller (@RestController)
  │  ├── Validate request body
  │  ├── Extract current user via UserUtils
  │  └── Delegate to Service
  │
  ▼
Service (@Service)
  │  ├── Business logic
  │  ├── Repository calls
  │  └── Return domain object
  │
  ▼
Repository (Spring Data JPA)
  │
  ▼
PostgreSQL
```

### Authentication Flow

```
POST /api/auth/register
  │
  ├── Validate email uniqueness
  ├── BCrypt hash password
  ├── Save User entity
  └── Return JWT token
       │
       └── JWT contains: sub (email), userId, iat, exp

POST /api/auth/login
  │
  ├── Load user by email
  ├── BCrypt.matches(rawPassword, hash)
  └── Return JWT token

Subsequent requests:
  Authorization: Bearer <token>
  │
  └── JwtAuthFilter validates → sets Authentication in SecurityContext
```

---

## Low-Level Design

### Class Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                        Controllers                            │
│                                                              │
│  AuthController          GroupController                     │
│  ├── register()          ├── createGroup()                   │
│  └── login()             ├── addMember()                     │
│                          └── getGroups()                     │
│  ExpenseController       SettlementController                │
│  ├── createExpense()     ├── getAllDebts()                   │
│  ├── getAllExpenses()     ├── getAllSettlements()             │
│  └── parseExpense()      └── recordSettlement()              │
└──────────────────────────────────────────────────────────────┘
         │ delegates to
┌──────────────────────────────────────────────────────────────┐
│                         Services                              │
│                                                              │
│  AuthService             GroupService                        │
│  ├── register()          ├── createGroup()                   │
│  └── login()             ├── addMember()                     │
│                          └── getGroupsForUser()              │
│  ExpenseService          SettlementService                   │
│  ├── createExpense()     ├── calculateBalances()             │
│  ├── getExpensesForGroup()├── getSimplifiedDebts()           │
│  └── parseExpense()      ├── recordSettlement()              │
│                          └── getSettlementHistory()          │
│                                                              │
│  DebtSimplificationService   ExpenseParserService            │
│  └── simplify()              └── parse()                     │
└──────────────────────────────────────────────────────────────┘
         │ uses
┌──────────────────────────────────────────────────────────────┐
│                       Repositories                            │
│                                                              │
│  UserRepository          GroupRepository                     │
│  ├── findByEmail()       └── findByMembers_User_Id()         │
│  └── existsByEmail()                                         │
│                                                              │
│  ExpenseRepository       ExpenseSplitRepository              │
│  └── findByGroupId()     └── findByGroupId()                 │
│                                                              │
│  SettlementRepository                                        │
│  ├── findByGroupId()                                         │
│  └── findByGroupAndUser()                                    │
└──────────────────────────────────────────────────────────────┘
```

### Security Filter Chain

```
DisableEncodeUrlFilter
WebAsyncManagerIntegrationFilter
SecurityContextPersistenceFilter
HeaderWriterFilter
CorsFilter
LogoutFilter
JwtAuthFilter  ← custom
RequestCacheAwareFilter
SecurityContextHolderAwareRequestFilter
AnonymousAuthenticationFilter
SessionManagementFilter
ExceptionTranslationFilter
AuthorizationFilter
```

### Jackson Serialization Strategy

| Relationship | Parent Annotation | Child Annotation |
|---|---|---|
| Group → GroupMember | `@JsonManagedReference` | `@JsonBackReference` |
| Expense → ExpenseSplit | `@JsonManagedReference` | `@JsonBackReference` |
| All entities with LAZY fields | `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` | — |
| User.passwordHash | `@JsonIgnore` | — |

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Register a new user |
| POST | `/api/auth/login` | None | Login, returns JWT |

### Groups

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/groups` | JWT | Create a group |
| POST | `/api/groups/{groupId}/members` | JWT | Add a member to group |
| GET | `/api/groups` | JWT | Get all groups for current user |

### Expenses

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/expenses` | JWT | Log an expense with equal split |
| GET | `/api/expenses/group/{groupId}` | JWT | Get all expenses for a group |
| POST | `/api/expenses/parse` | JWT | Parse natural language into expense |

### Settlements

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/settlements/group/{groupId}/balances` | JWT | Get simplified debts (who owes whom) |
| POST | `/api/settlements` | JWT | Record a payment |
| GET | `/api/settlements/group/{groupId}` | JWT | Get settlement history |

---

## Request / Response Examples

### POST /api/auth/register

**Request:**
```json
{
  "email": "alice@example.com",
  "password": "securepassword",
  "displayName": "Alice"
}
```

**Response (201):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### POST /api/auth/login

**Request:**
```json
{
  "email": "alice@example.com",
  "password": "securepassword"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### POST /api/groups

**Request:**
```json
{
  "name": "Trip to Vegas"
}
```

**Response (201):**
```json
{
  "id": "c57bc175-8fc1-4985-904b-d8df88d2d991",
  "name": "Trip to Vegas",
  "type": "OTHER",
  "members": [
    {
      "user": { "id": "...", "email": "alice@example.com", "displayName": "Alice" },
      "role": "ADMIN",
      "joinedAt": "2026-04-05T04:29:41Z"
    }
  ],
  "archived": false,
  "createdAt": "2026-04-05T04:29:41Z"
}
```

---

### POST /api/groups/{groupId}/members

**Request:**
```json
{
  "userId": "8c8c2405-5cd7-40c7-9246-19bfe96585f7"
}
```

**Response (200):** Updated group object

---

### POST /api/expenses

**Request:**
```json
{
  "groupId": "c57bc175-8fc1-4985-904b-d8df88d2d991",
  "amount": 90.00,
  "description": "Dinner",
  "currency": "USD"
}
```

**Response (201):**
```json
{
  "id": "38ebfbb4-3113-4d3a-ba4f-0fbe6f07a5a9",
  "group": { ... },
  "paidBy": { "displayName": "Alice", ... },
  "description": "Dinner",
  "amount": 90.00,
  "currency": "USD",
  "splitType": "EQUAL",
  "splits": [
    { "id": "...", "user": { ... }, "owedAmount": 30.00 },
    { "id": "...", "user": { ... }, "owedAmount": 30.00 },
    { "id": "...", "user": { ... }, "owedAmount": 30.00 }
  ]
}
```

---

### POST /api/expenses/parse

**Request:**
```json
{
  "input": "Split $90 dinner with Bob and Carol equally",
  "groupId": "c57bc175-8fc1-4985-904b-d8df88d2d991"
}
```

**Response — HIGH confidence (auto-created):**
```json
{
  "parsed": {
    "amount": 90.00,
    "currency": "USD",
    "description": "Dinner",
    "category": "FOOD",
    "splitType": "EQUAL",
    "participants": ["Alice", "Bob", "Carol"],
    "splits": [],
    "confidence": "HIGH",
    "clarificationNeeded": null
  },
  "groupId": "c57bc175-...",
  "autoCreated": true,
  "expenseId": "f1f9ea6a-..."
}
```

**Response — LOW confidence (needs confirmation):**
```json
{
  "parsed": {
    "amount": null,
    "confidence": "LOW",
    "clarificationNeeded": "How much was the expense?",
    "errorMessage": null
  },
  "groupId": "c57bc175-...",
  "autoCreated": false,
  "expenseId": null
}
```

---

### GET /api/settlements/group/{groupId}/balances

**Response (200):**
```json
[
  {
    "from": "8c8c2405-5cd7-40c7-9246-19bfe96585f7",
    "to": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "amount": 45.00
  }
]
```

---

### POST /api/settlements

**Request:**
```json
{
  "groupId": "c57bc175-8fc1-4985-904b-d8df88d2d991",
  "paidToId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 45.00,
  "note": "Dinner payback"
}
```

**Response (201):**
```json
{
  "id": "46faf988-7cdb-43f1-911b-b98481ba7a71",
  "group": { ... },
  "paidBy": { "displayName": "Bob", ... },
  "paidTo": { "displayName": "Alice", ... },
  "amount": 45.00,
  "note": "Dinner payback",
  "settledAt": "2026-04-05T21:04:49Z",
  "createdAt": "2026-04-05T21:04:49Z"
}
```

---

## Debt Simplification Algorithm

The core algorithm lives in `DebtSimplificationService` and minimizes the number of transactions required to settle a group.

### Problem

Given N people with M expenses, a naive approach creates O(M) settlements. The simplification reduces this to at most N-1 transactions.

### Algorithm

```
Input:  Map<UUID, BigDecimal> — net balance per user
        Positive = creditor (is owed money)
        Negative = debtor  (owes money)

1. Separate users into two max-heaps:
   - creditors (sorted by amount owed to them, descending)
   - debtors   (sorted by amount they owe, descending)

2. While both heaps are non-empty:
   a. Poll largest creditor and largest debtor
   b. Settlement amount = min(creditor balance, debtor balance)
   c. Record: debtor → creditor, amount
   d. Reduce both balances by settlement amount
   e. Re-insert into heap if remainder > 0.01

Output: List<Settlement(from, to, amount)>
```

### Example

```
Expenses in "Trip to Vegas" group (3 members: Alice, Bob, Carol):

Expense 1: Alice paid $90 dinner, split equally
  → Alice: +90, Bob: -30, Carol: -30, Alice: -30

Expense 2: Bob paid $60 Uber, split equally
  → Bob: +60, Alice: -20, Bob: -20, Carol: -20

Net balances:
  Alice: +90 - 30 - 20 = +40  (creditor)
  Bob:   +60 - 30 - 20 = +10  (creditor)
  Carol:      -30 - 20 = -50  (debtor)

After simplification:
  Carol → Alice: $40
  Carol → Bob:   $10

2 transactions instead of potentially many more.
```

---

## AI Expense Parser

The parser uses Claude via Spring AI to convert natural language into structured expense data.

### Flow

```
User Input: "Split $90 dinner with Bob and Carol equally"
     │
     ▼
ExpenseParserService.parse(input, payerName)
     │
     ├── Build prompt with system instructions + user input
     ├── Call AnthropicChatModel
     └── Parse JSON response into ExpenseParseResult
          │
          ├── confidence = HIGH, amount not null
          │     └── Auto-create expense → return autoCreated: true
          │
          └── confidence = LOW or clarificationNeeded not null
                └── Return parsed result for user confirmation
```

### System Prompt Strategy

The system prompt instructs Claude to:
- Always respond with valid JSON only (no markdown)
- Extract: amount, currency, description, category, splitType, participants, splits
- Return confidence level: HIGH / MEDIUM / LOW
- Set `clarificationNeeded` if input is ambiguous

### Supported Input Examples

| Input | Parsed Result |
|---|---|
| "Split $90 dinner with Bob and Carol" | amount: 90, splitType: EQUAL, participants: [Alice, Bob, Carol] |
| "I paid $45 for the Uber" | amount: 45, splitType: EQUAL, category: TRANSPORT |
| "Netflix $15 split 3 ways" | amount: 15, splitType: EQUAL, category: ENTERTAINMENT |
| "Groceries, not sure how much" | confidence: LOW, clarificationNeeded: "How much?" |

---

## Security Model

### JWT Structure

```
Header:  { "alg": "HS256" }
Payload: {
  "sub":    "alice@example.com",
  "userId": "8c8c2405-...",
  "iat":    1775362344,
  "exp":    1775363244   ← 15 minutes
}
Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
```

### Protected vs Public Routes

```
Public (no token required):
  POST /api/auth/register
  POST /api/auth/login

Protected (JWT required):
  All /api/groups/**
  All /api/expenses/**
  All /api/settlements/**
```

### Password Storage

Passwords are never stored in plaintext. BCrypt with strength 10 is used:
```
raw password → BCryptPasswordEncoder.encode() → $2a$10$... (stored in DB)
login attempt → BCryptPasswordEncoder.matches(raw, hash)
```

---

## Running Locally

### Prerequisites

- Java 21
- Docker (for PostgreSQL)
- Maven 3.9+
- Anthropic API key (for AI parser)

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/your-username/fairsplit-plus.git
cd fairsplit-plus

# 2. Start PostgreSQL
docker compose up postgres -d

# 3. Set environment variables
export ANTHROPIC_API_KEY=sk-ant-your-key-here

# 4. Build
mvn clean install -DskipTests

# 5. Run
cd fairsplit-api && mvn spring-boot:run
```

The app starts on `http://localhost:8080`

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DATABASE_URL` | No | `jdbc:postgresql://localhost:5432/fairsplit` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | No | `fairsplit` | DB username |
| `DATABASE_PASSWORD` | No | `fairsplit` | DB password |
| `JWT_SECRET` | Yes (prod) | `change-this-in-production-min-256-bits` | HMAC signing key |
| `ANTHROPIC_API_KEY` | Yes (AI) | — | Claude API key |

---

## Flyway Migrations

| Version | File | Description |
|---|---|---|
| V1 | `V1__initial_schema.sql` | Users, groups, group_members, expenses, expense_splits tables |
| V2 | `V2__fix_currency_column.sql` | CHAR → VARCHAR fix on currency column |
| V3 | `V3__add_settlements_table.sql` | Settlements table with indexes |

Migrations live in `fairsplit-core/src/main/resources/db/migration/` and are applied automatically on startup.

---

## Known Tech Debt

| Item | Status | Notes |
|---|---|---|
| Duplicate `spring-boot-maven-plugin` in `fairsplit-api/pom.xml` | Open | Warning only, doesn't affect build |
| `spring.jpa.open-in-view` enabled | Open | Add `spring.jpa.open-in-view=false` to application.yml |
| `validate-on-migrate: false` in Flyway | Open | V1 migration file needs to be recovered |
| JWT expiry is 15 minutes | Open | Add refresh token endpoint |
| No `@Transactional` on service methods | Open | Add for data consistency |
| No input validation (`@Valid`) | Open | Add Bean Validation to DTOs |

---

## Built In Public

This project was built week-by-week and documented publicly on LinkedIn:

- Week 1: Schema design + debt simplification algorithm
- Week 2: JWT authentication
- Week 3: Spring Security 6 filter chain
- Week 4: Group and Expense APIs
- Week 5: Settlement tracking
- Week 6: AI expense parser

**Stack:** Java 21 · Spring Boot 3 · PostgreSQL · Redis · Flyway · JWT · Spring AI · Claude