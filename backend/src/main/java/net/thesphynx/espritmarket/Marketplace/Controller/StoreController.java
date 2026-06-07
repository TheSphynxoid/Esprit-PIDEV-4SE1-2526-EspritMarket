package net.thesphynx.espritmarket.Marketplace.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Common.Exception.UnauthorizedException;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Service.StoreService;

/**
 * Contrôleur REST — gestion des boutiques (/api/marketplace/stores).
 */
@RestController
@RequestMapping("/api/marketplace/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    // ── Création ─────────────────────────────────────────────────────────

    /**
     * POST /api/marketplace/stores
     * Crée une boutique pour l'utilisateur connecté.
     */
    @PostMapping
    public ResponseEntity<StoreResponse> createStoreForAuthenticatedUser(
            @Valid @RequestBody StoreRequest request,
            Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(storeService.createMyStore(request, authentication.getName()));
    }

    /**
     * POST /api/marketplace/stores/create/{userId}
     * Crée une boutique pour un seller identifié par son ID.
     * L'utilisateur doit être authentifié. Maximum 3 boutiques par seller.
     */
    @PostMapping("/create/{userId}")
    public ResponseEntity<StoreResponse> createStoreForUser(
            @Valid @RequestBody StoreRequest request,
            @PathVariable Long userId,
            Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(storeService.createStore(request, userId));
    }

    /**
     * POST /api/marketplace/stores/upload
     * Crée une boutique avec upload de logo (multipart/form-data).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreResponse> createWithLogo(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("address") String address,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "categories", required = false) List<String> categories,
            @RequestPart("logo") MultipartFile logo,
            Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(storeService.createWithLogo(
                        name, description, address, phone, categories,
                        logo, authentication.getName()));
    }

    // ── Lecture ───────────────────────────────────────────────────────────

    /**
     * GET /api/marketplace/stores
     * Retourne toutes les boutiques (liste publique / admin).
     */
    @GetMapping
    public ResponseEntity<List<Store>> getAllStores() {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    /**
     * GET /api/marketplace/stores/my
     * Retourne la première boutique de l'utilisateur connecté.
     */
    @GetMapping("/my")
    public ResponseEntity<StoreResponse> getMyStore(Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity.ok(storeService.getMyStore(authentication.getName()));
    }

    /**
     * GET /api/marketplace/stores/my/all
     * Retourne toutes les boutiques de l'utilisateur connecté (max 3).
     */
    @GetMapping("/my/all")
    public ResponseEntity<List<StoreResponse>> getMyStores(Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity.ok(storeService.getMyStores(authentication.getName()));
    }

    /**
     * GET /api/marketplace/stores/{storeId}
     * Retourne une boutique par son ID.
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getById(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.getById(storeId));
    }

    /**
     * DELETE /api/marketplace/stores/{storeId}
     * Supprime une boutique appartenant à l'utilisateur authentifié.
     */
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long storeId, Authentication authentication) {
        requireAuth(authentication);
        storeService.deleteStore(authentication.getName(), storeId);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private void requireAuth(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
    }
}