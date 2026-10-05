# Esprit Market DevOps

Infrastructure, scripts, and shared resources for the Esprit Market monorepo workspace.

> **Phase 1 microservices**: `docker-compose.yml` now brings up db + eureka + 5 services + gateway (+ frontend, ml-service, prometheus, grafana). `local-stack.ps1` runs the whole backend stack locally without compose. `docker-compose.dev.yml` and the `k8s/` manifests are **monolith-era** and pending a Phase 2 refresh.

## Structure

```
devops/
├── docker-compose.yml        # Phase 1 microservices stack (db, eureka, 5 services, gateway, frontend, ml, monitoring)
├── docker-compose.dev.yml    # MONOLITH-ERA dev overrides (mvn spring-boot:run on the old single module) - stale
├── local-stack.ps1           # Run the 7 backend services + gateway locally as plain Java processes
├── db/
│   └── init/
│       └── 01-espritmarket-base-schema.sql   # base schema for FRESH databases (mounted into postgres docker-entrypoint-initdb.d)
├── config/
│   └── prometheus.yml        # scrape targets: gateway + 5 services + eureka
├── scripts/
│   ├── setup.sh              # Initialize workspace
│   ├── start-dev.sh          # Start dev environment (monolith-era)
│   ├── generate-types.sh     # Generate TypeScript from OpenAPI
│   ├── create-secrets.sh     # K8s secrets helper
│   ├── setup-vps.sh          # VPS bootstrap (kubeadm guide)
│   └── setup-storage.sh      # Storage bootstrap
├── shared/api-types/         # Auto-generated API types
└── k8s/                      # MONOLITH-ERA manifests - pending microservices rewrite
    ├── backend/              # monolith deployment + postgres
    ├── frontend/
    ├── dev/                  # dev overlays (monolith-era)
    ├── monitoring/
    └── overlays/single-node/
```

## Services (docker compose)

| Service | Container port | Host port | Description |
|---------|----------------|-----------|-------------|
| backend (**the gateway**) | 8088 | 8088 | Spring Cloud Gateway — single entrypoint (JWT validation + X-User-* headers). Named `backend` so the frontend nginx upstream `backend:8088` keeps resolving |
| eureka | 8761 | 8761 | Service registry console |
| auth | 8081 | — | login/register/refresh/logout, /api/common/users; runs Flyway |
| marketplace | 8082 | — | Marketplace + Delivery (shipped together) |
| srv | 8083 | — | services/bookings/projects; `/ws` booking chat |
| eventplanning | 8084 | — | events/tickets/Stripe |
| partnership | 8085 | — | partnership domain |
| db | 5432 | — | PostgreSQL 16, seeded by `db/init/*.sql` on a fresh volume |
| frontend | 8080 | 80 | Angular + nginx reverse proxy |
| ml-service | 8000 | 8000 | FastAPI predictions |
| prometheus / grafana | 9090 / 3000 | 9090 / 3000 | metrics |

Frontend dev server (`ng serve`) runs on 4200 and proxies `/api`, `/uploads`, `/ws`, `/ws-marketplace` to the gateway (see `frontend/proxy.conf.json`).

## Local stack (no compose)

Prerequisite: jars built once (`cd backend && ./mvnw package -DskipTests`) and a PostgreSQL reachable at the datasource URL (the script defaults to `localhost:55432` — e.g. a container seeded from `db/init/`).

```powershell
powershell -ExecutionPolicy Bypass -File local-stack.ps1 start    # eureka -> auth (Flyway) -> 4 services -> gateway
powershell -ExecutionPolicy Bypass -File local-stack.ps1 status
powershell -ExecutionPolicy Bypass -File local-stack.ps1 stop
```

Boot order matters: **auth must start before the other services** (it owns the shared-DB migrations; the others run Hibernate `validate`).

## Fresh database bootstrap

The legacy pre-baseline schema (app_user, event, courier, orders, ...) is **not** created by any Flyway migration. For a fresh database, seed it first:

```bash
docker run -d --name esprit-local-db \
  -e POSTGRES_DB=esprit_market -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -p 55432:5432 \
  -v "$(pwd)/db/init:/docker-entrypoint-initdb.d:ro" \
  postgres:16-alpine
```

Afterwards auth baselines at `20260325` and re-applies the guarded `V20260404+` migrations idempotently.

## Environment

Required/expected variables (see `.env.example` at the repo root):

| Variable | Description |
|----------|-------------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Database credentials |
| `JWT_SECRET` | JWT signing key — **must be identical for the gateway and all 5 services** |
| `GATEWAY_SHARED_TOKEN` | Shared secret injected as `X-Gateway-Token`; services require it to trust `X-User-*` headers |
| `GOOGLE_MAPS_API_KEY` | Marketplace delivery maps |
| `STRIPE_*` | Eventplanning payments |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | SMTP (auth, marketplace, srv, eventplanning) |
| `ML_DB_PASSWORD` | ml-service DB access |

## Docker Commands

```bash
# Build all images (each module builds via backend/Dockerfile.service, ARG MODULE)
docker compose build

# Start the stack
docker compose up -d

# View logs
docker compose logs -f
docker compose logs -f backend      # gateway logs

# Stop services
docker compose down

# Stop and remove volumes (drops the DB - re-seeded from db/init on next up)
docker compose down -v
```

## Kubernetes

> **Monolith-era.** The manifests deploy the pre-split backend image. They need a rewrite for the microservices topology (per-service deployments or gateway-as-ingress) — tracked for Phase 2.

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/
```

## Related Repositories

- **Backend**: `../backend/` - Maven reactor: esprit-common, 5 services, eureka, gateway
- **Frontend**: `../frontend/` - Angular Web App
