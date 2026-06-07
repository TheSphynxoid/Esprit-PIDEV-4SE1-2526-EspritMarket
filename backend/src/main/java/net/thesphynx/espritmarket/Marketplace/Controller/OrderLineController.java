package net.thesphynx.espritmarket.Marketplace.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineResponse;
import net.thesphynx.espritmarket.Marketplace.Service.OrderLineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des lignes de commande.
 * Chaque ligne de commande lie un produit à une commande spécifique avec une quantité donnée.
 */
@RestController
@RequestMapping("/api/marketplace/order-lines")
@Tag(name = "Marketplace - Order Lines")
public class OrderLineController {
    private final OrderLineService orderLineService;

    public OrderLineController(OrderLineService orderLineService) {
        this.orderLineService = orderLineService;
    }

    /**
     * Récupère toutes les lignes de commande existantes.
     *
     * @return Liste de OrderLineResponse contenant les détails des lignes de commande.
     */
    @GetMapping
    @Operation(summary = "List order lines")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Order lines retrieved")})
    public List<OrderLineResponse> getAll() {
        return orderLineService.getAll();
    }

    /**
     * Récupère une ligne de commande spécifique par son identifiant.
     *
     * @param id L'identifiant de la ligne de commande à récupérer.
     * @return La ligne de commande correspondante ou une réponse 404 si non trouvée.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order line by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order line found"),
        @ApiResponse(responseCode = "404", description = "Order line not found")
    })
    public ResponseEntity<OrderLineResponse> getById(@PathVariable Long id) {
        return orderLineService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crée une nouvelle ligne de commande.
     *
     * @param request Objet DTO contenant les informations de la ligne de commande.
     * @return La ligne de commande créée.
     */
    @PostMapping
    @Operation(summary = "Create order line")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order line created"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public OrderLineResponse create(@Valid @RequestBody OrderLineRequest request) {
        return orderLineService.create(request);
    }

    /**
     * Met à jour une ligne de commande existante.
     *
     * @param id L'identifiant de la ligne de commande à mettre à jour.
     * @param request Objet DTO contenant les nouvelles informations.
     * @return La ligne de commande mise à jour ou une réponse 404 si non trouvée.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update order line")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order line updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Order line not found")
    })
    public ResponseEntity<OrderLineResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody OrderLineRequest request) {
        if (orderLineService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderLineService.update(id, request));
    }

    /**
     * Supprime une ligne de commande par son identifiant.
     *
     * @param id L'identifiant de la ligne de commande à supprimer.
     * @return Une réponse vide (204 No Content) ou 404 si non trouvée.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order line")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Order line deleted"),
        @ApiResponse(responseCode = "404", description = "Order line not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (orderLineService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        orderLineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
