package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.RescheduleDecisionDto;
import net.thesphynx.espritmarket.Srv.Dto.RescheduleRequestDto;
import net.thesphynx.espritmarket.Srv.Dto.RescheduleResponse;
import net.thesphynx.espritmarket.Srv.Service.RescheduleRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/srv/bookings")
@Tag(name = "Srv - Reschedule")
public class RescheduleRequestController {
    private final RescheduleRequestService rescheduleService;
    private final UserRepository userRepository;

    public RescheduleRequestController(RescheduleRequestService rescheduleService,
                                        UserRepository userRepository) {
        this.rescheduleService = rescheduleService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{bookingId}/reschedule")
    @Operation(summary = "Request a reschedule for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reschedule request created")})
    public RescheduleResponse createRequest(
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleRequestDto dto,
            Authentication auth) {
        return rescheduleService.createRequest(bookingId, dto, extractUserId(auth));
    }

    @GetMapping("/{bookingId}/reschedule")
    @Operation(summary = "Get active reschedule request for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Active request found or null")})
    public ResponseEntity<RescheduleResponse> getActiveRequest(@PathVariable Long bookingId) {
        RescheduleResponse response = rescheduleService.getActiveRequest(bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}/reschedule/history")
    @Operation(summary = "Get reschedule history for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "History retrieved")})
    public List<RescheduleResponse> getHistory(@PathVariable Long bookingId) {
        return rescheduleService.getHistory(bookingId);
    }

    @PatchMapping("/reschedule/{requestId}/accept")
    @Operation(summary = "Accept a reschedule request")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reschedule accepted")})
    public RescheduleResponse acceptRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) RescheduleDecisionDto dto,
            Authentication auth) {
        return rescheduleService.acceptRequest(requestId, dto, extractUserId(auth));
    }

    @PatchMapping("/reschedule/{requestId}/reject")
    @Operation(summary = "Reject a reschedule request")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reschedule rejected")})
    public RescheduleResponse rejectRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) RescheduleDecisionDto dto,
            Authentication auth) {
        return rescheduleService.rejectRequest(requestId, dto, extractUserId(auth));
    }

    @PatchMapping("/reschedule/{requestId}/cancel")
    @Operation(summary = "Cancel your own reschedule request")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reschedule cancelled")})
    public RescheduleResponse cancelRequest(@PathVariable Long requestId, Authentication auth) {
        return rescheduleService.cancelRequest(requestId, extractUserId(auth));
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
