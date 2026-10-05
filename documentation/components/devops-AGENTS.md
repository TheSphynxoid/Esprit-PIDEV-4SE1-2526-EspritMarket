# AGENTS.md - Esprit Market Workspace

This document provides coding guidelines for the monorepo workspace containing Backend, Frontend, and DevOps.

## Workspace Structure

```
esprit-market/
├── backend/           # Maven reactor: esprit-common lib + 5 services + eureka + gateway (Java 21)
├── frontend/          # Angular Web App (TypeScript)
└── devops/            # docker compose, local-stack.ps1, db init SQL, scripts, shared types, k8s
    ├── scripts/       # Setup and utility scripts
    ├── shared/        # Auto-generated API types
    └── k8s/           # Kubernetes manifests (monolith-era, pending Phase 2 rewrite)
```

---

## Quick Start

```bash
# From devops/ folder
./scripts/setup.sh           # Install dependencies
./scripts/start-dev.sh       # Start dev environment (monolith-era script)
./scripts/generate-types.sh  # Regenerate TypeScript types from API

# Phase 1 microservices:
cd backend && ./mvnw package -DskipTests                                  # build all jars
powershell -ExecutionPolicy Bypass -File ../devops/local-stack.ps1 start  # local stack (no compose)
# or: cd devops && docker compose up --build
```

---

## Backend (`backend/`)

### Tech Stack
- Java 21, Spring Boot 4.0.4, Spring Cloud 2025.1.0 (Eureka + Gateway), PostgreSQL, JWT Auth, Prometheus. No gRPC (removed).

### Build Commands (from backend/, the reactor root)
```bash
./mvnw clean package -DskipTests            # build all modules
./mvnw clean test                            # run all tests (259)
./mvnw test -pl esprit-marketplace           # one module's tests
./mvnw spring-boot:run -pl esprit-auth       # run one service (:8081)
```

### Modules and Ports
| Module | Port | Routes |
|--------|------|--------|
| esprit-common | (library) | JwtService, exceptions, User/Role, events, XUserAuthFilter |
| esprit-auth | 8081 | /api/auth/**, /api/common/** - owns Flyway migrations |
| esprit-marketplace | 8082 | /api/marketplace/** + outliers (/api/market, /api/products, /api/stores, /api/search/products, /api/delivery/**, /api/admin/delivery/**, /api/admin/livreurs/**, /api/messages/**, /api/quiz/**, /uploads/**) |
| esprit-srv | 8083 | /api/srv/**, /ws/** |
| esprit-eventplanning | 8084 | /api/eventplanning/** |
| esprit-partnership | 8085 | /api/partnership/** |
| eureka-server | 8761 | registry |
| esprit-gateway | 8088 | single entrypoint: JWT validation + X-User-* header injection |

Phase 1 constraint: ONE shared PostgreSQL database for all services; esprit-auth is the only Flyway runner; fresh DBs are seeded from `devops/db/init/01-espritmarket-base-schema.sql`.

### Code Conventions
- Lombok: `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`
- Entities: `@JsonIgnoreProperties` on relationships, `FetchType.LAZY`
- DTOs: `Request` suffix for input, `Response` suffix for output
- Repositories: Prefix with `I` (e.g., `IProductRepository`)
- Services: Constructor injection, return `Optional<Dto>` for single lookups
- Controllers: OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponses`)
- Security: services trust gateway-injected `X-User-*` headers (via `XUserAuthFilter` in esprit-common); never parse `Authorization` in services; authorization reads the JWT role claim, never a local user table

### API Documentation
Each service has its own Swagger UI (not routed through the gateway):
`http://localhost:8081|8082|8083|8084|8085/swagger-ui.html`

---

## Frontend (`frontend/`)

### Tech Stack
- Angular 21, TypeScript, Tailwind CSS 4

### Build Commands
```bash
npm install --legacy-peer-deps   # Install dependencies
npm start                        # Dev server (port 4200, proxies to the gateway :8088)
ng build                         # Production build
ng test                          # Run unit tests
```

### Import API Types
```typescript
import { ProductResponse, ProductRequest } from '@esprit-market/api-types';
```

### Code Conventions
- Components: `*.component.ts`, `*.component.html`, `*.component.scss`
- Services: `*.service.ts` for HTTP calls
- Models: Import from `@esprit-market/api-types` (auto-generated)
- Naming: PascalCase for classes, camelCase for methods/properties

---

## Shared Types (`devops/shared/api-types/`)

### AUTO-GENERATED - Do Not Edit Manually

Types are generated from the backend OpenAPI spec.

**Regenerate types:**
```bash
# From devops/ folder
./scripts/generate-types.sh

# Or manually:
# 1. Start a service (e.g. esprit-marketplace on :8082)
# 2. curl http://localhost:8082/v3/api-docs > shared/api-types/openapi.json
# 3. npx openapi-generator-cli generate -i openapi.json -g typescript-angular -o src/generated
```

Note: since the split, each service exposes its own OpenAPI spec - types must be merged per service.

> **Known CI issue**: `frontend/Jenkinsfile` fetches `/v3/api-docs` from `http://10.100.202.174:8088` — that port is now the GATEWAY, which does not route `/v3/api-docs`. The generation step falls back to cached types today; a proper fix (gateway routes for swagger, or pointing the pipeline at a service port) is a Phase 2 item.

---

## Docker Commands

### Development
```bash
# From devops/ folder
docker compose up --build          # db, eureka, 5 services, gateway (named 'backend'), frontend, ml, monitoring
docker compose -f docker-compose.dev.yml up   # MONOLITH-ERA dev overrides (stale, pending Phase 2)
```

### Useful Commands
```bash
docker compose logs -f              # View all logs
docker compose logs -f backend      # Gateway logs
docker compose ps                   # Container status
docker compose down                 # Stop services
docker compose down -v              # Stop and remove volumes (DB re-seeded from db/init on next up)
```

---

## Kubernetes Commands

> **Monolith-era manifests** - they deploy the pre-split backend image. Rewrite for microservices planned in Phase 2.

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/
kubectl get all -n esprit-market
kubectl logs -f deployment/esprit-market-backend -n esprit-market
```

---

## CI/CD Pipeline

- **Frontend**: `frontend/Jenkinsfile` — checkout, OpenAPI type generation, `npm install` + `npm test`, docker build & push of the frontend image (repo `thesphynx2000/espritmarket-frontend`).
- **Backend**: no Jenkinsfile in the repo — CI relies on the Maven reactor (`./mvnw clean test` = 259 tests) and per-service images built with `backend/Dockerfile.service` (ARG MODULE).

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | (required) |
| `JWT_SECRET` | JWT signing key - identical for gateway + all services | (required) |
| `GATEWAY_SHARED_TOKEN` | Shared secret for gateway->service X-User-* trust | (optional; empty = trust mode) |
| `GOOGLE_MAPS_API_KEY` | Google Maps API | (optional) |

---

## Troubleshooting

### A service won't start
- Check PostgreSQL is running: `docker compose ps db` (or the local-stack DB)
- Check logs: `docker compose logs auth` (auth runs the migrations - start it first)
- Verify `JWT_SECRET` is set (empty secret fails on first token use)

### Frontend can't reach API
- The gateway listens on :8088 (dev proxy: `frontend/proxy.conf.json`)
- 401 from the gateway means the path is protected (valid JWT required)
- Check CORS/network policies in Kubernetes

### Type generation fails
- Ensure the target service is running on its port (8081-8085)
- Check `/v3/api-docs` endpoint returns valid JSON
- Verify openapi-generator-cli is installed

---

## MCP Servers

Configured in `opencode.json` at project root:

| MCP | Type | Purpose |
|-----|------|---------|
| `sweave` | Local | Agent orchestration/worktrees for this workspace |

---

## File References

When referencing files, use format: `path/to/file.ts:42` for line numbers.

Example: "The product service is defined in `backend/esprit-marketplace/src/main/java/net/thesphynx/espritmarket/Marketplace/Service/ProductService.java`"
