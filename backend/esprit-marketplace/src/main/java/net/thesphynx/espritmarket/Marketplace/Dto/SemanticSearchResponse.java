package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResponse {
    private String originalQuery;
    private String correctedQuery;
    private String category;
    private String occasion;
    private String season;
    private List<String> extractedKeywords;
    private List<ProductResponse> products;
}
