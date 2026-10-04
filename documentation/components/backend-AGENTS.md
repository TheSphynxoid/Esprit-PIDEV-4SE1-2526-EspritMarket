# AGENTS.md - EspritMarket Backend

This document provides coding guidelines and build commands for AI agents working on this codebase.

## Project Overview

Spring Boot 4.0.2 backend application with Java 21, PostgreSQL, JWT authentication, gRPC support, and Prometheus metrics. The application serves multiple domains: Marketplace, Delivery, EventPlanning, Partnership, and Services (Srv).

## Build/Lint/Test Commands

### Build
```powershell
# Full build (skip tests)
./mvnw -B clean package -DskipTests

# On Windows CMD
mvnw.cmd -B clean package -DskipTests
```

### Run Application
```powershell
./mvnw spring-boot:run
# Server runs on http://localhost:8088
```

### Docker
```powershell
docker compose up --build
docker compose down
```

### Test Commands
```powershell
# Run all unit tests
./mvnw -B clean test

# Run single test class
./mvnw test -Dtest=ProductServiceTest

# Run single test method
./mvnw test -Dtest=ProductServiceTest#create_shouldMapPersistAndReturnResponse

# Run tests for specific module (by package pattern)
./mvnw test -Dtest="net.thesphynx.espritmarket.Marketplace.**"
```

## Project Structure

```
src/main/java/net/thesphynx/espritmarket/
├── Common/           # Shared: Config, Security, DTO, Entity (User), Exception, Repository
├── Marketplace/      # Products, Orders, Reviews, Stores, Categories
├── Delivery/         # Delivery tracking, Vehicles, Map integration
├── EventPlanning/    # Events, Tickets, Stalls, Reservations, Equipment
├── Partnership/      # Partner companies, Job offers, Applications, Interviews
└── Srv/              # Services, Projects, Partners, ServiceRequests
```

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

- JWT-based authentication via `JwtAuthFilter`
- Public endpoints: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health/**`, `/api/auth/**`
- All other endpoints require authentication
- Use `@PreAuthorize` for role-based access if needed

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/esprit_market` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `badis` |
| `JWT_SECRET` | JWT signing key (Base64) | (see application.properties) |
| `GOOGLE_MAPS_API_KEY` | Google Maps API key | (empty) |

## Important Files

- `pom.xml` - Maven dependencies, Java 21, Spring Boot 4.0.2
- `application.properties` - Configuration (port 8088, JWT, CORS)
- `docker-compose.yml` - Docker setup for app + PostgreSQL
- `Jenkinsfile` - CI/CD pipeline (test -> build -> docker push)
