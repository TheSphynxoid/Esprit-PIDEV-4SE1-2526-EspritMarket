package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponse {
    private Long productId;
    private String name;
    private Double price;
    private String storeName;
    private String categoryName;
}
