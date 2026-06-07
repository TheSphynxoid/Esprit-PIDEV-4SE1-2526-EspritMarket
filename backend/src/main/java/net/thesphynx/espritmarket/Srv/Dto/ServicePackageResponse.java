package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServicePackageResponse {
    private Long id;
    private String tier;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer deliveryDays;
    private Integer revisions;
    private List<String> features;
    private boolean recommended;
}
