package net.thesphynx.espritmarket.Marketplace.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Marketplace.Dto.VisualSearchRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.VisualSearchResponse;
import net.thesphynx.espritmarket.Marketplace.Service.VisualSearchService;

@CrossOrigin(
    origins  = "http://localhost:4200",
    methods  = { RequestMethod.POST, RequestMethod.OPTIONS },
    allowedHeaders   = "*",
    allowCredentials = "true"
)
@RestController
@RequestMapping("/api/marketplace/visual-search")
@Tag(name = "Marketplace - Visual Search")
public class VisualSearchController {

    private final VisualSearchService visualSearchService;

    public VisualSearchController(VisualSearchService visualSearchService) {
        this.visualSearchService = visualSearchService;
    }

    @PostMapping
    @Operation(summary = "Search similar products from an uploaded image")
    public ResponseEntity<VisualSearchResponse> visualSearch(
            @Valid @RequestBody VisualSearchRequest request) {

        // ✅ Log debug — vérifier que imageBase64 arrive bien
        String raw = request.getImageBase64();
        System.out.println("📸 VisualSearch — imageBase64 reçu, longueur : "
                + (raw != null ? raw.length() : "NULL"));
        System.out.println("📸 VisualSearch — début : "
                + (raw != null && raw.length() > 50 ? raw.substring(0, 50) : raw));

        VisualSearchResponse response = visualSearchService.searchByImage(raw);
        return ResponseEntity.ok(response);
    }
}