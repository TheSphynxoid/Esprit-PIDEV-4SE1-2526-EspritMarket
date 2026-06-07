package net.thesphynx.espritmarket.Srv.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.TimeLogRequest;
import net.thesphynx.espritmarket.Srv.Dto.TimeTrackingResponse;
import net.thesphynx.espritmarket.Srv.Service.TimeTrackingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/srv/time-tracking")
@Tag(name = "Srv - Time Tracking")
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;
    private final UserRepository userRepository;

    public TimeTrackingController(TimeTrackingService timeTrackingService, UserRepository userRepository) {
        this.timeTrackingService = timeTrackingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get time tracking data for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Time tracking data retrieved")})
    public TimeTrackingResponse getTracking(@PathVariable Long bookingId) {
        return timeTrackingService.getTimeTracking(bookingId);
    }

    @PostMapping("/{bookingId}/start")
    @Operation(summary = "Start time tracking for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Timer started")})
    public TimeTrackingResponse startTimer(@PathVariable Long bookingId, @RequestBody(required = false) TimeLogRequest request, Authentication auth) {
        Long userId = extractUserId(auth);
        return timeTrackingService.startTimer(bookingId, userId, request != null ? request.getDescription() : null);
    }

    @PostMapping("/{bookingId}/stop")
    @Operation(summary = "Stop active timer for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Timer stopped")})
    public TimeTrackingResponse stopTimer(@PathVariable Long bookingId, Authentication auth) {
        Long userId = extractUserId(auth);
        return timeTrackingService.stopTimer(bookingId, userId);
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
