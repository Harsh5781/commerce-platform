# Commerce Platform — System Architecture

## 1. Architecture Overview

The Commerce Platform follows a **modular monolith** pattern (not microservices). All components run in a single Spring Boot application with clear package boundaries.

### Package Structure

| Package | Responsibility |
|---------|----------------|
| `auth` | JWT authentication, login, register, token refresh |
| `user` | User model, repository, roles |
| `order` | Order CRUD, status transitions, timeline |
| `channel` | Channel connectors (Website, Amazon, Blinkit), sync |
| `dashboard` | Aggregated stats, channel breakdown |
| `audit` | Audit logging for entity changes |
| `config` | Security, CORS, cache, MongoDB, Redis, Swagger |
| `common` | DTOs, exceptions, filters, utilities |

### Layered Architecture

```
Controller → Service → Repository → MongoDB
     ↓           ↓
   DTOs    Channel Connectors (external APIs)
```

---

## 2. Database Schema (MongoDB Collections)

### users

| Field | Type | Notes |
|-------|------|-------|
| id | String | ObjectId |
| email | String | Unique index |
| passwordHash | String | BCrypt |
| name | String | |
| role | Enum | ADMIN, MANAGER, VIEWER |
| active | Boolean | Default true |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

### orders

| Field | Type | Notes |
|-------|------|-------|
| id | String | ObjectId |
| orderNumber | String | Unique index |
| channel | String | WEBSITE, AMAZON, BLINKIT |
| channelOrderRef | String | External reference |
| customer | Embedded | name, email, phone |
| items | Embedded list | productName, sku, quantity, unitPrice, totalPrice |
| status | OrderStatus | PENDING, CONFIRMED, etc. |
| totalAmount | BigDecimal | |
| shippingAddress | Embedded | line1, city, state, pincode, country |
| channelMetadata | Map | Channel-specific data |
| timeline | Embedded list | status, changedBy, notes, timestamp |
| placedAt | LocalDateTime | Indexed DESC |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

### channels

| Field | Type | Notes |
|-------|------|-------|
| id | String | ObjectId |
| name | String | |
| code | ChannelCode | Unique index (WEBSITE, AMAZON, BLINKIT) |
| status | ChannelStatus | ACTIVE, INACTIVE |
| description | String | |
| logoUrl | String | |
| apiConfig | Map | API configuration |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

### audit_logs

| Field | Type | Notes |
|-------|------|-------|
| id | String | ObjectId |
| userId | String | |
| userName | String | |
| action | String | e.g. ORDER_STATUS_UPDATE |
| entityType | String | e.g. ORDER |
| entityId | String | |
| details | Map | Action-specific payload |
| ipAddress | String | Optional |
| createdAt | LocalDateTime | Indexed DESC |

### counters

| Field | Type | Notes |
|-------|------|-------|
| _id | String | Sequence name (e.g. order_sequence) |
| seq | Long | Atomic counter |

### Indexes

- **users**: unique on `email`
- **orders**: unique on `orderNumber`; indexes on `channel`, `status`, `placedAt`; compound `(channel, status, placedAt)`; text index on `customer.name`, `orderNumber`, `channelOrderRef`
- **channels**: unique on `code`
- **audit_logs**: compound on `(entityType, entityId)`; index on `createdAt` DESC

---

## 3. API Contract

### Response Wrapper

All API responses use `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Optional message",
  "data": { ... },
  "errors": null,
  "timestamp": "2026-02-27T12:00:00"
}
```

### Pagination

List endpoints use `PageResponse<T>`:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

### Auth

- **POST /api/auth/login** — Returns `accessToken` and `refreshToken`
- **POST /api/auth/refresh** — Body: `{ "refreshToken": "..." }` → new tokens
- Protected endpoints require `Authorization: Bearer <accessToken>`

### Main Endpoints

| Resource | List | Get | Create | Update |
|----------|------|-----|--------|--------|
| Orders | GET /api/orders | GET /api/orders/:id, /number/:orderNumber | POST /api/orders | PATCH /api/orders/:id/status |
| Channels | GET /api/channels | GET /api/channels/:code | — | POST /api/channels/:code/sync |
| Dashboard | — | GET /api/dashboard/stats | — | — |
| Audit | GET /api/audit | GET /api/audit/entity/:type/:id | — | — |

---

## 4. Data Flow

### Order Lifecycle

```
Channel Sync or Manual Create
         ↓
    PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
         ↓
    CANCELLED / RETURNED / REFUNDED (terminal)
```

- Status transitions are validated server-side
- Each transition is logged in `order.timeline` and `audit_logs`
- Invalid transitions (e.g. PENDING → DELIVERED) are rejected

### Channel Sync Flow

1. User calls `POST /api/channels/{code}/sync`
2. `ChannelService` invokes the appropriate connector (Website, Amazon, Blinkit)
3. Connector uses Circuit Breaker + Retry (Resilience4j)
4. Orders are upserted by `orderNumber` (deduplication)
5. Order cache is evicted (`@CacheEvict`)

---

## 5. Resilience & Reliability

### Circuit Breaker (Resilience4j)

- Applied to all channel connectors
- **Sliding window**: 10 calls
- **Failure threshold**: 50%
- **Open state duration**: 30 seconds
- **Half-open**: 5 permitted calls
- Health indicator registered for actuator

### Retry

- **Max attempts**: 3
- **Base wait**: 1 second
- **Exponential backoff**: 2x multiplier

### Rate Limiting (Bucket4j)

- Per-IP bucket
- **Dev**: 60 req/min
- **Prod**: 30 req/min
- Excludes: `/actuator`, `/swagger`, `/api-docs`

### Graceful Startup

- `StartupHealthCheck` runs before app serves traffic
- Verifies: MongoDB, Redis, JWT secret, CORS, rate limit config
- Redis failure: logs warning, order cache falls through to MongoDB

### Graceful Shutdown

- `server.shutdown=graceful`
- Max wait: 30 seconds
- Drains in-flight requests, closes DB connections

### Correlation IDs

- UUID per request via `CorrelationIdFilter`
- Stored in MDC as `correlationId`
- Response header: `X-Correlation-ID`
- Included in structured logs for tracing

---

## 6. Security

### Auth Flow

1. **Login** → JWT access token (1h) + refresh token (24h)
2. **Protected requests** → `Authorization: Bearer <accessToken>`
3. **Token refresh** → POST /api/auth/refresh with refresh token

### RBAC

| Role | Access |
|------|--------|
| ADMIN | Full access, user registration, audit logs |
| MANAGER | Order create/update, channel sync |
| VIEWER | Read-only (orders, channels, dashboard) |

### Security Headers

- **HSTS**: max-age 31536000, includeSubDomains
- **X-Frame-Options**: DENY
- **Content-Security-Policy**: default-src 'self'; script-src 'self'; etc.
- **Referrer-Policy**: strict-origin-when-cross-origin
- **Permissions-Policy**: camera=(), microphone=(), geolocation=(), payment=()
- **X-Content-Type-Options**: nosniff

### Password

- BCrypt with strength 12

### Secrets

- No hardcoded secrets — all via environment variables (`JWT_SECRET`, `REDIS_PASSWORD`, etc.)

---

## 7. Performance

- **Redis caching** for order lookups (`getOrderById`) — 10 min TTL
- **In-memory cache** for dashboard stats — 60s TTL, volatile + synchronized refresh
- **Channels**: Direct MongoDB (only 3 rows, no cache)
- **MongoDB indexes** optimized for common queries (channel, status, placedAt, text search)
- **Pagination**: Max 100 items per page
- **Async audit logging** — `@Async` to avoid blocking request thread

---

## 8. Caching Strategy

| Entity | Cache | TTL | Notes |
|--------|-------|-----|-------|
| Orders (getById) | Redis | 10 min | Evicted on create, update, sync |
| Dashboard stats | In-memory | 60 s | Volatile field, synchronized refresh |
| Channels | None | — | Direct MongoDB (3 rows) |

---

## 9. Monitoring & Observability

### Health

- **/actuator/health** — MongoDB, Redis, circuit breakers, channel connectors, app
- **/actuator/health/liveness** — Liveness probe
- **/actuator/health/readiness** — Readiness probe

### Metrics

- **/actuator/prometheus** — Micrometer format for Prometheus scraping
- **Custom metrics**: `orders_created_total`, `orders_status_updated_total`, `orders_create_duration`, `orders_query_duration`, `channels_sync_total`, `channels_available`, `auth_login_success_total`, `auth_login_failure_total`, `ratelimit_rejected_total`

### Logging

- **Dev**: Console, `[correlationId]` in pattern
- **Prod**: JSON format, correlation IDs in all logs

---

## 10. Connection Pooling

| Resource | Pool Size | Wait Timeout |
|----------|-----------|-------------|
| **MongoDB** | min 5, max 20 | 5s |
| **Redis (Lettuce)** | min 2, max 10 | 2s |
| **Tomcat** | min 10, max 100 threads, 200 connections | 5s |

---

## 11. CI/CD

Three GitHub Actions workflows (`.github/workflows/`), all manual trigger:

- **ci.yml** — Full stack: builds both Docker images (backend with tests), `npm audit`
- **backend.yml** — Backend only: Docker build + test → push to GHCR
- **frontend.yml** — Frontend only: Docker build → push to GHCR

Tests run inside Docker containers for environment consistency. Integration tests excluded from CI (platform-specific embedded MongoDB).

---

## 12. Testing

| Type | Count | What it covers |
|------|-------|----------------|
| Unit tests | 140 | Services, controllers, JWT, validation, custom metrics |
| Integration tests | 12 | Full HTTP flow with embedded MongoDB (local only) |
| Load tests | 3 scripts | k6: normal (10 VU), peak (30 VU), stress (100 VU) |
| Coverage | JaCoCo | HTML + XML reports, excludes DTOs/models/config |

---

## 13. Assumptions & Trade-offs

| Decision | Rationale |
|----------|------------|
| **Channel connectors mocked** | Real APIs require API keys; connectors return sample data |
| **Modular monolith over microservices** | 48-hour assignment constraint; simpler deployment |
| **MongoDB over PostgreSQL** | Flexible schema for varied channel metadata |
| **Redis for orders only** | Avoids serialization complexity for other entities; dashboard uses in-memory for simplicity |
| **MongoDB sequence counter** | Order sequence uses MongoDB `findAndModify` for atomicity across instances |
