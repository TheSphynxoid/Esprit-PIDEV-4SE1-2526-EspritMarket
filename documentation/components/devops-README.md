# Esprit Market DevOps

Infrastructure, scripts, and shared resources for the Esprit Market monorepo workspace.

## Structure

```
devops/
├── AGENTS.md              # AI agent instructions
├── docker-compose.yml     # Production Docker Compose
├── docker-compose.dev.yml # Development overrides
├── scripts/
│   ├── setup.sh           # Initialize workspace
│   ├── start-dev.sh       # Start dev environment
│   └── generate-types.sh  # Generate TypeScript from OpenAPI
├── shared/
│   └── api-types/         # Auto-generated API types
└── k8s/
    ├── backend/           # Backend K8s manifests
    ├── frontend/          # Frontend K8s manifests
    └── monitoring/        # Prometheus/Grafana configs
```

## Quick Start

```bash
# 1. Setup workspace
./scripts/setup.sh

# 2. Edit .env with your secrets
nano ../.env

# 3. Start development environment
./scripts/start-dev.sh

# 4. Generate API types (after backend starts)
./scripts/generate-types.sh
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 4200 | Angular dev server |
| Backend | 8088 | Spring Boot API |
| Backend Debug | 5005 | Remote debugging |
| PostgreSQL | 5432 | Database |
| Prometheus | 9090 | Metrics |
| Grafana | 3000 | Dashboards |

## Type Generation

API types are auto-generated from the backend's OpenAPI spec:

```bash
./scripts/generate-types.sh
```

This fetches `/v3/api-docs` from the running backend and generates TypeScript interfaces.

## Docker Commands

```bash
# Start all services
docker compose up -d

# Start with dev overrides
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# View logs
docker compose logs -f

# Stop services
docker compose down

# Stop and remove volumes
docker compose down -v
```

## Kubernetes

```bash
# Apply all manifests
kubectl apply -f k8s/

# Or apply by component
kubectl apply -f k8s/backend/
kubectl apply -f k8s/frontend/
kubectl apply -f k8s/monitoring/
```

## Related Repositories

- **Backend**: `../backend/` - Spring Boot API
- **Frontend**: `../frontend/` - Angular Web App
