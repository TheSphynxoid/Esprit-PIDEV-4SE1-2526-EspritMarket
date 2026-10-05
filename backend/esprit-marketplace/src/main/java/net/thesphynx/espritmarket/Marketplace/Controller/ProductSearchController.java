package net.thesphynx.espritmarket.Marketplace.Controller;

import net.thesphynx.espritmarket.Marketplace.Dto.ProductSearchResponse;
import net.thesphynx.espritmarket.Marketplace.Service.ProductSearchService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search/products")
public class ProductSearchController {
    private final ProductSearchService productSearchService;

    public ProductSearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductSearchResponse>> searchProducts(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(productSearchService.searchProducts(
                q,
                minPrice,
                maxPrice,
                category,
                page,
                size
        ));
    }
}
