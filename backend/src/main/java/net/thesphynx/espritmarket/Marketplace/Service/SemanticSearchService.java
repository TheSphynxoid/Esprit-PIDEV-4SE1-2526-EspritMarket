package net.thesphynx.espritmarket.Marketplace.Service;

import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Dto.SemanticSearchRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.SemanticSearchResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import net.thesphynx.espritmarket.Marketplace.Mapper.ProductMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final IProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai.fastapi.url:http://localhost:8000}")
    private String fastApiBaseUrl;

    public SemanticSearchResponse search(String query) {
        String semanticUrl = fastApiBaseUrl + "/semantic-search";
        
        // 1. Appeler l'IA pour analyser l'intention
        Map<String, Object> aiResponse = callPythonSemanticSearch(query, semanticUrl);
        
        String category = (String) aiResponse.getOrDefault("category", "product");
        String occasion = (String) aiResponse.getOrDefault("occasion", "none");
        String season = (String) aiResponse.getOrDefault("season", "none");
        List<String> keywords = (List<String>) aiResponse.getOrDefault("keywords", new ArrayList<>());

        // 2. Recherche floue (Fuzzy Search) sur la requête originale
        List<Product> products = new ArrayList<>();
        try {
            products.addAll(productRepository.semanticSearch(query));
        } catch (Exception e) {
            System.err.println("⚠️ La recherche floue (pg_trgm) a échoué, passage en mode mots-clés purs. Erreur: " + e.getMessage());
        }

        // 3. Si pas assez de résultats, on cherche par mots-clés extraits
        if (products.size() < 5 && !keywords.isEmpty()) {
            Set<Long> existingIds = products.stream().map(Product::getId).collect(Collectors.toSet());
            for (String kw : keywords) {
                if (kw.length() < 2) continue; // Ignore very short keywords
                List<Product> extra = productRepository.searchByKeyword(kw);
                for (Product p : extra) {
                    if (!existingIds.contains(p.getId())) {
                        products.add(p);
                        existingIds.add(p.getId());
                    }
                }
                if (products.size() > 15) break;
            }
        }

        String correctedQuery = (String) aiResponse.getOrDefault("corrected_query", query);

        return SemanticSearchResponse.builder()
                .originalQuery(query)
                .correctedQuery(correctedQuery)
                .category(category)
                .occasion(occasion)
                .season(season)
                .extractedKeywords(keywords)
                .products(products.stream().map(productMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    private Map<String, Object> callPythonSemanticSearch(String query, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> body = Map.of("query", query);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            
            return restTemplate.postForObject(url, entity, Map.class);
        } catch (Exception e) {
            System.err.println("❌ Erreur IA Sémantique: " + e.getMessage());
            return Map.of("keywords", Arrays.asList(query.split(" ")));
        }
    }
}
