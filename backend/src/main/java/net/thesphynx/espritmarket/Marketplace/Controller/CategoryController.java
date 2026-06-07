package net.thesphynx.espritmarket.Marketplace.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Marketplace.Dto.CategoryRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.CategoryResponse;
import net.thesphynx.espritmarket.Marketplace.Service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des catégories de produits dans la place de marché.
 */
@RestController
@RequestMapping("/api/marketplace/categories")
@Tag(name = "Marketplace - Categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Récupère toutes les catégories disponibles.
     *
     * @return Liste de CategoryResponse contenant les informations des catégories.
     */
    @GetMapping
    @Operation(summary = "List categories")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Categories retrieved")})
    public List<CategoryResponse> getAll() {
        return categoryService.getAll();
    }

    /**
     * Récupère une catégorie spécifique par son identifiant.
     *
     * @param id L'identifiant de la catégorie à récupérer.
     * @return La catégorie correspondante ou une réponse 404 si non trouvée.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return categoryService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crée une nouvelle catégorie de produit.
     *
     * @param request Objet DTO contenant les informations de la nouvelle catégorie.
     * @return La catégorie créée.
     */
    @PostMapping
    @Operation(summary = "Create category")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category created"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    /**
     * Met à jour une catégorie de produit existante.
     *
     * @param id L'identifiant de la catégorie à mettre à jour.
     * @param request Objet DTO contenant les nouvelles informations.
     * @return La catégorie mise à jour ou une réponse 404 si non trouvée.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryRequest request) {
        if (categoryService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    /**
     * Supprime une catégorie de produit par son identifiant.
     *
     * @param id L'identifiant de la catégorie à supprimer.
     * @return Une réponse vide (204 No Content) ou 404 si non trouvée.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deleted"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (categoryService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
