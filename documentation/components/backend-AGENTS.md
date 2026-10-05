# AGENTS.md - EspritMarket Backend

This document provides coding guidelines and build commands for AI agents working on this codebase.

## Project Overview

Maven multi-module reactor (`backend/pom.xml`, parent `esprit-market-backend` 0.2.0): a shared library (`esprit-common`), five Spring Boot 4.0.4 services (Java 21) behind a Eureka registry (`eureka-server`) and a Spring Cloud Gateway (`esprit-gateway`), all against ONE shared PostgreSQL database in Phase 1. Domains: auth (Common), Marketplace+Delivery, EventPlanning, Partnership, Srv. No gRPC (removed in `d2ab59f`).

## Build/Lint/Test Commands

### Build (from backend/, the reactor root)
```powershell
# Full reactor build (skip tests)
./mvnw -B clean package -DskipTests

# One module (+ its dependencies: parent + esprit-common)
./mvnw -B clean package -DskipTests -pl esprit-auth -am

# On Windows CMD
mvnw.cmd -B clean package -DskipTests
```

### Run a service
```powershell
# Each module is its own boot app (own application.yml + port):
./mvnw spring-boot:run -pl esprit-auth          # :8081
./mvnw spring-boot:run -pl esprit-marketplace   # :8082
./mvnw spring-boot:run -pl esprit-srv           # :8083
./mvnw spring-boot:run -pl esprit-eventplanning # :8084
./mvnw spring-boot:run -pl esprit-partnership   # :8085

# Or the whole stack: powershell devops/local-stack.ps1 start
```

### Docker
```powershell
cd devops
docker compose up --build        # builds each module via backend/Dockerfile.service (ARG MODULE)
docker compose down
```

### Test Commands
```powershell
# All modules (reactor)
./mvnw -B clean test

# Single module
./mvnw test -pl esprit-marketplace

# Single test class
./mvnw test -pl esprit-marketplace -Dtest=ProductServiceTest

# Single test method
./mvnw test -pl esprit-marketplace -Dtest=ProductServiceTest#create_shouldMapPersistAndReturnResponse

# By package pattern
./mvnw test -pl esprit-marketplace -Dtest="net.thesphynx.espritmarket.Marketplace.**"
```

Baseline: 259 tests, 0 failures (esprit-common 3, esprit-auth 11, esprit-marketplace 129, esprit-srv 42, esprit-eventplanning 42, esprit-partnership 32). Keep this true.

## Project Structure

```
backend/
├── pom.xml                 # parent aggregator (packaging=pom, Boot 4.0.4 parent)
├── Dockerfile.service      # parameterized multi-stage build (ARG MODULE)
├── esprit-common/          # shared library JAR (plain; no scanning, no datasource):
│                           #   JwtService, exceptions + GlobalExceptionHandler,
│                           #   ErrorResponse, PageResponse, User/Role entities,
│                           #   NotificationEvent/StatusTransitionEvent, XUserAuthFilter
├── esprit-auth/            # :8081  login/register/refresh/logout, /api/common/users,
│                           #        password reset, EmailService; OWNS Flyway migrations
├── esprit-marketplace/     # :8082  Marketplace + Delivery packages (shipped together:
│                           #        Order<->Delivery bidirectional cascades, shared raw SQL)
├── esprit-srv/             # :8083  services, bookings, projects, deliverables, escrow,
│                           #        wallets, notifications, PgNotify bridge
├── esprit-eventplanning/   # :8084  events, tickets, stalls, equipment, Stripe payments
├── esprit-partnership/     # :8085  job offers, applications, interviews, companies, profiles
├── eureka-server/          # :8761  Eureka registry
└── esprit-gateway/         # :8088  routes, JwtValidationFilter, X-User-* header injection
```

Inside each service, packages keep their monolith-era names (`net.thesphynx.espritmarket.<Module>`): business imports are unchanged. Each service carries Phase-1 copies of small shared infra under `Common/` (UserRepository, the security filter stack, EmailService/NotificationEventListener where needed) — this is deliberate transitional duplication, not an accident.

Each domain module follows layered architecture:
- `Config/` - OpenAPI configuration per module
- `Controller/` - REST endpoints
- `Dto/` or `DTO/` - Request/Response DTOs
- `Entity/` - JPA entities
- `Mapper/` - Entity-DTO mapping (where applicable)
- `Repository/` - Spring Data JPA interfaces
- `Service/` - Business logic

## Code Style Guidelines

### Imports
```java
// Order: java.* -> jakarta.* -> third-party -> Spring -> project packages
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import net.thesphynx.espritmarket.Module.Dto.RequestDto;
import net.thesphynx.espritmarket.Module.Service.SomeService;
```

### Entity Classes
```java
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @JsonIgnoreProperties({"products", "categories"})
    private Store store;
}
```
- Use Lombok annotations: `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`
- Use `@JsonIgnoreProperties` on relationships to prevent serialization loops
- Use `FetchType.LAZY` for relationships

### DTO Classes
```java
// Request DTO - with validation
@Data
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;
}

// Response DTO - simple data holder
@Data
public class ProductResponse {
    private Long id;
    private String name;
    private Double price;
}
```
- Request DTOs: Use `@Data` with Jakarta validation annotations
- Response DTOs: Use `@Data` with simple fields
- Suffix: `Request` for input, `Response` for output

### Repository Interfaces
```java
@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {
    // Custom query methods if needed
}
```
- Prefix interface names with `I` (e.g., `IProductRepository`)
- Annotate with `@Repository`
- Extend `JpaRepository<Entity, IdType>`

### Service Classes
```java
@Service
public class ProductService {
    private final IProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(IProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<ProductResponse> getById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse);
    }
}
```
- Use constructor injection (no `@Autowired`)
- Return `Optional<Dto>` for single-item lookups
- Use method references for mapping: `mapper::toResponse`

### Controller Classes
```java
@RestController
@RequestMapping("/api/marketplace/products")
@Tag(name = "Marketplace - Products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Products retrieved")})
    public List<ProductResponse> getAll() {
        return productService.getAll();
    }

    @PostMapping
    @Operation(summary = "Create product")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ProductRequest request) {
        if (productService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.update(id, request));
    }
}
```
- Use OpenAPI annotations: `@Tag`, `@Operation`, `@ApiResponses`, `@ApiResponse`
- Use `@Valid` for request body validation
- Return `ResponseEntity<T>` for endpoints with different status codes

### Mapper Classes
```java
@Component
public class ProductMapper {
    public Product toEntity(ProductRequest request) {
        if (request == null) return null;
        Product product = new Product();
        product.setName(request.getName());
        return product;
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        return response;
    }
}
```
- Manual mapping (no MapStruct)
- Null-check inputs
- Create proxy entities for relationships by setting only the ID

### Exception Handling
```java
// Custom exceptions extend RuntimeException
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));
    }
}

// Use existing exceptions:
// - BadRequestException - for invalid input
// - ResourceNotFoundException - for 404
// - UnauthorizedException - for auth failures
// - IllegalArgumentException - for business rule violations
// - IllegalStateException - for conflict states
```

### Error Response Format
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}
```

## Testing Guidelines

### Unit Test Structure
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService service;

    @Test
    void getById_whenFound_shouldReturnMappedItem() {
        var id = 1L;
        var entity = new Product();
        var response = new ProductResponse();
        
        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productMapper.toResponse(entity)).thenReturn(response);

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(response, result.get());
        verify(productRepository).findById(id);
    }
}
```
- Use JUnit 5 with Mockito
- Test class naming: `ClassNameTest`
- Test method naming: `methodName_whenCondition_shouldExpectedBehavior`
- Use `var` for local variables in tests
- Verify mock interactions

## Security

- The **gateway** (`esprit-gateway`, :8088) validates JWTs (signature, expiry, `type=access` via `esprit-common` `JwtService`), strips `Authorization` and client-supplied `X-User-*`, then injects `X-User-Email` / `X-User-Id` / `X-User-Roles` (+ `X-Gateway-Token`).
- Each service uses `XUserAuthFilter` (from esprit-common) which rebuilds the SecurityContext from those headers — the principal's username is the user email, so `@AuthenticationPrincipal` and `@PreAuthorize` keep working. Services do NOT parse `Authorization` and must NOT read a local user table for authorization.
- Per-service `SecurityConfig` files declare that module's public paths (`permitAll`) plus OPTIONS/swagger/actuator; everything else is `authenticated()`.
- Authorization MUST read the JWT role claim (carried in `X-User-Roles`), never a local user table.
- Logout blacklisting (`TokenBlacklistService`) lives only in esprit-auth and is per-instance/in-memory — a known Phase 1 limitation (a logged-out access token stays valid until expiry; the gateway does not consult the blacklist).

## Environment Variables

Identical `JWT_SECRET` must be supplied to the gateway AND all services (JwtService Base64-decodes it; empty fails on first token use).

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL (SHARED DB in Phase 1) | `jdbc:postgresql://localhost:5432/esprit_market` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | JWT signing key (Base64) | (empty — must be provided) |
| `GATEWAY_SHARED_TOKEN` | Shared secret the gateway injects; services require it in `X-User-*` trust checks | (empty = trust mode) |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka registry URL | `http://localhost:8761/eureka/` |
| `GOOGLE_MAPS_API_KEY` | Google Maps API key (esprit-marketplace) | (empty) |

## Important Files

- `backend/pom.xml` - parent aggregator (Boot 4.0.4, Spring Cloud 2025.1.0 BOM)
- `<module>/application.yml` - per-service configuration (port, datasource, flyway off except auth)
- `esprit-auth/src/main/resources/db/migration/` - THE shared-DB migration set (only auth runs Flyway)
- `devops/db/init/01-espritmarket-base-schema.sql` - base schema for fresh databases (pre-baseline legacy tables are not in any migration)
- `devops/docker-compose.yml` - microservices stack (db, eureka, 5 services, gateway, frontend, ml, monitoring)
- `devops/local-stack.ps1` - run the whole stack locally without compose
- `backend/Dockerfile.service` - parameterized per-module image build
