package net.thesphynx.espritmarket.Marketplace.Dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatsResponse {
    private Double totalRevenue;
    private Long totalOrders;
    private BestSeller bestSeller;
    private List<MonthlyRevenue> monthlyRevenue = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestSeller {
        private Long productId;
        private String productName;
        private Long totalQuantity;
        private Double revenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private Double revenue;
    }
}