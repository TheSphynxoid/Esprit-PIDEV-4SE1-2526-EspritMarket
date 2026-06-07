package net.thesphynx.espritmarket.Srv.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.NotificationResponse;
import net.thesphynx.espritmarket.Srv.Dto.NotificationSummaryResponse;
import net.thesphynx.espritmarket.Srv.Service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/srv/notifications")
@Tag(name = "Srv - Notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "List notifications for current user")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Notifications retrieved")})
    public PageResponse<NotificationResponse> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return notificationService.getNotifications(userId, page, size);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get notification summary")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Summary retrieved")})
    public NotificationSummaryResponse getSummary(Authentication auth) {
        Long userId = extractUserId(auth);
        return notificationService.getSummary(userId);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Notification marked as read")})
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "All notifications marked as read")})
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        Long userId = extractUserId(auth);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
