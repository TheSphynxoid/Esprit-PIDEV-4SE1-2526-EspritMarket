package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Service.UserFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/srv/favorites")
@Tag(name = "Srv - Favorites")
public class UserFavoriteController {
    private final UserFavoriteService userFavoriteService;
    private final UserRepository userRepository;

    public UserFavoriteController(UserFavoriteService userFavoriteService, UserRepository userRepository) {
        this.userFavoriteService = userFavoriteService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Get user's favorite services")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Favorites retrieved")})
    public PageResponse<ServiceResponse> getFavorites(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userFavoriteService.getUserFavorites(extractUserId(auth), page, size);
    }

    @PostMapping("/{serviceId}")
    @Operation(summary = "Add service to favorites")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Added to favorites")})
    public ResponseEntity<Void> addFavorite(@PathVariable Long serviceId, Authentication auth) {
        userFavoriteService.addFavorite(extractUserId(auth), serviceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{serviceId}")
    @Operation(summary = "Remove service from favorites")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Removed from favorites")})
    public ResponseEntity<Void> removeFavorite(@PathVariable Long serviceId, Authentication auth) {
        userFavoriteService.removeFavorite(extractUserId(auth), serviceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{serviceId}/check")
    @Operation(summary = "Check if service is in user's favorites")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Check result")})
    public boolean isFavorite(@PathVariable Long serviceId, Authentication auth) {
        return userFavoriteService.isFavorite(extractUserId(auth), serviceId);
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
