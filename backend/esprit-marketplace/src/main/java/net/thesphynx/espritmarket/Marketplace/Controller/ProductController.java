package net.thesphynx.espritmarket.Marketplace.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Common.Exception.UnauthorizedException;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace/products")
@Tag(name = "Marketplace - Products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * ✅ CORRIGÉ : retourne tous les produits OU filtre par storeId si fourni.
     * GET /api/marketplace/products           → tous les produits
     * GET /api/marketplace/products?storeId=3 → produits de la boutique 3 uniquement
     */
    @GetMapping
    @Operation(summary = "List products (optionally filtered by storeId)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Products retrieved")})
    public List<ProductResponse> getAll(
            @RequestParam(value = "storeId", required = false) Long storeId) {
        if (storeId != null && storeId > 0) {
            return productService.getByStoreId(storeId);
        }
        return productService.getMarketProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create product (JSON)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(productService.create(request));
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create product with image upload")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ProductResponse createWithImage(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") Double price,
            @RequestParam("stock") String stockStr,
            @RequestParam("storeId") Long storeId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "dimensionsLabel", required = false) String dimensionsLabel,
            @RequestParam(value = "weight", required = false) Double weight,
            @RequestPart("image") org.springframework.web.multipart.MultipartFile image,
            Authentication authentication) {

        Integer stockValue;
        try {
            stockValue = Integer.parseInt(stockStr.trim());
        } catch (NumberFormatException e) {
            System.err.println("❌ FAILED TO PARSE STOCK: " + stockStr);
            stockValue = 0;
        }

        return productService.createWithImage(
                name, description, price, stockValue,
                dimensionsLabel, weight, storeId, categoryId,
                image, authentication.getName());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            Authentication authentication) {
        String principal = requirePrincipal(authentication);
        return ResponseEntity.ok(productService.update(id, request, principal));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of product (e.g. stock)")
    public ResponseEntity<ProductResponse> patch(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> updates) {
        return ResponseEntity.ok(productService.patch(id, updates));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String requirePrincipal(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
        return authentication.getName();
    }
}