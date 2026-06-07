package net.thesphynx.espritmarket.Marketplace.Mapper;

import net.thesphynx.espritmarket.Marketplace.Dto.StoreRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StoreMapper {

    public Store toEntity(StoreRequest request) {
        if (request == null)
            return null;
        Store entity = new Store();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setAddress(request.getAddress());
        entity.setPhone(request.getPhone());
        entity.setLogoUrl(request.getLogoUrl());
        return entity;
    }

    public StoreResponse toResponse(Store entity) {
        if (entity == null)
            return null;
        StoreResponse response = new StoreResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setAddress(entity.getAddress());
        response.setPhone(entity.getPhone());
        response.setRating(entity.getRating());
        response.setBalance(entity.getBalance());
        response.setLogoUrl(entity.getLogoUrl());


        if (entity.getCategories() != null) {
            response.setCategories(entity.getCategories().stream()
                    .map(Category::getName)
                    .collect(Collectors.toList()));
        }

        if (entity.getOwner() != null) {
            response.setOwnerId(entity.getOwner().getId());
        }

        return response;
    }
}
