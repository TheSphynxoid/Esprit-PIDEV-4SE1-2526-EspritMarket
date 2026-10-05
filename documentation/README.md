# Documentation

This folder consolidates all project documentation. It is the single entry point.

## Contents

- [Architecture](#architecture) — UML diagrams (PlantUML)
- [Guides](#guides) — deployment and operations guides
- [Components](#components) — per-component READMEs and agent guidelines
- [Roadmap](#roadmap) — module roadmaps
- [Testing](#testing) — test plans

## Architecture

The UML diagrams (PlantUML source) are available in the [`architecture/diagrams/`](architecture/diagrams/) directory:

| Diagram | Path | Description |
|---------|------|-------------|
| Class Diagram | `architecture/diagrams/global-class.puml` | Global class diagram |
| Logical Architecture | `architecture/diagrams/global-logical-architecture.puml` | Logical architecture (Phase 1 microservices) |
| Physical Architecture | `architecture/diagrams/global-physical-architecture.puml` | Physical deployment architecture (Phase 1 microservices) |
| Use Case | `architecture/diagrams/global-usecase.puml` | Global use case diagram |
| Srv Module Classes | `architecture/diagrams/srv-module/Srv-Diagram.md` | Class diagram (PlantUML source) for the Srv module |

The `architecture/diagrams/sprint2/`, `architecture/diagrams/class/`, `architecture/diagrams/sequence/`, and `architecture/diagrams/usecase/` directories contain per-module diagrams for Marketplace, Delivery, Events, Services, and Partnerships.

### Diagram accuracy status (updated after the Phase 1 microservices split)

- `global-physical-architecture.puml` and `global-logical-architecture.puml` were **redesigned** for the Phase 1 microservices topology: gateway (JWT validation + X-User-* header injection), Eureka registry, five services (auth, marketplace+delivery, srv, eventplanning, partnership), the shared `esprit-common` library, and the **shared PostgreSQL** (deliberate Phase 1 constraint).
- The per-module business diagrams (`class/`, `sequence/`, `usecase/`, `sprint2/`, `srv-module/`) are business-level sources that were moved verbatim during the documentation consolidation. The Phase 1 split did not change business logic, so they remain representative; their older inaccuracies (e.g. outdated role names in `srv-module/Srv-Diagram.md`) are known and not yet redesigned.

## Guides

| Guide | Description |
|-------|-------------|
| [`guides/K8S_DEPLOYMENT.md`](guides/K8S_DEPLOYMENT.md) | Full Kubernetes (kubeadm VPS) deployment guide: cluster init, storage, ingress, cert-manager, troubleshooting |
| [`guides/backend-k8s-README.md`](guides/backend-k8s-README.md) | Backend Kubernetes manifests overview: manifest list, secret management options, production checklist, rollback |

## Components

Per-component READMEs and AI-agent coding guidelines. Each doc describes one deployable component of the monorepo.

| Document | Component | Description |
|----------|-----------|-------------|
| [`components/backend-README.md`](components/backend-README.md) | `backend/` | Maven reactor: esprit-common library, 5 microservices, eureka, gateway — build/run, ports and routes, auth model, database/Flyway policy, testing |
| [`components/backend-AGENTS.md`](components/backend-AGENTS.md) | `backend/` | Coding guidelines and build/test commands for AI agents working on the backend |
| [`components/frontend-README.md`](components/frontend-README.md) | `frontend/` | Angular frontend: features, folder structure, scripts, technologies, deployment |
| [`components/devops-README.md`](components/devops-README.md) | `devops/` | DevOps workspace: structure, services/ports, type generation, Docker and Kubernetes commands |
| [`components/devops-AGENTS.md`](components/devops-AGENTS.md) | `devops/` | Workspace-wide coding guidelines for AI agents: backend/frontend conventions, Docker/K8s commands, CI/CD pipeline |
| [`components/ml-service-data-README.md`](components/ml-service-data-README.md) | `ml-service/` | ML service data directory: generated datasets (booking, project, sentiment) and how to regenerate them |

## Roadmap

| Document | Description |
|----------|-------------|
| [`roadmap/Srv-ROADMAP.md`](roadmap/Srv-ROADMAP.md) | Srv module refactoring and feature roadmap: product direction, domain hierarchy rule, phase-by-phase execution status (phases 1–8) |

## Testing

| Document | Description |
|----------|-------------|
| [`tests/plan-de-test-xray.md`](tests/plan-de-test-xray.md) | Xray test plan (French): preconditions, coverage matrix, test cases, test plan, and execution for the EspritMarket backlog (ES-10 to ES-47) |

## API Documentation

Each service exposes its own OpenAPI/Swagger UI (they are NOT routed through the gateway; access services directly during development):

| Service | Port | Swagger UI |
|---------|------|------------|
| esprit-auth | 8081 (18081 locally if 8081 is taken) | `http://localhost:8081/swagger-ui.html` |
| esprit-marketplace | 8082 | `http://localhost:8082/swagger-ui.html` |
| esprit-srv | 8083 | `http://localhost:8083/swagger-ui.html` |
| esprit-eventplanning | 8084 | `http://localhost:8084/swagger-ui.html` |
| esprit-partnership | 8085 | `http://localhost:8085/swagger-ui.html` |

The single entrypoint for the application traffic is the gateway on `http://localhost:8088`.

## Kubernetes Deployment

> **Status: monolith-era.** The K8s manifests (`devops/k8s/`, `backend/k8s/`) and the guides below still describe the pre-split monolith. They are kept for reference; they need a rewrite for the microservices topology (one deployment per service, or the gateway as the single ingress) — planned with Phase 2.

See [`guides/K8S_DEPLOYMENT.md`](guides/K8S_DEPLOYMENT.md) for the full Kubernetes deployment guide.
