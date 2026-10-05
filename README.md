# EspritMarket

## Description

EspritMarket is a full-stack marketplace platform designed for the ESPRIT student community. It integrates an e-commerce marketplace (products, orders, reviews, stores), a delivery tracking system with map integration, an event planning module with ticketing and stall reservations, a partnership hub with job offers, and an AI-powered ML service for booking completion prediction, project delay risk assessment, and sentiment analysis.

## Technologies utilisees

**Frontend :** Angular 21, TypeScript, Tailwind CSS, Angular Material, Spartan NG

**Backend :** Java 21, Spring Boot 4.0.4, Spring Cloud 2025.1.0 (Eureka, Spring Cloud Gateway), Spring Security (JWT), Spring Data JPA, Flyway, Prometheus, WebSocket — découpé en 5 microservices (Phase 1)

**Base de donnees :** PostgreSQL 16

**ML Service :** Python 3.12, FastAPI, LightGBM, scikit-learn, Sentiment140 dataset

**Infrastructure :** Docker, Docker Compose, Kubernetes, Prometheus, Grafana

## prerequis

- Docker and Docker Compose
- Node.js 20+ (for local frontend dev only)
- JDK 21 (for local backend dev only)
- Python 3.12 (for local ML service dev only)

## Installation

Clone the repository and copy the environment file:

```bash
git clone https://github.com/TheSphynxoid/Esprit-PIDEV-4SE1-2526-EspritMarket.git
cd EspritMarket
cp .env.example .env
```

Edit `.env` and fill in your values (see Variables d'environnement below).

## Lancement

With Docker (recommended):

```bash
docker compose -f devops/docker-compose.yml up --build
```

The application will be available at:
- Frontend: http://localhost
- Gateway (API entrypoint): http://localhost:8088
- Eureka console: http://localhost:8761
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

Swagger UI is exposed per service (8081-8085), not through the gateway.

### Local Development

If you prefer to run services individually (Phase 1 microservices, one shared PostgreSQL):

```powershell
# 0. (optional) seed a fresh database from devops/db/init/
docker run -d --name esprit-local-db -e POSTGRES_DB=esprit_market -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 55432:5432 -v "<repo>/devops/db/init:/docker-entrypoint-initdb.d:ro" postgres:16-alpine

# 1. Build all modules (backend/ is a Maven reactor)
cd backend
./mvnw package -DskipTests

# 2. Start the whole stack: eureka -> auth (Flyway owner) -> services -> gateway
powershell -ExecutionPolicy Bypass -File ..\devops\local-stack.ps1 start
#    (status / stop subcommands available)

# 3. Frontend (Angular)
cd ..\frontend
npm install --legacy-peer-deps
npm start

# 4. ML Service (Python)
cd ..\ml-service
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python train.py --download-sentiment140
uvicorn main:app --host 0.0.0.0 --port 8000
```

## Variables d'environnement

See [`.env.example`](.env.example) for the complete list.

| Variable | Description | Required |
|----------|-------------|----------|
| `POSTGRES_USER` | Database username | Yes |
| `POSTGRES_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | JWT signing key (min 256 bits) — **identical for the gateway and all services** | Yes |
| `GATEWAY_SHARED_TOKEN` | Shared secret the gateway injects as `X-Gateway-Token`; services require it to trust `X-User-*` headers | Recommended |
| `GOOGLE_MAPS_API_KEY` | Google Maps API key | No |
| `GRAFANA_PASSWORD` | Grafana admin password | No |

## Infrastructure (Cloud / Kubernetes)

> **Note**: the Kubernetes manifests (`devops/k8s/`) and their guides are monolith-era — they still deploy the pre-split backend image. A microservices rewrite is planned with Phase 2.

See [documentation/guides/K8S_DEPLOYMENT.md](documentation/guides/K8S_DEPLOYMENT.md).

### Architecture (Phase 1 microservices)

```
Browser → Angular (nginx :80 / ng serve :4200)
        → Spring Cloud Gateway :8088  (validates JWT, injects X-User-* headers)
        → lb:// via Eureka :8761
        → esprit-auth :8081      (users, login, /api/auth/**, /api/common/**)
        → esprit-marketplace :8082 (Marketplace + Delivery)
        → esprit-srv :8083       (services, bookings, projects)
        → esprit-eventplanning :8084 (events, tickets, stalls)
        → esprit-partnership :8085 (job offers, applications)
        → shared PostgreSQL :5432  (Phase 1: one database for all services)
```

`esprit-common` is a shared library JAR (JWT service, exceptions, User/Role entities). Architecture diagrams (PlantUML) live in `documentation/architecture/diagrams/` and are documented in [documentation/README.md](documentation/README.md).

## ML Service (IA)

The ML service provides three prediction models:

| Model | Endpoint | Accuracy |
|-------|----------|----------|
| Booking Completion | `POST /predict/booking` | ~0.95 |
| Project Delay | `POST /predict/project` | ~0.90 |
| Sentiment Analysis | `POST /predict/sentiment` | ~0.80 |

Models are automatically trained on first startup using synthetic data and the Sentiment140 dataset. They are stored as `.joblib` files (not versioned in git) and regenerated by `python train.py --download-sentiment140`.

For more details, see `ml-service/data/README.md`.

## Demo

See [demo/](demo/) for screenshots and video links.

## Architecture

Phase 1 organizes the backend as five Spring Boot services (auth, marketplace+delivery, srv, eventplanning, partnership) behind a Eureka registry and a Spring Cloud Gateway, with a shared library (`esprit-common`) and **one shared PostgreSQL database** (deliberate Phase 1 constraint; database-per-service is Phase 2). Each service keeps a layered structure per domain module:
- Controller layer (REST endpoints with OpenAPI annotations)
- Service layer (business logic)
- Repository layer (Spring Data JPA)
- Entity/DTO layers with manual mappers

## Auteurs

- Hamza Jamil Saied -- Classe : 4SE1 -- Année 2526
- Hazem Omri -- Classe : 4SE1 -- Année 2526
- Badis Ghaoui -- Classe : 4SE1 -- Année 2526
- Wajdi Majbri -- Classe : 4SE1 -- Année 2526
- Baha eddine Jouil -- Classe : 4SE1 -- Année 2526

**Tuteurs :** Chahnez Sardouk, Sirine Naifar
