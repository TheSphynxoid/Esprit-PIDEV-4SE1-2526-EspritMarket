# AGENTS.md - Esprit Market Workspace

This document provides coding guidelines for the monorepo workspace containing Backend, Frontend, and DevOps.

## Workspace Structure

```
esprit-market/
├── backend/           # Spring Boot API (Java 21)
├── frontend/          # Angular Web App (TypeScript)
└── devops/            # Infrastructure, Scripts, Shared Types
    ├── scripts/       # Setup and utility scripts
    ├── shared/        # Auto-generated API types
    └── k8s/           # Kubernetes manifests
```

---

## Quick Start

```bash
# From devops/ folder
./scripts/setup.sh           # Install dependencies
./scripts/start-dev.sh       # Start full development environment
./scripts/generate-types.sh  # Regenerate TypeScript types from API
```

---

## Backend (`backend/`)

### Tech Stack
- Java 21, Spring Boot 4.0.2, PostgreSQL, JWT Auth, gRPC, Prometheus

### Build Commands
```bash
./mvnw clean package -DskipTests    # Build
./mvnw clean test                    # Run tests
./mvnw spring-boot:run              # Run locally (port 8088)
```

### Project Structure
```
src/main/java/net/thesphynx/espritmarket/
├── Common/           # Config, Security, DTO, Entity, Exception
├── Marketplace/      # Products, Orders, Reviews, Stores
├── Delivery/         # Delivery tracking, Vehicles
├── EventPlanning/    # Events, Tickets, Stalls
├── Partnership/      # Partners, Job offers
└── Srv/              # Services, Projects
```

### Code Conventions
- Lombok: `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`
- Entities: `@JsonIgnoreProperties` on relationships, `FetchType.LAZY`
- DTOs: `Request` suffix for input, `Response` suffix for output
- Repositories: Prefix with `I` (e.g., `IProductRepository`)
- Services: Constructor injection, return `Optional<Dto>` for single lookups
- Controllers: OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponses`)

### API Documentation
- OpenAPI spec available at: `http://localhost:8088/v3/api-docs`
- Swagger UI: `http://localhost:8088/swagger-ui.html`

---

## Frontend (`frontend/`)

### Tech Stack
- Angular 17+, TypeScript, SCSS

### Build Commands
```bash
npm install           # Install dependencies
ng serve              # Dev server (port 4200)
ng build              # Production build
ng test               # Run unit tests
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

Types are generated from backend OpenAPI spec.

**Regenerate types:**
```bash
# From devops/ folder
./scripts/generate-types.sh

# Or manually:
# 1. Start backend
# 2. curl http://localhost:8088/v3/api-docs > shared/api-types/openapi.yaml
# 3. npx openapi-generator-cli generate -i openapi.yaml -g typescript-angular -o src/generated
```

**Usage in frontend:**
```typescript
// tsconfig.json paths already configured
import { ProductResponse } from '@esprit-market/api-types';

// In service
getProducts(): Observable<ProductResponse[]> {
  return this.http.get<ProductResponse[]>('/api/products');
}
```

---

## Docker Commands

### Development
```bash
# From esprit-market/ root
docker compose -f devops/docker-compose.yml -f devops/docker-compose.dev.yml up

# Or with rebuild
docker compose -f devops/docker-compose.yml -f devops/docker-compose.dev.yml up --build
```

### Production
```bash
docker compose -f devops/docker-compose.yml up -d
```

### Useful Commands
```bash
docker compose logs -f              # View all logs
docker compose logs -f backend      # Backend logs only
docker compose down                 # Stop services
docker compose down -v              # Stop and remove volumes
```

---

## Kubernetes Commands

### Deploy All
```bash
kubectl apply -f devops/k8s/namespace.yaml
kubectl apply -f devops/k8s/backend/
kubectl apply -f devops/k8s/frontend/
kubectl apply -f devops/k8s/monitoring/
```

### Verify
```bash
kubectl get all -n esprit-market
kubectl logs -f deployment/esprit-market-backend -n esprit-market
```

### Update
```bash
kubectl set image deployment/esprit-market-backend backend=thesphynx2000/espritmarket-backend:new-tag -n esprit-market
kubectl rollout status deployment/esprit-market-backend -n esprit-market
```

---

## CI/CD Pipeline

Jenkins pipeline stages:
1. Checkout
2. Dependency Check
3. Unit Tests
4. SonarQube Analysis
5. Quality Gate
6. Build Artifact
7. Security Scan (Trivy)
8. Build Docker Image
9. Scan Docker Image
10. Push Docker Image
11. Deploy to Kubernetes

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | (required) |
| `JWT_SECRET` | JWT signing key | (required) |
| `GOOGLE_MAPS_API_KEY` | Google Maps API | (optional) |

---

## Troubleshooting

### Backend won't start
- Check PostgreSQL is running: `docker compose ps db`
- Check logs: `docker compose logs backend`
- Verify environment variables

### Frontend can't reach API
- Check CORS configuration in backend
- Verify API URL in frontend environment
- Check network policies in Kubernetes

### Type generation fails
- Ensure backend is running on port 8088
- Check `/v3/api-docs` endpoint returns valid JSON
- Verify openapi-generator-cli is installed

---

## MCP Servers

Configured in `opencode.json` at project root.

| MCP | Type | Purpose |
|-----|------|---------|
| `context7` | Remote | Search Spring Boot, Angular, K8s docs |
| `postgres` | Local | Query PostgreSQL directly (via `POSTGRES_CONNECTION_STRING`) |

Use `use context7` in prompts to search documentation.

---

## Skills

Located in `.opencode/skills/<name>/SKILL.md`. Load with the `skill` tool.

| Skill | Purpose |
|-------|---------|
| `k8s-deploy` | Deploy/update app on Kubernetes |
| `debug-k8s` | Diagnose K8s pod and deployment failures |
| `generate-types` | Regenerate TypeScript types from OpenAPI spec |
| `docker-ops` | Manage Docker Compose stacks |

---

## File References

When referencing files, use format: `path/to/file.ts:42` for line numbers.

Example: "The product service is defined in `frontend/src/app/services/product.service.ts:15`"
