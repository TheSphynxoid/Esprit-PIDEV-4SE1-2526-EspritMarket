package net.thesphynx.espritmarket.Marketplace.Controller;

import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Common.Exception.UnauthorizedException;
import net.thesphynx.espritmarket.Marketplace.Dto.OwnerProductCreateRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OwnerProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductPromotionRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour la gestion des produits par leurs propriétaires (vendeurs).
 * Permet aux vendeurs de créer, mettre à jour, supprimer leurs produits et gérer les promotions.
 */
@RestController
@RequestMapping("/api/products")
public class OwnerProductController {
    private static final Logger logger = LoggerFactory.getLogger(OwnerProductController.class);

    private final ProductService productService;

    public OwnerProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Crée un nouveau produit pour la boutique de l'utilisateur authentifié.
     *
     * @param request Objet DTO contenant les informations de création du produit.
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return Le produit créé avec un statut 201 Created.
     */
    @PostMapping("/my-store")
    public ResponseEntity<ProductResponse> createForMyStore(@Valid @RequestBody OwnerProductCreateRequest request,
            Authentication authentication) {
        ProductResponse response = productService.createInMyStore(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Met à jour les informations d'un produit spécifique appartenant à l'utilisateur.
     * Utilise un DTO spécifique pour les mises à jour par le propriétaire.
     *
     * @param productId L'identifiant du produit à mettre à jour.
     * @param request Objet DTO contenant les nouvelles informations du produit.
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return Le produit mis à jour.
     */
    @PutMapping("/{productId}/owner-update")
    public ResponseEntity<ProductResponse> ownerUpdate(@PathVariable Long productId,
            @Valid @RequestBody OwnerProductUpdateRequest request,
            Authentication authentication) {
        String principal = requirePrincipal(authentication);
        logger.info("product.owner_update.endpoint.hit path=/api/products/{}/owner-update user={}",
                productId,
                principal);
        return ResponseEntity.ok(productService.ownerUpdate(productId, request, principal));
    }

    /**
     * Met à jour un produit existant (version générique).
     *
     * @param productId L'identifiant du produit à mettre à jour.
     * @param request Objet DTO contenant les nouvelles informations.
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return Le produit mis à jour.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            Authentication authentication) {
        String principal = requirePrincipal(authentication);
        logger.info("product.update.endpoint.hit path=/api/products/{} user={} storeId={} categoryId={}",
                productId,
            principal,
                request.resolveStoreId(),
                request.resolveCategoryId());
        return ResponseEntity.ok(productService.update(productId, request, principal));
    }

    /**
     * Supprime un produit appartenant à l'utilisateur authentifié.
     *
     * @param productId L'identifiant du produit à supprimer.
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return Une réponse vide (204 No Content).
     */
    @DeleteMapping("/{productId}/owner-delete")
    public ResponseEntity<Void> ownerDelete(@PathVariable Long productId, Authentication authentication) {
        productService.ownerDelete(productId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Ajoute ou met à jour une promotion pour un produit spécifique.
     *
     * @param productId L'identifiant du produit concerné.
     * @param request Objet DTO contenant les détails de la promotion.
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return Le produit avec les informations de promotion mises à jour.
     */
    @PatchMapping("/{productId}/promotion")
    public ResponseEntity<ProductResponse> upsertPromotion(@PathVariable Long productId,
            @Valid @RequestBody ProductPromotionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(productService.upsertPromotion(productId, request, authentication.getName()));
    }

    /**
     * Supprime la promotion d'un produit spécifique.
     *
     * @param productId L'identifiant du produit concerné.
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return Le produit mis à jour sans la promotion.
     */
    @DeleteMapping("/{productId}/promotion")
    public ResponseEntity<ProductResponse> removePromotion(@PathVariable Long productId, Authentication authentication) {
        return ResponseEntity.ok(productService.removePromotion(productId, authentication.getName()));
    }

    private String requirePrincipal(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
        return authentication.getName();
    }
}
