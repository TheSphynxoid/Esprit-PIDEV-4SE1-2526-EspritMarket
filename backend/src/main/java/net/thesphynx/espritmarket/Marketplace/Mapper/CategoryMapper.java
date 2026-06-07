package net.thesphynx.espritmarket.Marketplace.Mapper;

import net.thesphynx.espritmarket.Marketplace.Dto.CategoryRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.CategoryResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        if (request == null) return null;
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getStoreId() != null) {
            Store store = new Store();
            store.setId(request.getStoreId());
            category.setStore(store);
        }
        return category;
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) return null;
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        if (category.getStore() != null) {
            response.setStoreId(category.getStore().getId());
        }
        return response;
    }
}
