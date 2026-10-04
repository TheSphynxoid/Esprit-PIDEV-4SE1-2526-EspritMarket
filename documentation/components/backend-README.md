# EspritMarket Backend

> A comprehensive, well-structured monolithic Spring Boot application for a full-featured marketplace with integrated delivery management, event planning, and partnership features.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-green?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue?logo=maven)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-blue?logo=postgresql)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Authentication](#authentication)
- [Database](#database)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

EspritMarket Backend is a comprehensive, modular Spring Boot application designed to power a full-featured marketplace ecosystem. It features a clean, layered architecture organized by functional domains (Common, Delivery, Marketplace, EventPlanning, Partnership) within a single cohesive application. It provides robust support for:

- **User Management**: Registration, authentication, and profile management
- **Marketplace Operations**: Product listings, categories, and transactions
- **Delivery Management**: Order tracking, courier management, and logistics coordination
- **Event Planning**: Event creation, management, and collaboration
- **Partnership Management**: Partner onboarding, contracts, and collaboration tools
- **High-Performance Communication**: gRPC services for internal low-latency operations

The backend follows modern architectural patterns including JWT-based authentication, modular domain-driven design, caching strategies, and comprehensive error handling.

## ✨ Features

### Core Functionality
- ✅ **JWT Authentication**: Secure token-based authentication with role-based access control (RBAC)
- ✅ **User Management**: Complete user lifecycle management with profile customization
- ✅ **Product Management**: Comprehensive product catalog with categories and inventory tracking
- ✅ **Order Processing**: Full order lifecycle from creation to delivery
- ✅ **Delivery Tracking**: Real-time delivery status updates with Google Maps integration
- ✅ **Event Management**: Flexible event creation and attendee management
- ✅ **Partnership Management**: Partner registration and collaboration features

### Technical Features
- ✅ **gRPC Services**: High-performance internal communication layer
- ✅ **REST API**: Complete REST API with OpenAPI/Swagger documentation
- ✅ **Database Migrations**: Automated schema versioning with Flyway
- ✅ **Caching**: Multi-level caching for improved performance
- ✅ **Security**: Spring Security integration with JWT tokens
- ✅ **Validation**: Comprehensive input validation with Bean Validation
- ✅ **Email Notifications**: Built-in email service for user communications
- ✅ **API Documentation**: Interactive Swagger UI for API exploration
- ✅ **Exception Handling**: Centralized error handling with meaningful responses

## 🛠️ Tech Stack

### Backend Framework
- **Java 21**: Latest LTS version with modern language features
- **Spring Boot 4.0.2**: Enterprise-grade monolithic application framework
- **Spring Cloud 2025.1.0**: Cloud-ready capabilities for deployment flexibility

### Data & Persistence
- **Spring Data JPA**: Object-relational mapping
- **PostgreSQL**: Production-grade relational database
- **Flyway**: Database schema migration and versioning
- **Hibernate**: Advanced ORM capabilities

### Communication & API
- **gRPC 1.77.1**: High-performance internal RPC framework
- **Protocol Buffers 4.33.4**: Efficient message serialization
- **REST Endpoints**: RESTful API for client applications
- **SpringDoc OpenAPI 2.6.0**: Automatic API documentation with Swagger UI

### Security & Authentication
- **Spring Security**: Authentication and authorization
- **JWT (JJWT 0.12.3)**: JSON Web Tokens for stateless authentication
- **Spring Validation**: Input validation and constraint enforcement

### Additional Libraries
- **Lombok**: Reduce boilerplate code
- **Spring Mail**: Email sending capabilities
- **Spring Cache**: In-memory caching

### Development & Testing
- **Spring Boot DevTools**: Fast application restarts
- **JUnit 5**: Testing framework
- **Mockito**: Mocking library
- **H2 Database**: In-memory database for testing

## 🏗️ Architecture

### Modular Monolith Architecture

```
┌──────────────────────────────────────────────────────────────┐
│              REST Client Requests (Angular, Postman, etc.)    │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │  REST Controllers Layer       │
        │  ┌──────────────────────────┐│
        │  │ Common       Delivery     ││
        │  │ Marketplace  Events       ││
        │  │ Partnership  Auth         ││
        │  └──────────────────────────┘│
        └──────────────┬───────────────┘
                       │
          ┌────────────┴──────────────┐
          │                           │
          ▼                           ▼
  ┌─────────────────┐       ┌──────────────────┐
  │  Service Layer  │       │ gRPC Internal    │
  │ - Business      │       │   Services       │
  │   Logic         │       │ (for high-perf   │
  │ - Validation    │       │  operations)     │
  │ - Transactions  │       │                  │
  └────────┬────────┘       └────────┬─────────┘
           │                         │
           └────────────┬────────────┘
                        │
        ┌───────────────┴───────────────┐
        │   Data Access Layer (JPA)     │
        │  - Repositories               │
        │  - Entity Mapping             │
        │  - Query Building             │
        └───────────────┬───────────────┘
                        │
                        ▼
                ┌───────────────────┐
                │  PostgreSQL DB    │
                │  with Flyway      │
                │  Versioning       │
                └───────────────────┘
```

### Module Structure (Domain-Driven Organization)

```
esprit-market-backend/
├── Common/           # Shared entities, DTOs, exceptions, configurations
│   ├── Config/       # Spring configurations and beans
│   ├── Security/     # JWT, authentication, security filters
│   ├── Exception/    # Global exception handling
│   └── Entity/       # Base entities and repositories
│
├── Delivery/         # Delivery management domain
│   ├── Service/      # Delivery & courier business logic
│   ├── Controller/   # REST endpoints
│   ├── Entity/       # Domain entities
│   └── DTO/          # Data transfer objects
│
├── Marketplace/      # Product catalog and transactions domain
│   ├── Service/      # Product business logic
│   ├── Controller/   # REST endpoints
│   └── Entity/       # Domain entities
│
├── EventPlanning/    # Event management domain
│   ├── Service/      # Event business logic
│   ├── Controller/   # REST endpoints
│   └── Entity/       # Domain entities
│
├── Partnership/      # Partnership management domain
│   ├── Service/      # Partnership business logic
│   ├── Controller/   # REST endpoints
│   └── Entity/       # Domain entities
│
└── Srv/             # gRPC service definitions
    └── proto/       # Protocol buffer definitions
```

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher
  ```bash
  java --version
  ```
- **Maven 3.8+** for dependency management
  ```bash
  mvn --version
  ```
- **PostgreSQL 12+** for the main database
  ```bash
  psql --version
  ```
- **Git** for version control
  ```bash
  git --version
  ```

### Environment Variables Required

```powershell
# Google Maps API Configuration
$env:GOOGLE_MAPS_API_KEY="your_google_maps_key"

# Database Configuration
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="espritmarket"
$env:DB_USER="postgres"
$env:DB_PASSWORD="your_password"

# JWT Configuration
$env:JWT_SECRET="your_jwt_secret_key_min_256_bits"
$env:JWT_EXPIRATION="86400000"  # 24 hours in milliseconds

# Email Configuration
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your_email@gmail.com"
$env:MAIL_PASSWORD="your_email_password"
```

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/esprit-market-backend.git
cd esprit-market-backend
```

### 2. Install Dependencies

```bash
mvn clean install
```

### 3. Configure Environment Variables

For Windows PowerShell:
```powershell
$env:GOOGLE_MAPS_API_KEY="your_key"
$env:DB_HOST="localhost"
$env:DB_NAME="espritmarket"
$env:DB_USER="postgres"
$env:DB_PASSWORD="password"
$env:JWT_SECRET="your_secret_key"
```

For Linux/macOS:
```bash
export GOOGLE_MAPS_API_KEY="your_key"
export DB_HOST="localhost"
export DB_NAME="espritmarket"
export DB_USER="postgres"
export DB_PASSWORD="password"
export JWT_SECRET="your_secret_key"
```

## ⚙️ Configuration

### Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE espritmarket;
CREATE USER esprit_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE espritmarket TO esprit_user;
```

2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/espritmarket
spring.datasource.username=esprit_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Google Maps Configuration

Verify Maps configuration at runtime:
```bash
curl -X GET http://localhost:8080/api/delivery/maps/config
```

Expected response:
```json
{
    "configured": true,
    "baseUrl": "https://maps.googleapis.com/maps/api"
}
```

### Application Properties

Key configurations in `application.properties`:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api

# JWT Configuration
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration=86400000

# Email Configuration
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Caching
spring.cache.type=simple

# Logging
logging.level.root=INFO
logging.level.net.thesphynx.espritmarket=DEBUG
```

## ▶️ Running the Application

### Using Maven

```bash
# Development mode (with DevTools)
mvn spring-boot:run

# Production mode
mvn clean package
java -jar target/EspritMarket-0.0.1-SNAPSHOT.jar
```

### Using IDE (IntelliJ IDEA/Eclipse)

1. Open project in IDE
2. Create Run Configuration for `EspritMarketApplication`
3. Configure environment variables in Run Configuration
4. Click Run

### Using Docker (Optional)

```bash
# Build Docker image
docker build -t esprit-market:latest .

# Run container
docker run -e GOOGLE_MAPS_API_KEY="your_key" \
           -e DB_HOST="postgres-host" \
           -p 8080:8080 \
           esprit-market:latest
```

## 📚 API Documentation

### Accessing Swagger UI

Once the application is running:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### Core API Endpoints

#### Authentication
```bash
# Register new user
POST /api/auth/register
Content-Type: application/json
{
    "email": "user@example.com",
    "password": "securePassword123",
    "firstName": "John",
    "lastName": "Doe"
}

# Login user
POST /api/auth/login
Content-Type: application/json
{
    "email": "user@example.com",
    "password": "securePassword123"
}

# Response includes JWT token
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400000,
    "user": {
        "id": "uuid",
        "email": "user@example.com",
        "roles": ["USER"]
    }
}
```

#### Marketplace
```bash
# Get products
GET /api/marketplace/products

# Create product
POST /api/marketplace/products
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
{
    "name": "Product Name",
    "description": "Product Description",
    "price": 99.99,
    "categoryId": "category-uuid"
}
```

#### Delivery
```bash
# Get delivery configuration
GET /api/delivery/maps/config

# Create delivery order
POST /api/delivery/orders
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
{
    "orderId": "order-uuid",
    "pickupLocation": "Location 1",
    "deliveryLocation": "Location 2",
    "courierId": "courier-uuid"
}

# Track delivery
GET /api/delivery/orders/{orderId}/track
```

#### Events
```bash
# Get events
GET /api/events

# Create event
POST /api/events
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
{
    "title": "Event Title",
    "description": "Event Description",
    "date": "2024-12-31T18:00:00Z",
    "isOnline": true
}
```

## 📁 Project Structure

```
src/main/java/net/thesphynx/espritmarket/
├── Common/
│   ├── Config/              # Spring configurations
│   ├── Controller/          # REST controllers
│   ├── DTO/                 # Data Transfer Objects
│   ├── Entity/              # Domain entities
│   ├── Exception/           # Custom exceptions
│   ├── Repository/          # Data access layer
│   ├── Security/            # JWT and security filters
│   └── Service/             # Business logic
├── Delivery/
│   ├── Controller/          # Delivery endpoints
│   ├── Service/             # Delivery services (CourierService, etc.)
│   ├── Entity/              # Delivery entities
│   └── DTO/                 # Delivery DTOs
├── EventPlanning/
│   ├── Controller/          # Event endpoints
│   ├── Service/             # Event services
│   └── Entity/              # Event entities
├── Marketplace/
│   ├── Controller/          # Product endpoints
│   ├── Service/             # Product services
│   └── Entity/              # Product entities
├── Partnership/
│   ├── Controller/          # Partnership endpoints
│   ├── Service/             # Partnership services
│   └── Entity/              # Partnership entities
└── Srv/                     # gRPC service definitions

src/main/resources/
├── application.properties   # Main configuration
├── application-test.properties  # Test configuration
├── db/migration/            # Flyway migration scripts
└── proto/                   # Protocol Buffer definitions
```

## 🔐 Authentication

### JWT Implementation

The backend uses JWT (JSON Web Tokens) for stateless authentication:

**Token Structure:**
- **Header**: Algorithm (HS256)
- **Payload**: 
  - `sub`: User email
  - `roles`: User roles (ADMIN, USER, COURIER, etc.)
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp (24 hours)
- **Signature**: HMAC-SHA256 with secret key

**Usage:**
```bash
# Include token in Authorization header
curl -X GET http://localhost:8080/api/auth/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Key Files:**
- `Security/JwtService.java`: Token generation and validation
- `Security/JwtAuthenticationFilter.java`: Request filtering
- `Security/SecurityConfig.java`: Security configuration

## 💾 Database

### Schema Management

Flyway is used for database versioning and migration:

```bash
# Migrations are automatically applied on startup
# Location: src/main/resources/db/migration/
# Format: V<version>__<description>.sql
```

**Current Migrations:**
- `V1__add_is_online_to_event.sql`: Online event flag support

**Running Migrations:**
```bash
# Manual migration (if needed)
mvn flyway:migrate

# Validate schema
mvn flyway:validate
```

### Entity Relationships

Key entities and their relationships:
- **User** ↔ Orders, Events, Partnerships
- **Product** → Category, Orders
- **Order** ↔ Delivery, User
- **Delivery** → Courier, Order
- **Event** → Attendees, Organizer

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthServiceTest

# Run with coverage
mvn test jacoco:report

# View coverage report
# target/site/jacoco/index.html
```

### Test Configuration

- Uses H2 in-memory database
- Spring Security Test utilities
- Mockito for mocking dependencies
- Configuration: `src/test/resources/application-test.properties`

## 📊 Performance & Optimization

### Caching Strategy
- Spring Cache abstraction with simple provider
- Cache key patterns for frequently accessed data
- TTL configurations per entity type

### Database Optimization
- Indexed columns for better query performance
- Batch operations for bulk updates
- Query optimization with proper JPA relationships

### gRPC Services
- Protobuf message serialization for efficiency
- Streaming capabilities for large datasets
- Built-in compression and multiplexing

## 🐛 Troubleshooting

### Common Issues

**1. Database Connection Error**
```
Error: FATAL: password authentication failed
Solution: 
- Verify PostgreSQL is running: pg_isready
- Check credentials in application.properties
- Ensure database and user exist
```

**2. JWT Token Invalid**
```
Error: Invalid JWT token
Solution:
- Verify JWT_SECRET environment variable is set
- Token may have expired (24-hour expiration)
- Check Authorization header format: "Bearer <token>"
```

**3. Google Maps API Error**
```
Error: Maps API key not found
Solution:
- Set GOOGLE_MAPS_API_KEY environment variable
- Verify API key is valid and enabled in Google Cloud Console
- Check /api/delivery/maps/config endpoint
```

**4. Port Already in Use**
```
Error: Address already in use :8080
Solution:
- Change port in application.properties: server.port=8081
- Or kill process using port 8080
```

## 📝 Contributing

### Getting Started

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Code Standards

- Follow Google Java Style Guide
- Use meaningful variable and method names
- Add JavaDoc for public methods
- Write unit tests for new features
- Keep methods focused and single-responsibility

### Pull Request Process

1. Update README.md with any new features
2. Ensure all tests pass: `mvn test`
3. Add/update documentation
4. Get review approval
5. Merge to main branch

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Development Team**: ESPRIT School
- **Project**: EspritMarket Marketplace Platform

## 📞 Support & Contact

- **Email**: support@espritmarket.com
- **Issues**: [GitHub Issues](https://github.com/yourusername/esprit-market-backend/issues)
- **Documentation**: See `/docs` folder for additional guides

## 🎓 Learning Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Guide](https://spring.io/projects/spring-security)
- [JWT Tutorial](https://jwt.io/introduction)
- [gRPC Java Documentation](https://grpc.io/docs/languages/java/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

**Last Updated**: March 31, 2026
**Version**: 0.0.1-SNAPSHOT
**Status**: Active Development
