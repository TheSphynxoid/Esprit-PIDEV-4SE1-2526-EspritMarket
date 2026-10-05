# EspritMarket Backend

> Phase 1 microservices: one Maven reactor (`esprit-market-backend`) that builds a shared library, five independently deployable Spring Boot services, a Eureka registry, and a Spring Cloud Gateway edge — all against **one shared PostgreSQL database** (deliberate Phase 1 constraint).

[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-green?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-blue?logo=spring)](https://spring.io/projects/spring-cloud)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)

## 📋 Table of Contents

- [Overview](#overview)
- [Modules](#modules)
- [Ports and Routes](#ports-and-routes)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Building](#building)
- [Running](#running)
- [Authentication Model](#authentication-model)
- [Database and Flyway](#database-and-flyway)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

The backend was split (Phase 1) from a monolith into five services behind a Eureka registry and a Spring Cloud Gateway. Key properties:

- **Maven multi-module reactor**: `backend/pom.xml` is the parent; every service is its own Spring Boot application with its own `application.yml` and port.
- **Shared database**: all five services point at the same PostgreSQL schema in Phase 1 (a conscious "distributed monolith" compromise; database-per-service, event-driven projections and FK removal are Phase 2).
- **Gateway-centric security**: the gateway validates JWTs and injects trusted `X-User-*` headers; services do not parse the `Authorization` header themselves.
- **`esprit-common` library**: plain JAR (no component scanning, no datasource) shared by all services.

## 📦 Modules

| Module | Type | Contents |
|--------|------|----------|
| `esprit-common` | library JAR | `JwtService`, exception types + `GlobalExceptionHandler`, `ErrorResponse`, `PageResponse`, `User`/`Role` entities, `NotificationEvent`/`StatusTransitionEvent`, `XUserAuthFilter` |
| `esprit-auth` | service :8081 | login/register/refresh/logout, `/api/auth/**`, `/api/common/users/**`, password reset, email service; **owns the shared-DB Flyway migrations in Phase 1** |
| `esprit-marketplace` | service :8082 | Marketplace (products, stores, orders, reviews, seller requests) **and** Delivery (deliveries, couriers, vehicules, quiz, messages, map tracking) — shipped together deliberately: bidirectional `Order`↔`Delivery` cascades and shared raw SQL |
| `esprit-srv` | service :8083 | services, bookings, projects, deliverables, escrow, wallets, notifications, pg_notify bridge |
| `esprit-eventplanning` | service :8084 | events, tickets, stalls, equipment, reservations, Stripe payments |
| `esprit-partnership` | service :8085 | job offers, applications, interviews, partner companies, profiles |
| `eureka-server` | registry :8761 | Eureka service registry (standalone) |
| `esprit-gateway` | edge :8088 | route table, JWT validation, `X-User-*` header injection |

Package names are unchanged (`net.thesphynx.espritmarket.<Module>`), so business code imports are identical to the monolith era.

## 🌐 Ports and Routes

| Container | Port | Routes (via gateway) |
|-----------|------|----------------------|
| esprit-gateway | 8088 | single entrypoint |
| esprit-auth | 8081 | `/api/auth/**`, `/api/common/**` |
| esprit-marketplace | 8082 | `/api/marketplace/**`, `/api/market/**`, `/api/products/**`, `/api/stores/**`, `/api/search/products/**`, `/api/delivery/**`, `/api/admin/delivery/**`, `/api/admin/livreurs/**`, `/api/messages/**`, `/api/quiz/**`, `/uploads/**`, `/ws-marketplace/**` |
| esprit-srv | 8083 | `/api/srv/**`, `/ws/**` (booking chat + srv notifications) |
| esprit-eventplanning | 8084 | `/api/eventplanning/**` |
| esprit-partnership | 8085 | `/api/partnership/**` |
| eureka-server | 8761 | registry console |

> The partnership service also exposes `/ws` for its notification push, but the gateway routes `/ws/**` to `esprit-srv` (booking chat is the heavier `/ws` consumer). Partnership notifications degrade gracefully to REST polling (`@Autowired(required = false) SimpMessagingTemplate`) — Phase 2 will introduce proper topic routing.

## 🏗️ Architecture

```
Browser → Angular (nginx :80 / ng serve :4200)
        → Spring Cloud Gateway :8088  (validates JWT, strips Authorization,
                                       injects X-User-Email/Id/Roles)
        → lb://<service> via Eureka :8761
        → service (XUserAuthFilter rebuilds SecurityContext from headers)
        → shared PostgreSQL :5432
```

- **No per-request DB user lookup**: services no longer run `JwtAuthFilter` (which loaded the user from the DB on every request). Authorization (`@PreAuthorize`, `hasRole`) reads the JWT role claim carried in `X-User-Roles`.
- **Service-to-gateway trust**: the gateway injects `X-Gateway-Token` when `GATEWAY_SHARED_TOKEN` is set; `XUserAuthFilter` in each service only accepts `X-User-*` headers when that secret matches (empty value = trust mode for bare local development).
- **WebSocket**: SockJS endpoints (`/ws`, `/ws-marketplace`) work through the gateway via HTTP transports; the SockJS `websocket` transport and native WS endpoints cannot be upgraded through the servlet gateway (known `gateway-server-webmvc` limitation, verified empirically) and fall back / degrade as described above.

## ✅ Prerequisites

- JDK 21 (`java --version`)
- Maven wrapper included (`./mvnw`); no local Maven install needed
- PostgreSQL 16 (local or via Docker)
- Docker (optional, for the container stack)

## 🔨 Building

From `backend/`:

```bash
# Full reactor: build + test all modules
./mvnw test                     # 259 tests across 9 modules

# Build all jars, skip tests
./mvnw package -DskipTests

# Build one service (+ its dependencies)
./mvnw package -DskipTests -pl esprit-auth -am
```

Artifacts land in `<module>/target/<module>-0.2.0.jar`.

## ▶️ Running

### Whole stack locally without Docker compose

Use `devops/local-stack.ps1` (starts eureka → auth → services → gateway, tracks PIDs, health-checks):

```powershell
powershell -ExecutionPolicy Bypass -File devops/local-stack.ps1 start    # or stop | status
```

It expects a PostgreSQL at `SPRING_DATASOURCE_URL` (default `localhost:55432`, e.g. the seeded container from `devops/db/init/`) and sets `JWT_SECRET` / `GATEWAY_SHARED_TOKEN` identically for all processes.

### One service in an IDE

Run the module's application class (`AuthApplication`, `MarketplaceApplication`, `SrvApplication`, `EventPlanningApplication`, `PartnershipApplication`, `EurekaServerApplication`, `GatewayApplication`) with the module's `application.yml` defaults; override the datasource with environment variables. Boot `esprit-auth` before the other services so Flyway can prepare the schema.

### Docker (per-service images)

`backend/Dockerfile.service` is a parameterized multi-stage build (`ARG MODULE`); `devops/docker-compose.yml` builds and wires db + eureka + 5 services + gateway (+ frontend, ml-service, prometheus, grafana):

```bash
cd devops
docker compose build
docker compose up -d
```

## 🔐 Authentication Model

**Token issuance** (esprit-auth only):
- `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh` return `AuthResponse` with access + refresh tokens.
- Access token claims: `sub` = email, `roles` = `["ROLE_<ROLE>"]`, `userId`/`user_id`/`id` = user id, `type` = `access` (refresh tokens carry `type=refresh`).

**Token consumption** (gateway):
- The gateway validates signature, expiry and `type=access`, then strips `Authorization` (and any client-supplied `X-User-*`) and injects `X-User-Email`, `X-User-Id`, `X-User-Roles` (+ `X-Gateway-Token`).
- Public paths (login/register/forgot/reset/refresh/logout, actuator, uploads, the public GET/POST endpoints of each module, WS paths) pass through untouched.
- Everything else requires a valid access token → 401 from the gateway.

**Known Phase 1 limitation**: logout blacklisting is per-instance inside esprit-auth (`TokenBlacklistService`, in-memory). The gateway does not consult it, so a logged-out access token remains valid until expiry. A shared blacklist store is Phase 2 work.

## 💾 Database and Flyway

- **One shared database** (`esprit_market`) for all services in Phase 1.
- **esprit-auth is the only migration runner** (`spring.flyway.enabled=true`, full `classpath:db/migration` set); all other services run with `spring.flyway.enabled=false` and rely on Hibernate `ddl-auto=validate`.
- The **pre-baseline legacy schema** (tables like `app_user`, `event`, `courier`, `orders`…) is not created by any migration file. On a fresh database it comes from `devops/db/init/01-espritmarket-base-schema.sql` (generated via Hibernate HBM2DDL from the pre-split monolith), mounted into the Postgres container's `docker-entrypoint-initdb.d`. Flyway then baselines at `20260325` and re-applies the guarded `V20260404+` migrations idempotently.
- Cross-module FKs (e.g. `event.user_id → app_user(id)`) exist on the shared schema; removing them is Phase 2.

## 🧪 Testing

```bash
# All modules (from backend/)
./mvnw test

# Single module
./mvnw test -pl esprit-marketplace

# Single test class / method
./mvnw test -pl esprit-marketplace -Dtest=ProductServiceTest
./mvnw test -pl esprit-marketplace -Dtest='ProductServiceTest#create_shouldMapPersistAndReturnResponse'
```

All tests are plain JUnit 5 + Mockito unit tests (no Spring context / no DB required). Current counts: esprit-common 3, esprit-auth 11, esprit-marketplace 129, esprit-srv 42, esprit-eventplanning 42, esprit-partnership 32 — total **259**.

## 🐛 Troubleshooting

**`relation "event" does not exist` at startup (fresh DB)**
→ The base schema is missing. Seed the DB from `devops/db/init/01-espritmarket-base-schema.sql`, or start `esprit-auth` against a DB that already has the legacy schema.

**`jwt.secret` empty / token errors**
→ `JWT_SECRET` must be set **identically** for the gateway and every service (the secret is Base64-decoded; an empty value fails on first use).

**Gateway 401 on everything**
→ A token was expected (path is protected). For bare-service debugging without the gateway, note that services trust `X-User-*` headers only when `GATEWAY_SHARED_TOKEN` matches or is unset.

**Port already in use**
→ Service ports are set in each module's `application.yml` (`server.port`); override with `SERVER_PORT`. (Local note: on some dev machines `httpd` occupies 8081 — the local stack script runs auth on 18081.)

**Eureka shows nothing**
→ Services retry registration; check `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` (default `http://localhost:8761/eureka/`).

---

**Version**: 0.2.0
**Status**: Phase 1 microservices (shared DB) — Phase 2 planned: database-per-service, event-driven projections, FK removal, shared token blacklist, WS routing.
