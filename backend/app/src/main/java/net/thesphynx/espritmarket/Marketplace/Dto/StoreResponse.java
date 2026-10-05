package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.Data;
import java.util.List;

@Data
public class StoreResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String phone;
    private Double rating;
    private Double balance;
    private String logoUrl;
    private List<String> categories;

    private Long ownerId;
}
