package net.thesphynx.espritmarket.Srv.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Entity.EscrowHold;
import net.thesphynx.espritmarket.Srv.Entity.Wallet;
import net.thesphynx.espritmarket.Srv.Entity.WalletTransaction;
import net.thesphynx.espritmarket.Srv.Service.EscrowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/srv/wallet")
@Tag(name = "Srv - Wallet & Escrow")
public class WalletController {

    private final EscrowService escrowService;
    private final UserRepository userRepository;

    public WalletController(EscrowService escrowService, UserRepository userRepository) {
        this.escrowService = escrowService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Get user wallet balance")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Wallet retrieved")})
    public Map<String, Object> getWallet(Authentication auth) {
        Long userId = extractUserId(auth);
        Wallet wallet = escrowService.getOrCreateWallet(userId);
        return Map.of("userId", userId, "balance", wallet.getBalance());
    }

    @PostMapping("/top-up")
    @Operation(summary = "Top up wallet balance")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Wallet topped up")})
    public Map<String, Object> topUp(Authentication auth, @RequestBody Map<String, Double> body) {
        Long userId = extractUserId(auth);
        Double amount = body.get("amount");
        if (amount == null || amount <= 0) throw new IllegalArgumentException("Invalid amount");
        Wallet wallet = escrowService.topUp(userId, amount);
        return Map.of("userId", userId, "balance", wallet.getBalance(), "added", amount);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get wallet transaction history")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Transactions retrieved")})
    public List<WalletTransaction> getTransactions(Authentication auth) {
        Long userId = extractUserId(auth);
        return escrowService.getTransactions(userId);
    }

    @PostMapping("/escrow/{bookingId}")
    @Operation(summary = "Create escrow hold for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Escrow hold created")})
    public EscrowHold createHold(@PathVariable Long bookingId, Authentication auth) {
        Long userId = extractUserId(auth);
        return escrowService.createHold(bookingId, userId);
    }

    @GetMapping("/escrow/{bookingId}")
    @Operation(summary = "Get escrow hold for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Escrow hold retrieved")})
    public ResponseEntity<EscrowHold> getHold(@PathVariable Long bookingId) {
        return escrowService.getHold(bookingId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/escrow/{bookingId}/release")
    @Operation(summary = "Release escrow to provider")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Escrow released")})
    public EscrowHold release(@PathVariable Long bookingId) {
        return escrowService.release(bookingId);
    }

    @PostMapping("/escrow/{bookingId}/refund")
    @Operation(summary = "Refund escrow to client")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Escrow refunded")})
    public EscrowHold refund(@PathVariable Long bookingId) {
        return escrowService.refund(bookingId);
    }

    @PostMapping("/escrow/auto-release")
    @Operation(summary = "Process auto-releases for overdue holds")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Auto-releases processed")})
    public Map<String, Object> autoRelease() {
        int count = escrowService.processAutoReleases();
        return Map.of("released", count);
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
