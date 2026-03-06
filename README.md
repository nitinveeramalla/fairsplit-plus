# FairSplit+ 💸

> Open-source expense sharing — all Splitwise premium features free, plus AI-powered capabilities no other app has.

[![CI](https://github.com/YOUR_USERNAME/fairsplit-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/fairsplit-plus/actions)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://react.dev/)

## ✨ What makes it different

| Feature | Splitwise Free | Splitwise Premium | FairSplit+ |
|---|:---:|:---:|:---:|
| Group expense splitting | ✅ | ✅ | ✅ |
| Multi-currency | ❌ | ✅ | ✅ Free |
| Receipt scanning | ❌ | ✅ | ✅ Free |
| Spending analytics | ❌ | ✅ | ✅ Free |
| CSV/PDF export | ❌ | ✅ | ✅ Free |
| **NLP expense entry** | ❌ | ❌ | ✅ Unique |
| **AI spend coach (RAG)** | ❌ | ❌ | ✅ Unique |
| **Collaborative trip planner** | ❌ | ❌ | ✅ Unique |
| **Recurring auto-splits** | ❌ | ❌ | ✅ Unique |

## 🚀 Local Setup (5 minutes)

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### 1. Clone & configure
```bash
git clone https://github.com/YOUR_USERNAME/fairsplit-plus.git
cd fairsplit-plus
cp .env.example .env
# Edit .env and add your ANTHROPIC_API_KEY
```

### 2. Start infrastructure
```bash
docker compose up postgres redis -d
```

### 3. Run the API
```bash
mvn clean install -DskipTests
cd fairsplit-api
mvn spring-boot:run
```

API runs at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui.html`

### 4. Run tests
```bash
mvn test
```

## 🏗️ Architecture

```
fairsplit-plus/
├── fairsplit-core/          # Entities, repositories, business logic
│   ├── entity/              # JPA entities (User, Group, Expense, ...)
│   ├── repository/          # Spring Data JPA repositories
│   ├── service/             # Business logic (debt simplification, balance calc)
│   └── resources/db/        # Flyway migrations
├── fairsplit-integrations/  # External integrations
│   ├── ai/                  # Claude AI expense parsing
│   └── ocr/                 # Receipt scanning (Tesseract)
└── fairsplit-api/           # REST API layer
    ├── controller/          # REST endpoints
    ├── security/            # JWT + OAuth2 config
    └── config/              # App configuration
```

## 🔑 Key Technical Decisions

**Why Flyway over Hibernate DDL?**
Flyway gives explicit, versioned control over schema changes. Hibernate's `ddl-auto: create/update` is fine for demos but dangerous in production — schema changes become undocumented and irreversible.

**Why pgvector over Pinecone?**
Keeps the AI search layer inside the existing Postgres instance — no extra service, no extra cost, simpler ops. For a project of this scale, pgvector performs identically.

**Debt simplification algorithm**
Uses a greedy net-balance approach (O(n log n)) that reduces O(n²) individual debts to at most O(n-1) settlements. See `DebtSimplificationService.java`.

## 📄 License

MIT — use it, fork it, improve it.
