package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ForbiddenException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Dto.OwnerProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductPromotionRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Mapper.ProductMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ICategoryRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;
import net.thesphynx.espritmarket.Common.Entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private IProductRepository productRepository;
    private ICategoryRepository categoryRepository;
    private UserRepository userRepository;
    private StoreService storeService;
    private SimpMessagingTemplate messagingTemplate;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(IProductRepository.class);
        categoryRepository = Mockito.mock(ICategoryRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        storeService = Mockito.mock(StoreService.class);
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        productService = new ProductService(productRepository, categoryRepository, userRepository, storeService, 
                new ProductMapper(), Mockito.mock(ImageStorageService.class), messagingTemplate);
    }

    @Test
    void ownerCanUpdateOwnProduct() {
        Product product = productWithOwner(10L, 1L);
        OwnerProductUpdateRequest request = new OwnerProductUpdateRequest();
        request.setName("Updated Laptop");
        request.setDescription("Updated desc");
        request.setPrice(80.0);
        request.setStock(7);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user(1L, "owner@test.com")));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.ownerUpdate(10L, request, "owner@test.com");

        assertEquals("Updated Laptop", response.getName());
        assertEquals(80.0, response.getPrice());
        assertEquals("Updated desc", response.getDescription());
        assertEquals(7, response.getStock());
    }

    @Test
    void adminCanModifyAnyProduct() {
        Product product = productWithOwner(10L, 1L); // Owned by user 1
        OwnerProductUpdateRequest request = new OwnerProductUpdateRequest();
        request.setName("Admin Override");
        request.setPrice(50.0);

        // Current user is Admin (user 999)
        User admin = user(999L, "admin@test.com");
        admin.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Should not throw ForbiddenException
        ProductResponse response = productService.ownerUpdate(10L, request, "admin@test.com");

        assertEquals("Admin Override", response.getName());
        assertEquals(50.0, response.getPrice());
    }

    @Test
    void nonOwnerReceives403() {
        Product product = productWithOwner(10L, 1L);

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(user(2L, "other@test.com")));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));

        ForbiddenException exception = assertThrows(ForbiddenException.class,
            () -> productService.ownerDelete(10L, "other@test.com"));

        assertEquals("You are not allowed to modify this product", exception.getMessage());
        }

        @Test
        void legacyUpdateAllowsMissingStoreAndCategoryWhenOwner() {
        Product product = productWithOwner(10L, 1L);
        Category category = new Category();
        category.setId(99L);
        category.setStore(product.getStore());
        product.setCategory(category);
        product.setImageUrl("https://old.example.com/image.jpg");

        ProductRequest request = new ProductRequest();
        request.setName("Legacy updated");
        request.setDescription("Legacy description");
        request.setPrice(199.0);
        request.setStock(4);
        request.setImageUrl("");
        request.setStoreId(null);
        request.setCategoryId(null);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user(1L, "owner@test.com")));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(99L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.update(10L, request, "owner@test.com");

        assertEquals("Legacy updated", response.getName());
        assertEquals(99L, response.getCategoryId());
        assertEquals(5L, response.getStoreId());
        assertEquals("", response.getImageUrl());
    }

    @Test
    void promoActiveCalculatesDiscountedPrice() {
        Product product = productWithOwner(10L, 1L);
        product.setPrice(100.0);

        ProductPromotionRequest request = new ProductPromotionRequest();
        request.setDiscountPercent(20.0);
        request.setPromoEndAt(LocalDateTime.now().plusHours(2));

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user(1L, "owner@test.com")));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.upsertPromotion(10L, request, "owner@test.com");

        assertTrue(Boolean.TRUE.equals(response.getIsPromotionActive()));
        assertEquals(80.0, response.getDiscountedPrice());
        assertTrue(response.getRemainingSeconds() > 0);
    }

    @Test
    void expiredPromoBecomesInactive() {
        Product product = productWithOwner(10L, 1L);
        product.setPrice(120.0);
        product.setDiscountPercent(50.0);
        product.setPromoStartAt(LocalDateTime.now().minusDays(2));
        product.setPromoEndAt(LocalDateTime.now().minusMinutes(10));

        when(productRepository.findAllByOrderByIdDesc()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getMarketProducts();

        assertEquals(1, responses.size());
        ProductResponse response = responses.get(0);
        assertFalse(Boolean.TRUE.equals(response.getIsPromotionActive()));
        assertEquals(120.0, response.getDiscountedPrice());
        assertEquals(0L, response.getRemainingSeconds());
    }

    @Test
    void updateWithNestedStoreAndCategoryIdsSucceeds() {
        Product product = productWithOwner(10L, 1L);
        Category category = new Category();
        category.setId(2L);
        category.setName("Backpacks");
        category.setStore(product.getStore());
        product.setCategory(category);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("crk");
        request.setDescription("sac for");
        request.setPrice(550.0);
        request.setStock(5);
        request.setImageUrl("data:image/png;base64,AAAB");

        ProductUpdateRequest.RelationRef storeRef = new ProductUpdateRequest.RelationRef();
        storeRef.setId(5L);
        request.setStore(storeRef);

        ProductUpdateRequest.RelationRef categoryRef = new ProductUpdateRequest.RelationRef();
        categoryRef.setId(2L);
        request.setCategory(categoryRef);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user(1L, "owner@test.com")));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.update(10L, request, "owner@test.com");

        assertEquals("crk", response.getName());
        assertEquals("sac for", response.getDescription());
        assertEquals(550.0, response.getPrice());
        assertEquals(5, response.getStock());
        assertEquals("data:image/png;base64,AAAB", response.getImageUrl());
        assertEquals(5L, response.getStoreId());
        assertEquals(2L, response.getCategoryId());
    }

    @Test
    void updateReturns404WhenProductMissing() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("crk");
        request.setDescription("sac for");
        request.setPrice(550.0);
        request.setStock(5);
        request.setStoreId(1L);
        request.setCategoryId(2L);

        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(10L, request, "owner@test.com"));
    }

    @Test
    void updateReturns400WhenCategoryReferenceMissing() {
        Product product = productWithOwner(10L, 1L);
        product.setCategory(null);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("crk");
        request.setDescription("sac for");
        request.setPrice(550.0);
        request.setStock(5);
        request.setStoreId(5L);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user(1L, "owner@test.com")));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> productService.update(10L, request, "owner@test.com"));

        assertEquals("Category id is required (categoryId or category.id)", exception.getMessage());
    }

    @Test
    void updateReturns400WhenCategoryIdInvalid() {
        Product product = productWithOwner(10L, 1L);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("crk");
        request.setDescription("sac for");
        request.setPrice(550.0);
        request.setStock(5);
        request.setStoreId(5L);
        request.setCategoryId(999L);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user(1L, "owner@test.com")));
        when(productRepository.findWithStoreAndCategoryById(10L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> productService.update(10L, request, "owner@test.com"));

        assertEquals("Invalid category id=999", exception.getMessage());
    }

    private Product productWithOwner(Long productId, Long ownerId) {
        Product product = new Product();
        product.setId(productId);
        product.setName("Laptop");
        product.setDescription("Product");
        product.setPrice(100.0);
        product.setStock(10);

        Store store = new Store();
        store.setId(5L);
        store.setName("My Store");
        store.setOwner(user(ownerId, "owner@test.com"));
        product.setStore(store);

        return product;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

}
