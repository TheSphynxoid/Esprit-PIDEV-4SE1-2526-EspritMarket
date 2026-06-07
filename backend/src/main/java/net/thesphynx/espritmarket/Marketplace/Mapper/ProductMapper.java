package net.thesphynx.espritmarket.Marketplace.Mapper;

import net.thesphynx.espritmarket.Marketplace.Dto.ProductRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        if (request == null) return null;
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setDimensionsLabel(request.getDimensionsLabel());
        product.setWeight(request.getWeight());
        Long storeId = request.resolveStoreId();
        if (storeId != null) {
            Store store = new Store();
            store.setId(storeId);
            product.setStore(store);
        }
        Long categoryId = request.resolveCategoryId();
        if (categoryId != null) {
            Category category = new Category();
            category.setId(categoryId);
            product.setCategory(category);
        }
        return product;
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setOriginalPrice(product.getOriginalPrice());
        response.setDiscountPercent(product.getDiscountPercent());
        response.setPromoStartAt(product.getPromoStartAt());
        response.setPromoEndAt(product.getPromoEndAt());
        response.setStock(product.getStock());
        String imageUrl = product.getImageUrl();
        response.setImageUrl(imageUrl);
        System.out.println("🖼️  PRODUCT MAPPER - ProductId=" + product.getId() + " imageUrl=" + imageUrl);
        response.setDimensionsLabel(product.getDimensionsLabel());
        response.setWeight(product.getWeight());
        if (product.getStore() != null) {
            response.setStoreId(product.getStore().getId());
            response.setStoreName(product.getStore().getName());
        }
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getName());
        }
        return response;
    }
}
