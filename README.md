# Commerce Platform — Admin CRM Portal

## Overview

Multi-channel order management CRM for e-commerce, aggregating orders from **Website**, **Amazon**, and **Blinkit**. Built as a Senior SDE assignment.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Spring Boot 3.2.4, Java 21 |
| **Database** | MongoDB 7 |
| **Cache** | Redis 7 (order caching), In-memory (dashboard stats) |
| **Frontend** | React 18, TypeScript, Vite 5, Tailwind CSS 3 |
| **Auth** | JWT with Spring Security, RBAC (ADMIN / MANAGER / VIEWER) |
| **Resilience** | Resilience4j (Circuit Breaker, Retry), Bucket4j (Rate Limiting) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Containerization** | Docker + Docker Compose |
| **Metrics** | Micrometer + Prometheus |

---

## Features

- **Multi-platform order aggregation** — Orders from Website, Amazon, and Blinkit in one dashboard
- **Order dashboard** — Filtering, sorting, search, and pagination
- **Order status tracking** — Full lifecycle with audit trail and timeline
- **JWT authentication + RBAC** — Role-based access (ADMIN, MANAGER, VIEWER)
- **Circuit breaker + retry** — Resilience4j on external channel APIs
- **Rate limiting** — Bucket4j per-IP (configurable dev/prod limits)
- **Graceful startup/shutdown** — Pre-flight health checks, connection draining
- **Structured logging** — Correlation IDs for distributed tracing
- **Health checks** — Actuator health (MongoDB, Redis, circuit breakers)
- **Prometheus metrics** — `/actuator/prometheus` for monitoring

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local dev)
- Node.js 20+ (for local dev)

### 1. Clone and configure

```bash
git clone https://github.com/Harsh5781/commerce-platform.git
cd commerce-platform
cp .env.example .env
```

### 2. Run with Docker Compose

```bash
docker-compose up --build
```

### 3. Access

| Service | URL |
|---------|-----|
| **Frontend** | http://localhost:5173 |
| **Backend API** | http://localhost:8080 |
| **Swagger UI** | http://localhost:8080/swagger-ui |
| **Actuator Health** | http://localhost:8080/actuator/health |

---

## Local Development (without Docker)

### 1. Start MongoDB

```bash
mongosh < init-mongo.js
```

### 2. Start Redis

```bash
redis-server
```

### 3. Backend

```bash
cd backend
./gradlew bootRun
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## Demo Credentials

| Role | Email | Password |
|------|-------|----------|
| **ADMIN** | admin@commerce.com | admin123 |
| **MANAGER** | manager@commerce.com | manager123 |
| **VIEWER** | viewer@commerce.com | viewer123 |

---

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | No | Login with email/password |
| POST | `/api/auth/register` | Admin | Register new user |
| POST | `/api/auth/refresh` | No | Refresh access token |
| GET | `/api/auth/me` | Yes | Current user info |
| GET | `/api/orders` | Yes | List orders (filter, sort, search, paginate) |
| GET | `/api/orders/{id}` | Yes | Get order by ID |
| GET | `/api/orders/number/{orderNumber}` | Yes | Get order by order number |
| POST | `/api/orders` | Admin, Manager | Create order |
| PATCH | `/api/orders/{id}/status` | Admin, Manager | Update order status |
| GET | `/api/channels` | Yes | List all channels |
| GET | `/api/channels/{code}` | Yes | Get channel by code |
| POST | `/api/channels/{code}/sync` | Admin, Manager | Sync orders from channel |
| GET | `/api/dashboard/stats` | Yes | Dashboard statistics |
| GET | `/api/audit` | Admin | List audit logs |
| GET | `/api/audit/entity/{entityType}/{entityId}` | Admin | Audit logs for entity |

---

## Project Structure

```
commerce-platform/
├── backend/
│   ├── src/main/java/com/crm/commerce/platform/
│   │   ├── Application.java
│   │   ├── auth/           # JWT, login, register, refresh
│   │   ├── user/           # User model, repository
│   │   ├── order/          # Order CRUD, status, timeline
│   │   ├── channel/        # Channel connectors, sync
│   │   ├── dashboard/      # Stats, channel breakdown
│   │   ├── audit/          # Audit logs
│   │   ├── config/         # Security, CORS, cache, etc.
│   │   ├── common/         # DTOs, exceptions, filters
│   │   └── migration/      # DatabaseSeeder
│   └── src/main/resources/
│       ├── application.properties
│       ├── application-dev.properties
│       └── application-prod.properties
├── frontend/
│   ├── src/
│   │   ├── api/            # API client, auth, orders, channels, dashboard
│   │   ├── components/     # Header, Sidebar, Layout, StatusBadge
│   │   ├── context/        # AuthContext
│   │   ├── pages/          # Dashboard, Orders, OrderDetail, Channels, Login
│   │   └── types/
│   └── vite.config.ts
├── load-tests/             # k6 load test scripts
│   ├── config.js
│   ├── helpers.js
│   ├── normal-load.js      # Normal load scenario
│   ├── peak-load.js        # Peak load scenario
│   └── stress-test.js      # Stress test scenario
├── init-mongo.js           # MongoDB init script
├── docker-compose.yml
└── .env.example
```

---

## Testing

### Unit & Integration Tests

```bash
cd backend

# Run all tests (unit + integration)
./gradlew test

# Coverage report (auto-generated with test run)
# Open: build/reports/jacoco/test/html/index.html
```

| Type | Count | Scope |
|------|-------|-------|
| Unit tests | 140 | Services, controllers, JWT, validation, metrics |
| Integration tests | 12 | Full HTTP flow with embedded MongoDB |

### Load Testing

The `load-tests/` directory contains k6 scripts:

| Script | Description |
|--------|-------------|
| `normal-load.js` | Steady 10 VUs, 2 min — p95: 103ms |
| `peak-load.js` | Ramp to 30 VUs — p95: 96ms |
| `stress-test.js` | Ramp to 100 VUs — p95: 96ms |

```bash
# Install k6
brew install k6  # macOS

# Run (ensure Docker containers are running)
k6 run load-tests/normal-load.js
k6 run load-tests/peak-load.js
k6 run load-tests/stress-test.js
```

---

## CI/CD

Three GitHub Actions workflows (manual trigger via Actions tab):

| Workflow | File | What it does |
|----------|------|-------------|
| **CI Pipeline** | `.github/workflows/ci.yml` | Full stack build + tests + security scan |
| **Backend** | `.github/workflows/backend.yml` | Backend Docker build + tests → push to GHCR |
| **Frontend** | `.github/workflows/frontend.yml` | Frontend Docker build → push to GHCR |

To trigger: GitHub repo → **Actions** tab → select workflow → **Run workflow** → pick branch + environment.

---

## Environment Variables

See `.env.example` for all configurable variables. Key ones:

- **MongoDB**: `MONGO_PORT`, `MONGO_ROOT_USER`, `MONGO_ROOT_PASSWORD`, `MONGO_DB`
- **Redis**: `REDIS_PORT`, `REDIS_PASSWORD`
- **Backend**: `SERVER_PORT`, `JWT_SECRET`, `CORS_ORIGINS`, `RATE_LIMIT_RPM`
- **Feature flags**: `FF_CHANNEL_SYNC`, `FF_AUDIT_LOG`, `FF_NOTIFICATIONS`

---

## Documentation

| Document | Contents |
|----------|----------|
| [Technical Documentation](docs/TECHNICAL_DOCUMENTATION.md) | Full technical doc: architecture diagrams, API contracts, security threat model, load test results, scaling strategy, CI/CD pipeline |
| [Architecture](docs/ARCHITECTURE.md) | System architecture, database schema, data flow, resilience, caching, monitoring |
| [Swagger UI](http://localhost:8080/swagger-ui) | Interactive API documentation (when running) |
| [Prometheus Metrics](http://localhost:8080/actuator/prometheus) | Custom + JVM + HTTP metrics (when running) |
