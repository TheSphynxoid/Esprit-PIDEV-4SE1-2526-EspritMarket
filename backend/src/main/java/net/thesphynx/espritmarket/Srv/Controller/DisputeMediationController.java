package net.thesphynx.espritmarket.Srv.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.DisputeMediationResponse;
import net.thesphynx.espritmarket.Srv.Dto.DisputeVoteRequest;
import net.thesphynx.espritmarket.Srv.Service.DisputeMediationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/srv/disputes")
@Tag(name = "Srv - Dispute Mediation")
public class DisputeMediationController {

    private final DisputeMediationService mediationService;
    private final UserRepository userRepository;

    public DisputeMediationController(DisputeMediationService mediationService, UserRepository userRepository) {
        this.mediationService = mediationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{bookingId}/mediate")
    @Operation(summary = "Start automated dispute mediation for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Mediation created or retrieved")})
    public DisputeMediationResponse createMediation(@PathVariable Long bookingId) {
        return mediationService.createMediation(bookingId);
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get dispute mediation status for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Mediation retrieved")})
    public ResponseEntity<DisputeMediationResponse> getMediation(@PathVariable Long bookingId) {
        return mediationService.getMediationOptional(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{bookingId}/vote")
    @Operation(summary = "Vote on a dispute mediation suggestion")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Vote recorded")})
    public DisputeMediationResponse vote(@PathVariable Long bookingId, @RequestBody DisputeVoteRequest request, Authentication auth) {
        Long userId = extractUserId(auth);
        return mediationService.vote(bookingId, userId, request.getVote());
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
