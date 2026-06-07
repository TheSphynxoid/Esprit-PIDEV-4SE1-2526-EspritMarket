# Documentation

## Architecture Diagrams

The UML diagrams (PlantUML source) are available in the [`../diagrams/`](../diagrams/) directory:

| Diagram | Path | Description |
|---------|------|-------------|
| Class Diagram | `diagrams/global-class.puml` | Global class diagram |
| Logical Architecture | `diagrams/global-logical-architecture.puml` | Logical architecture overview |
| Physical Architecture | `diagrams/global-physical-architecture.puml` | Physical deployment architecture |
| Use Case | `diagrams/global-usecase.puml` | Global use case diagram |

The `diagrams/sprint2/`, `diagrams/class/`, `diagrams/sequence/`, and `diagrams/usecase/` directories contain per-module diagrams for Marketplace, Delivery, Events, Services, and Partnerships.

## API Documentation

The backend exposes an OpenAPI/Swagger UI when running:

- Swagger UI: `http://localhost:8088/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8088/v3/api-docs`

## Kubernetes Deployment

See [`../devops/K8S_DEPLOYMENT.md`](../devops/K8S_DEPLOYMENT.md) for the full Kubernetes deployment guide.
