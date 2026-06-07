package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Long storeId;
}
