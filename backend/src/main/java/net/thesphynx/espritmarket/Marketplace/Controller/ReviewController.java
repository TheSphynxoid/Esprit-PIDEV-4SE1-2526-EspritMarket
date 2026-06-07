package net.thesphynx.espritmarket.Marketplace.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Marketplace.Dto.ReviewRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ReviewResponse;
import net.thesphynx.espritmarket.Marketplace.Service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des avis (critiques) des produits.
 * Permet aux utilisateurs de laisser des notes et des commentaires sur les produits achetés.
 */
@RestController
@RequestMapping("/api/marketplace/reviews")
@Tag(name = "Marketplace - Reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Récupère tous les avis enregistrés dans le système.
     *
     * @return Liste de ReviewResponse contenant les détails des avis.
     */
    @GetMapping
    @Operation(summary = "List reviews")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reviews retrieved")})
    public List<ReviewResponse> getAll() {
        return reviewService.getAll();
    }

    /**
     * Récupère un avis spécifique par son identifiant.
     *
     * @param id L'identifiant de l'avis à récupérer.
     * @return L'avis correspondant ou une réponse 404 si non trouvé.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get review by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review found"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ReviewResponse> getById(@PathVariable Long id) {
        return reviewService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crée un nouvel avis pour un produit.
     *
     * @param request Objet DTO contenant les informations de l'avis (note, commentaire, produitId).
     * @return L'avis créé.
     */
    @PostMapping
    @Operation(summary = "Create review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review created"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ReviewResponse create(@Valid @RequestBody ReviewRequest request) {
        return reviewService.create(request);
    }

    /**
     * Met à jour un avis existant.
     *
     * @param id L'identifiant de l'avis à mettre à jour.
     * @param request Objet DTO contenant les nouvelles informations.
     * @return L'avis mis à jour ou une réponse 404 si non trouvé.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ReviewResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody ReviewRequest request) {
        if (reviewService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reviewService.update(id, request));
    }

    /**
     * Supprime un avis par son identifiant.
     *
     * @param id L'identifiant de l'avis à supprimer.
     * @return Une réponse vide (204 No Content) ou 404 si non trouvé.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review deleted"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (reviewService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
