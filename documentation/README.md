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
| Logical Architecture | `architecture/diagrams/global-logical-architecture.puml` | Logical architecture overview |
| Physical Architecture | `architecture/diagrams/global-physical-architecture.puml` | Physical deployment architecture |
| Use Case | `architecture/diagrams/global-usecase.puml` | Global use case diagram |
| Srv Module Classes | `architecture/diagrams/srv-module/Srv-Diagram.md` | Class diagram (PlantUML source) for the Srv module |

The `architecture/diagrams/sprint2/`, `architecture/diagrams/class/`, `architecture/diagrams/sequence/`, and `architecture/diagrams/usecase/` directories contain per-module diagrams for Marketplace, Delivery, Events, Services, and Partnerships.

### Known diagram inaccuracies (not yet fixed in the sources)

The following gaps were identified during the documentation consolidation. The `.puml` sources were moved verbatim and were **not** redesigned:

- `architecture/diagrams/global-logical-architecture.puml` shows a purely technical layering (Presentation / Application / Business Logic / Data Access) with **no business modules**, although the codebase is organized into the business modules Marketplace, Delivery, EventPlanning, Srv, Partnership, and Common.
- `architecture/diagrams/global-logical-architecture.puml:33` still shows `services --> external : REST/gRPC`, but gRPC was removed from the backend in commit `d2ab59f` ("chore: remove unused grpc and protobuf dependencies").
- `architecture/diagrams/global-physical-architecture.puml` still shows a single monolithic "Spring Boot Application" box, which no longer reflects the actual deployment (separate frontend, backend, ML service, and database components).

## Guides

| Guide | Description |
|-------|-------------|
| [`guides/K8S_DEPLOYMENT.md`](guides/K8S_DEPLOYMENT.md) | Full Kubernetes (kubeadm VPS) deployment guide: cluster init, storage, ingress, cert-manager, troubleshooting |
| [`guides/backend-k8s-README.md`](guides/backend-k8s-README.md) | Backend Kubernetes manifests overview: manifest list, secret management options, production checklist, rollback |

## Components

Per-component READMEs and AI-agent coding guidelines. Each doc describes one deployable component of the monorepo.

| Document | Component | Description |
|----------|-----------|-------------|
| [`components/backend-README.md`](components/backend-README.md) | `backend/` | Spring Boot backend: features, tech stack, architecture, setup, API endpoints, testing |
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

The backend exposes an OpenAPI/Swagger UI when running:

- Swagger UI: `http://localhost:8088/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8088/v3/api-docs`

## Kubernetes Deployment

See [`guides/K8S_DEPLOYMENT.md`](guides/K8S_DEPLOYMENT.md) for the full Kubernetes deployment guide.
