package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.ProductSearchResponse;
import net.thesphynx.espritmarket.Marketplace.Repository.ProductSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSearchService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;

    private final ProductSearchRepository productSearchRepository;

    public ProductSearchService(ProductSearchRepository productSearchRepository) {
        this.productSearchRepository = productSearchRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductSearchResponse> searchProducts(String q,
                                                      Double minPrice,
                                                      Double maxPrice,
                                                      String category,
                                                      Integer page,
                                                      Integer size) {
        int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        String normalizedQuery = normalizeText(q);
        String normalizedCategory = normalizeText(category);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Object[]> rawPage = productSearchRepository.searchProducts(
                normalizedQuery,
                minPrice,
                maxPrice,
                normalizedCategory,
                pageable
        );

        return rawPage.map(this::toResponse);
    }

    private ProductSearchResponse toResponse(Object[] row) {
        Long productId = row[0] != null ? ((Number) row[0]).longValue() : null;
        String name = row[1] != null ? String.valueOf(row[1]) : "";
        Double price = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        String storeName = row[3] != null ? String.valueOf(row[3]) : "";
        String categoryName = row[4] != null ? String.valueOf(row[4]) : "";

        return new ProductSearchResponse(productId, name, price, storeName, categoryName);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}
