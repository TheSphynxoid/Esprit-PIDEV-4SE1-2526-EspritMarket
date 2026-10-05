package net.thesphynx.espritmarket.Marketplace.Dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisualSearchResponse {

    private String detectedType;
    private List<String> colors = new ArrayList<>();
    private String style;
    private List<String> keywords = new ArrayList<>();
    private List<VisualSearchResultItem> results = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualSearchResultItem {
        private ProductResponse product;
        private Double similarityScore;
    }
}
