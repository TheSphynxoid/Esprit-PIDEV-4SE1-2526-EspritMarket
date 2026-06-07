package net.thesphynx.espritmarket.Marketplace.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Marketplace.Dto.VisualSearchResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import net.thesphynx.espritmarket.Marketplace.Mapper.ProductMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;

@Service
public class VisualSearchService {

    private static final Pattern DATA_URL_PATTERN = Pattern.compile(
            "^data:(image/[a-zA-Z0-9.+\\-]+);base64,(.+)$", Pattern.DOTALL);

    private static final int MAX_RESULTS = 24;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "this", "that", "is", "are", "was", "were", "with",
            "and", "or", "in", "on", "of", "has", "have", "it", "its", "at",
            "for", "to", "by", "as", "be", "been", "which", "there", "their",
            "image", "photo", "picture", "shows", "showing", "features", "appears",
            "looks", "like", "very", "quite", "made", "from", "pair", "type",
            "style", "some", "all", "both", "each", "more", "also", "into", "than",
            "men", "women", "man", "woman", "boys", "girls", "unisex");

    private static final Set<String> GENERIC_TERMS = Set.of(
            "produit", "product", "article", "item", "mode", "fashion",
            "boutique", "shop", "store", "thing", "object", "look");

    private final IProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RestTemplate restTemplate;

    @Value("${app.ai.fastapi.url:http://127.0.0.1:8000/analyze}")
    private String fastApiUrl;

    public VisualSearchService(IProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Transactional(readOnly = true)
    public VisualSearchResponse searchByImage(String imageBase64) {
        String rawImage = String.valueOf(imageBase64).trim();
        if (rawImage.isBlank()) {
            throw new BadRequestException("imageBase64 is required");
        }

        // 1. Appel FastAPI pour obtenir l'analyse visuelle
        FastApiResponse fastapiResponse = callFastApiService(rawImage);

        if (fastapiResponse == null) {
            return unavailableResponse("FastAPI indisponible");
        }

        System.out.println("✅ FastAPI: type=" + fastapiResponse.productType()
                + ", colors=" + fastapiResponse.colors()
                + ", keywords=" + fastapiResponse.keywords());

        // 2. Construire les termes de recherche depuis l'analyse FastAPI
        List<String> searchTerms = buildSearchTerms(fastapiResponse);
        System.out.println("🔍 Termes de recherche: " + searchTerms);

        // 3. Chercher dans ta DB PostgreSQL avec ces termes
        Map<Long, RankedProduct> ranking = new LinkedHashMap<>();

        for (String term : searchTerms) {
            List<Product> matches = productRepository.searchByKeyword(term);
            for (Product product : matches) {
                double delta = scoreProduct(product, term, fastapiResponse);
                if (delta <= 0) continue;

                RankedProduct entry = ranking.computeIfAbsent(
                        product.getId(), ignored -> new RankedProduct(product, 0.0));
                entry.score += delta;
            }
        }

        // 4. Trier par score et limiter les résultats
        List<VisualSearchResponse.VisualSearchResultItem> results = ranking.values().stream()
                .sorted(Comparator.comparingDouble((RankedProduct r) -> r.score).reversed())
                .limit(MAX_RESULTS)
                .map(ranked -> new VisualSearchResponse.VisualSearchResultItem(
                        productMapper.toResponse(ranked.product),
                        Math.round(ranked.score * 100.0) / 100.0))
                .toList();

        System.out.println("✅ Produits trouvés dans la DB: " + results.size());

        // 5. Si aucun résultat → fallback sur tous les produits
        if (results.isEmpty()) {
            results = fallbackAllProducts(fastapiResponse, searchTerms);
            System.out.println("⚠️ Fallback: " + results.size() + " produits");
        }

        return new VisualSearchResponse(
                fastapiResponse.productType(),
                fastapiResponse.colors(),
                fastapiResponse.style(),
                fastapiResponse.keywords(),
                results
        );
    }

    // ── Appel FastAPI ─────────────────────────────────────────────────────────
    private FastApiResponse callFastApiService(String imageBase64) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("imageBase64", imageBase64);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<FastApiResponse> response = restTemplate.postForEntity(
                    fastApiUrl, entity, FastApiResponse.class);

            return response.getBody();
        } catch (Exception e) {
            System.err.println("❌ Erreur appel FastAPI: " + e.getMessage());
            return null;
        }
    }

    // ── Construire les termes depuis la réponse FastAPI ───────────────────────
    private List<String> buildSearchTerms(FastApiResponse analysis) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();

        // 1. Type de produit (le plus important)
        addTerm(terms, analysis.productType());

        // 2. Couleurs
        if (analysis.colors() != null) {
            analysis.colors().forEach(c -> addTerm(terms, c));
        }

        // 3. Mots-clés
        if (analysis.keywords() != null) {
            analysis.keywords().forEach(k -> addTerm(terms, k));
        }

        // 4. Types des produits Kaggle similaires (article_type)
        if (analysis.results() != null) {
            analysis.results().stream()
                    .limit(3) // Prendre seulement les 3 premiers
                    .forEach(r -> {
                        addTerm(terms, r.article_type());
                        // Extraire mots utiles du nom produit Kaggle
                        if (r.product_name() != null) {
                            for (String word : r.product_name().split("\\s+")) {
                                addTerm(terms, word);
                            }
                        }
                    });
        }

        // 5. Synonymes
        LinkedHashSet<String> expanded = new LinkedHashSet<>(terms);
        for (String term : terms) {
            expandSynonyms(expanded, term);
        }

        return new ArrayList<>(expanded);
    }

    // ── Score d'un produit DB pour un terme ───────────────────────────────────
    private double scoreProduct(Product product, String term, FastApiResponse analysis) {
        String name        = String.valueOf(product.getName()).toLowerCase(Locale.ROOT);
        String description = String.valueOf(product.getDescription()).toLowerCase(Locale.ROOT);
        String category    = product.getCategory() != null
                ? product.getCategory().getName().toLowerCase(Locale.ROOT) : "";

        double score = 0.0;

        // Bonus selon où le terme apparaît
        if (name.contains(term))        score += 3.0;
        if (description.contains(term)) score += 1.5;
        if (category.contains(term))    score += 2.0;

        // Bonus si le type principal matche
        String type = String.valueOf(analysis.productType()).toLowerCase(Locale.ROOT);
        if (!type.isBlank() && (name.contains(type) || category.contains(type))) {
            score += 2.0;
        }

        // Bonus couleur
        if (analysis.colors() != null) {
            for (String color : analysis.colors()) {
                String c = color.toLowerCase(Locale.ROOT);
                if (name.contains(c) || description.contains(c)) {
                    score += 1.0;
                }
            }
        }

        return score;
    }

    // ── Fallback : chercher dans tous les produits ────────────────────────────
    private List<VisualSearchResponse.VisualSearchResultItem> fallbackAllProducts(
            FastApiResponse analysis, List<String> terms) {

        return productRepository.findAllByOrderByIdDesc().stream()
                .map(p -> {
                    double s = 0.0;
                    String haystack = (String.valueOf(p.getName()) + " "
                            + String.valueOf(p.getDescription()) + " "
                            + (p.getCategory() != null ? p.getCategory().getName() : ""))
                            .toLowerCase(Locale.ROOT);

                    for (String t : terms) {
                        if (haystack.contains(t)) s += 1.0;
                    }

                    String type = String.valueOf(analysis.productType()).toLowerCase(Locale.ROOT);
                    if (!type.isBlank() && haystack.contains(type)) s += 2.0;

                    return new RankedProduct(p, s);
                })
                .filter(r -> r.score > 0)
                .sorted(Comparator.comparingDouble((RankedProduct r) -> r.score).reversed())
                .limit(MAX_RESULTS)
                .map(r -> new VisualSearchResponse.VisualSearchResultItem(
                        productMapper.toResponse(r.product),
                        Math.round(r.score * 100.0) / 100.0))
                .toList();
    }

    // ── Synonymes ─────────────────────────────────────────────────────────────
    private void expandSynonyms(Set<String> terms, String source) {
        String term = String.valueOf(source).toLowerCase(Locale.ROOT).trim();
        switch (term) {
            case "jean", "jeans", "denim", "trouser", "pants", "pantalon" -> {
                addTerm(terms, "jean"); addTerm(terms, "jeans");
                addTerm(terms, "denim"); addTerm(terms, "pantalon");
                addTerm(terms, "trouser"); addTerm(terms, "jogger");
            }
            case "sweater", "pull", "pullover", "cardigan", "sweatshirt", "tricot", "maille" -> {
                addTerm(terms, "sweater"); addTerm(terms, "pull");
                addTerm(terms, "pullover"); addTerm(terms, "cardigan");
                addTerm(terms, "sweatshirt"); addTerm(terms, "tricot");
                addTerm(terms, "maille"); addTerm(terms, "laine");
            }
            case "t-shirt", "tee", "shirt", "top", "haut", "tshirt", "tshirts" -> {
                addTerm(terms, "t-shirt"); addTerm(terms, "tee");
                addTerm(terms, "shirt"); addTerm(terms, "top"); addTerm(terms, "haut");
            }
            case "hoodie" -> {
                addTerm(terms, "hoodie"); addTerm(terms, "sweatshirt"); addTerm(terms, "sweater");
            }
            case "sneaker", "shoe", "trainer", "chaussure", "footwear" -> {
                addTerm(terms, "sneaker"); addTerm(terms, "shoe");
                addTerm(terms, "trainer"); addTerm(terms, "chaussure");
            }
            case "bag", "backpack", "handbag", "sac", "purse" -> {
                addTerm(terms, "bag"); addTerm(terms, "backpack");
                addTerm(terms, "handbag"); addTerm(terms, "sac");
            }
            case "dress", "robe" -> {
                addTerm(terms, "dress"); addTerm(terms, "robe");
            }
            case "jacket", "coat", "blazer", "parka", "manteau", "veste" -> {
                addTerm(terms, "jacket"); addTerm(terms, "coat");
                addTerm(terms, "blazer"); addTerm(terms, "veste"); addTerm(terms, "manteau");
            }
            case "skirt", "jupe" -> {
                addTerm(terms, "skirt"); addTerm(terms, "jupe");
            }
            case "shorts", "short" -> {
                addTerm(terms, "short"); addTerm(terms, "shorts");
            }
        }
    }

    private void addTerm(Set<String> terms, String raw) {
        if (raw == null) return;
        String normalized = raw.toLowerCase(Locale.ROOT).trim();
        if (normalized.length() < 3) return;
        if (STOP_WORDS.contains(normalized)) return;
        if (GENERIC_TERMS.contains(normalized)) return;
        terms.add(normalized);
    }

    // ── Réponse indisponible ──────────────────────────────────────────────────
    private VisualSearchResponse unavailableResponse(String reason) {
        System.err.println("❌ VisualSearch indisponible: " + reason);
        return new VisualSearchResponse(
                "Analyse IA indisponible", List.of(), "ai-unavailable", List.of(), List.of());
    }

    // ─── Inner types ──────────────────────────────────────────────────────────

    private record FastApiResponse(
            String productType,
            List<String> colors,
            String style,
            List<String> keywords,
            List<FastApiResultItem> results) {
    }

    private record FastApiResultItem(
            int rank,
            double score,
            String product_id,
            String product_name,
            String category,
            String article_type,
            String base_colour,
            String image_url) {
    }

    private static final class RankedProduct {
        private final Product product;
        private double score;

        private RankedProduct(Product product, double score) {
            this.product = product;
            this.score = score;
        }
    }
}