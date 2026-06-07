package net.thesphynx.espritmarket.Marketplace.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.thesphynx.espritmarket.Common.Exception.UnauthorizedException;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreStatsResponse;
import net.thesphynx.espritmarket.Marketplace.Service.StoreStatsService;

@RestController
@RequestMapping("/api/marketplace/stores")
public class StoreStatsController {

    private final StoreStatsService storeStatsService;

    public StoreStatsController(StoreStatsService storeStatsService) {
        this.storeStatsService = storeStatsService;
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<StoreStatsResponse> getStoreStats(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }

        StoreStatsResponse stats = storeStatsService.getStoreStats(id, authentication.getName());
        return ResponseEntity.ok(stats);
    }
}