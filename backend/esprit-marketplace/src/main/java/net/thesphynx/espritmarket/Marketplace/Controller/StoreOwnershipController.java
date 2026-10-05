package net.thesphynx.espritmarket.Marketplace.Controller;

import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreResponse;
import net.thesphynx.espritmarket.Marketplace.Service.StoreService;
import net.thesphynx.espritmarket.Common.Exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST pour la gestion de la propriété des boutiques.
 * Permet aux utilisateurs authentifiés de gérer leur propre boutique et de consulter les informations de propriété.
 */
@RestController
@RequestMapping("/api/stores")
public class StoreOwnershipController {

    private final StoreService storeService;

    public StoreOwnershipController(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * Crée une boutique pour l'utilisateur actuellement authentifié.
     *
     * @param request Objet DTO contenant les détails de la boutique.
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return La boutique créée avec un statut 201 Created.
     */
    @PostMapping("/my")
    public ResponseEntity<StoreResponse> createMyStore(@Valid @RequestBody StoreRequest request,
            Authentication authentication) {
        requireAuth(authentication);
        StoreResponse response = storeService.createMyStore(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/my/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreResponse> createMyStoreWithLogo(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("address") String address,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "categories", required = false) java.util.List<String> categories,
            @RequestPart("logo") org.springframework.web.multipart.MultipartFile logo,
            Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeService.createWithLogo(name, description, address, phone, categories, logo, authentication.getName()));
    }


    /**
     * Récupère la boutique appartenant à l'utilisateur actuellement authentifié.
     *
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return La boutique de l'utilisateur.
     */
    @GetMapping("/my")
    public ResponseEntity<StoreResponse> getMyStore(Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity.ok(storeService.getMyStore(authentication.getName()));
    }

    /**
     * Retourne toutes les boutiques de l'utilisateur actuellement authentifie.
     */
    @GetMapping("/my/list")
    public ResponseEntity<List<StoreResponse>> getMyStores(Authentication authentication) {
        requireAuth(authentication);
        return ResponseEntity.ok(storeService.getMyStores(authentication.getName()));
    }

    /**
     * Récupère la boutique de l'utilisateur actuel, ou en crée une par défaut si elle n'existe pas.
     *
     * @param authentication Contexte d'authentification de l'utilisateur.
     * @return La boutique existante ou nouvellement créée.
     */
    @GetMapping("/by-user")
    public ResponseEntity<?> getStoreByCurrentUser(Authentication authentication) {
        requireAuth(authentication);
        StoreResponse response = storeService.getOrCreateMyStore(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les informations d'une boutique spécifique par son identifiant.
     *
     * @param id L'identifiant de la boutique à récupérer.
     * @return Les détails de la boutique demandée.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStoreById(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.getById(id));
    }

    private void requireAuth(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
    }
}
