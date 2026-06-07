package net.thesphynx.espritmarket.Marketplace.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ForbiddenException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Dto.OwnerProductCreateRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OwnerProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductPromotionRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Mapper.ProductMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ICategoryRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StoreService storeService;
    private final ProductMapper productMapper;
    private final ImageStorageService imageStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ProductService(IProductRepository productRepository,
            ICategoryRepository categoryRepository,
            UserRepository userRepository,
            StoreService storeService,
            ProductMapper productMapper,
            ImageStorageService imageStorageService,
            SimpMessagingTemplate messagingTemplate) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.storeService = storeService;
        this.productMapper = productMapper;
        this.imageStorageService = imageStorageService;
        this.messagingTemplate = messagingTemplate;
    }

    // =========================================================
    // READ
    // =========================================================

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMarketProducts() {
        return productRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ NOUVEAU : retourne uniquement les produits d'une boutique spécifique.
     * Utilisé par GET /api/marketplace/products?storeId=X
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getByStoreId(Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new BadRequestException("storeId is required and must be > 0");
        }
        return productRepository.findAllByStoreIdOrderByIdDesc(storeId)
                .stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getById(Long id) {
        return productRepository.findWithStoreAndCategoryById(id)
                .map(this::toEnrichedResponse);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public ProductResponse create(ProductRequest request) {
        logger.info("product.create.stock_input: {}", request.getStock());
        requireStoreAndCategoryIds(request);
        Product entity = productMapper.toEntity(request);
        initializeBasePrice(entity);
        Product saved = productRepository.save(entity);
        notifyProductChange("CREATED", saved.getId());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String userEmail) {
        logger.info("product.create_auth.stock_input: {}", request.getStock());
        Long storeId = request.resolveStoreId();
        Long categoryId = request.resolveCategoryId();

        if (storeId == null) {
            throw new BadRequestException("Store ID is required");
        }

        Store myStore = storeService.getOwnedStoreEntity(userEmail, storeId);

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + categoryId));
            if (category.getStore() != null && !category.getStore().getId().equals(myStore.getId())) {
                logger.warn("Category belongs to another store: {}", category.getId());
            }
        }

        Product entity = productMapper.toEntity(request);
        entity.setStore(myStore);
        entity.setCategory(category);
        initializeBasePrice(entity);
        Product saved = productRepository.save(entity);
        notifyProductChange("CREATED", saved.getId());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ProductResponse createWithImage(String name, String description, Double price,
                                           Integer stock, String dimensionsLabel, Double weight,
                                           Long storeId, Long categoryId,
                                           MultipartFile image, String userEmail) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Product name is required");
        }
        if (price == null || price <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Product image is required");
        }

        String imageUrl = imageStorageService.store(image);

        Store myStore = storeService.getOwnedStoreEntity(userEmail, storeId);

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id=" + categoryId));
            if (category.getStore() != null && !category.getStore().getId().equals(myStore.getId())) {
                logger.warn("Category belongs to another store: {}", category.getId());
            }
        }

        Product product = new Product();
        product.setName(name.trim());
        product.setDescription(description);
        product.setPrice(price);
        product.setOriginalPrice(price);
        System.out.println("📦 SERVICE SAVING STOCK: " + stock);
        product.setStock(stock);
        product.setDimensionsLabel(dimensionsLabel);
        product.setWeight(weight);
        product.setImageUrl(imageUrl);
        product.setStore(myStore);
        product.setCategory(category);

        Product saved = productRepository.save(product);
        System.out.println("💾 DATABASE SAVED PRODUCT WITH STOCK: " + saved.getStock());
        logger.info("product.createWithImage.success productId={} user={} imageUrl={}",
                saved.getId(), userEmail, imageUrl);
        notifyProductChange("CREATED", saved.getId());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ProductResponse createInMyStore(OwnerProductCreateRequest request, String userEmail) {
        Store myStore = storeService.getOwnedStoreEntity(userEmail, request.resolveStoreId());
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id=" + request.getCategoryId()));
            if (category.getStore() != null && !category.getStore().getId().equals(myStore.getId())) {
                logger.warn("Category belongs to another store: {}", category.getId());
            }
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getPrice());
        logger.info("product.createInMyStore.stock_input: {}", request.getStock());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setStore(myStore);
        product.setCategory(category);
        product.setDimensionsLabel(request.getDimensionsLabel());
        product.setWeight(request.getWeight());

        Product saved = productRepository.save(product);
        logger.info("Product created by ownerId={}, productId={}", myStore.getOwner().getId(), saved.getId());
        notifyProductChange("CREATED", saved.getId());
        return toEnrichedResponse(saved);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product entity = productMapper.toEntity(request);
        entity.setId(id);
        initializeBasePrice(entity);
        Product saved = productRepository.save(entity);
        notifyProductChange("UPDATED", saved.getId());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, String userEmail) {
        ProductUpdateRequest normalizedRequest = toProductUpdateRequest(request);
        return update(id, normalizedRequest, userEmail);
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request, String userEmail) {
        Product product = getOwnedProductOrThrow(id, userEmail);

        if (product.getStore() == null || product.getStore().getId() == null) {
            throw new BadRequestException("Product has no associated store");
        }

        Long payloadStoreId = request.resolveStoreId();
        Long payloadCategoryId = request.resolveCategoryId();

        if (payloadStoreId == null) {
            payloadStoreId = product.getStore().getId();
        }
        if (payloadCategoryId == null) {
            payloadCategoryId = Optional.ofNullable(product.getCategory())
                    .map(Category::getId)
                    .orElse(null);
        }

        logger.info(
                "product.update.start productId={} user={} name={} price={} stock={} storeId={} categoryId={}",
                id, userEmail, request.getName(), request.getPrice(),
                request.getStock(), payloadStoreId, payloadCategoryId);

        if (!product.getStore().getId().equals(payloadStoreId)) {
            throw new BadRequestException("store.id does not match product store");
        }

        if (payloadCategoryId == null) {
            throw new BadRequestException("Category id is required (categoryId or category.id)");
        }

        Long categoryIdToUse = payloadCategoryId;
        Category category = categoryRepository.findById(categoryIdToUse)
                .orElseThrow(() -> new BadRequestException("Invalid category id=" + categoryIdToUse));
        if (category.getStore() != null && !category.getStore().getId().equals(product.getStore().getId())) {
            logger.warn("Category belongs to another store: {}", category.getId());
        }

        String normalizedImageUrl = normalizeAndValidateImageUrl(request.getImageUrl());

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(normalizedImageUrl);
        product.setOriginalPrice(request.getPrice());
        product.setCategory(category);
        product.setDimensionsLabel(request.getDimensionsLabel());
        product.setWeight(request.getWeight());

        try {
            Product saved = productRepository.save(product);
            logger.info("product.update.success productId={} user={} categoryId={} storeId={}",
                    id, userEmail, payloadCategoryId, payloadStoreId);
            notifyProductChange("UPDATED", saved.getId());
            return toEnrichedResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            logger.warn("product.update.invalid_data productId={} user={} message={}",
                    id, userEmail, ex.getMessage());
            throw new BadRequestException("Product update violates data constraints");
        }
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer stock, String userEmail) {
        Product product = getOwnedProductOrThrow(id, userEmail);
        if (stock == null) {
            throw new BadRequestException("Stock value is required");
        }
        product.setStock(stock);
        Product saved = productRepository.save(product);
        notifyProductChange("UPDATED_STOCK", saved.getId());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ProductResponse patch(Long id, Map<String, Object> updates) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + id));

        if (updates.containsKey("stock")) {
            Object stockObj = updates.get("stock");
            if (stockObj instanceof Number) {
                product.setStock(((Number) stockObj).intValue());
            }
        }
        Product saved = productRepository.save(product);
        notifyProductChange("UPDATED", saved.getId());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ProductResponse ownerUpdate(Long productId, OwnerProductUpdateRequest request, String userEmail) {
        Product product = getOwnedProductOrThrow(productId, userEmail);

        if (request.getName() != null) {
            product.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        product.setPrice(request.getPrice());
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        product.setOriginalPrice(request.getPrice());
        product.setDimensionsLabel(request.getDimensionsLabel());
        product.setWeight(request.getWeight());

        try {
            Product saved = productRepository.save(product);
            logger.info("product.owner_update.success productId={} user={}", productId, userEmail);
            notifyProductChange("UPDATED", saved.getId());
            return toEnrichedResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            logger.warn("product.owner_update.invalid_data productId={} user={} message={}",
                    productId, userEmail, ex.getMessage());
            throw new BadRequestException("Product update violates data constraints");
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
        notifyProductChange("DELETED", id);
    }

    @Transactional
    public void delete(Long id, String userEmail) {
        Product product = getOwnedProductOrThrow(id, userEmail);
        productRepository.delete(product);
        notifyProductChange("DELETED", id);
    }

    @Transactional
    public void ownerDelete(Long productId, String userEmail) {
        Product product = getOwnedProductOrThrow(productId, userEmail);
        productRepository.delete(product);
        logger.info("Product deleted by owner, productId={}", productId);
        notifyProductChange("DELETED", productId);
    }

    // =========================================================
    // PROMOTIONS
    // =========================================================

    @Transactional
    public ProductResponse upsertPromotion(Long productId, ProductPromotionRequest request, String userEmail) {
        Product product = getOwnedProductOrThrow(productId, userEmail);
        validatePromotionRequest(request);

        LocalDateTime now = LocalDateTime.now();
        product.setOriginalPrice(product.getPrice());
        product.setDiscountPercent(request.getDiscountPercent());
        product.setPromoStartAt(now);
        product.setPromoEndAt(request.getPromoEndAt());

        Product saved = productRepository.save(product);
        logger.info("Promotion applied by owner, productId={}, discountPercent={}",
                productId, request.getDiscountPercent());
        notifyProductChange("PROMOTION_UPDATED", saved.getId());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ProductResponse removePromotion(Long productId, String userEmail) {
        Product product = getOwnedProductOrThrow(productId, userEmail);
        clearPromotion(product);
        Product saved = productRepository.save(product);
        logger.info("Promotion removed by owner, productId={}", productId);
        notifyProductChange("PROMOTION_REMOVED", saved.getId());
        return toEnrichedResponse(saved);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private Product getOwnedProductOrThrow(Long productId, String principalName) {
        Product product = productRepository.findWithStoreAndCategoryById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + productId));

        Long ownerId = Optional.ofNullable(product.getStore())
                .map(Store::getOwner)
                .map(User::getId)
                .orElse(null);

        User authenticatedUser = userRepository.findByEmail(principalName)
                .or(() -> userRepository.findByName(principalName))
                .orElse(null);
        Long authenticatedUserId = authenticatedUser != null ? authenticatedUser.getId() : null;

        logger.info("Ownership check: principalName={}, resolvedUserId={}, productId={}, productStoreOwnerId={}",
                principalName, authenticatedUserId, productId, ownerId);

        if (authenticatedUser != null && authenticatedUser.getRole() == Role.ADMIN) {
            logger.info("Ownership check bypassed for ADMIN user: {}", principalName);
            return product;
        }

        if (authenticatedUserId == null) {
            logger.warn("Ownership denied: user not found for principal='{}' while accessing productId={}",
                    principalName, productId);
            throw new ForbiddenException("You are not allowed to modify this product");
        }

        if (ownerId == null) {
            logger.warn("Ownership denied: productId={} has no owning store/owner", productId);
            throw new ForbiddenException("You are not allowed to modify this product");
        }

        if (!ownerId.equals(authenticatedUserId)) {
            logger.warn("Ownership denied: userId={} is not ownerId={} for productId={}",
                    authenticatedUserId, ownerId, productId);
            throw new ForbiddenException("You are not allowed to modify this product");
        }
        return product;
    }

    private void requireStoreAndCategoryIds(ProductRequest request) {
        if (request.resolveStoreId() == null) {
            throw new BadRequestException("Store ID is required");
        }
    }

    private ProductUpdateRequest toProductUpdateRequest(ProductRequest request) {
        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setName(request.getName());
        updateRequest.setDescription(request.getDescription());
        updateRequest.setPrice(request.getPrice());
        updateRequest.setStock(request.getStock());
        updateRequest.setImageUrl(request.getImageUrl());
        updateRequest.setStoreId(request.resolveStoreId());
        updateRequest.setCategoryId(request.resolveCategoryId());
        updateRequest.setDimensionsLabel(request.getDimensionsLabel());
        updateRequest.setWeight(request.getWeight());
        return updateRequest;
    }

    private String normalizeAndValidateImageUrl(String imageUrl) {
        if (imageUrl == null) return null;
        String normalized = imageUrl.trim();
        if (normalized.isEmpty()) return normalized;

        boolean httpUrl = normalized.startsWith("http://") || normalized.startsWith("https://");
        boolean dataImage = normalized.startsWith("data:image/");
        if (!httpUrl && !dataImage) {
            throw new BadRequestException("imageUrl must be an http(s) URL or data:image/* URI");
        }
        if (normalized.length() > 5_500_000) {
            throw new BadRequestException("imageUrl is too long");
        }
        return normalized;
    }

    private void validatePromotionRequest(ProductPromotionRequest request) {
        if (request.getDiscountPercent() == null
                || request.getDiscountPercent() <= 0
                || request.getDiscountPercent() > 100) {
            throw new BadRequestException("discountPercent must be > 0 and <= 100");
        }
        if (request.getPromoEndAt() == null) {
            throw new BadRequestException("promoEndAt is required");
        }
        if (!request.getPromoEndAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("promoEndAt must be in the future");
        }
    }

    private ProductResponse toEnrichedResponse(Product product) {
        ProductResponse response = productMapper.toResponse(product);
        if (response == null) return null;

        int soldQuantity = 0;
        if (product.getOrderLines() != null) {
            soldQuantity = product.getOrderLines().stream()
                    .mapToInt(line -> Math.max(line.getQuantity(), 0))
                    .sum();
        }
        response.setSoldQuantity(soldQuantity);

        boolean active = isPromotionActive(product);
        response.setIsPromotionActive(active);
        response.setPromotionStatus(active ? "ACTIVE" : "INACTIVE");

        double basePrice = product.getPrice() != null ? product.getPrice() : 0.0;
        response.setDiscountedPrice(active
                ? calculateDiscountedPrice(basePrice, product.getDiscountPercent())
                : basePrice);
        response.setRemainingSeconds(active ? remainingSeconds(product.getPromoEndAt()) : 0L);

        if (!active && isPromotionExpired(product)) {
            response.setPromotionStatus("EXPIRED");
        }
        return response;
    }

    private boolean isPromotionActive(Product product) {
        if (product.getDiscountPercent() == null
                || product.getPromoStartAt() == null
                || product.getPromoEndAt() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (now.isEqual(product.getPromoStartAt()) || now.isAfter(product.getPromoStartAt()))
                && now.isBefore(product.getPromoEndAt());
    }

    private boolean isPromotionExpired(Product product) {
        return product.getPromoEndAt() != null
                && LocalDateTime.now().isAfter(product.getPromoEndAt());
    }

    private long remainingSeconds(LocalDateTime promoEndAt) {
        return Math.max(Duration.between(LocalDateTime.now(), promoEndAt).getSeconds(), 0L);
    }

    private double calculateDiscountedPrice(double basePrice, Double discountPercent) {
        if (discountPercent == null) return basePrice;
        return Math.max(basePrice * (1 - discountPercent / 100.0), 0.0);
    }

    private void initializeBasePrice(Product product) {
        if (product.getOriginalPrice() == null) {
            product.setOriginalPrice(product.getPrice());
        }
    }

    private void clearPromotion(Product product) {
        product.setDiscountPercent(null);
        product.setPromoStartAt(null);
        product.setPromoEndAt(null);
    }

    private void notifyProductChange(String action, Long productId) {
        try {
            Map<String, Object> payload = Map.of(
                    "action", action,
                    "productId", productId,
                    "timestamp", LocalDateTime.now().toString()
            );
            messagingTemplate.convertAndSend("/topic/marketplace/products", (Object) payload);
            logger.info("WebSocket broadcast sent: action={}, productId={}", action, productId);
        } catch (Exception e) {
            logger.error("Failed to send WebSocket broadcast: action={}, productId={}, error={}",
                    action, productId, e.getMessage());
        }
    }
}