package net.thesphynx.espritmarket.Marketplace.Controller;

import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.Marketplace.Dto.SemanticSearchRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.SemanticSearchResponse;
import net.thesphynx.espritmarket.Marketplace.Service.SemanticSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketplace/semantic-search")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @PostMapping
    public ResponseEntity<SemanticSearchResponse> search(@RequestBody SemanticSearchRequest request) {
        return ResponseEntity.ok(semanticSearchService.search(request.getQuery()));
    }
    
    @GetMapping
    public ResponseEntity<SemanticSearchResponse> searchGet(@RequestParam String q) {
        return ResponseEntity.ok(semanticSearchService.search(q));
    }
}
