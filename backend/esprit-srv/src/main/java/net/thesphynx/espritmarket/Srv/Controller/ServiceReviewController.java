package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewResponse;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Service.MlPredictionService;
import net.thesphynx.espritmarket.Srv.Service.ServiceReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/srv/service-reviews")
@Tag(name = "Srv - Service Reviews")
public class ServiceReviewController {
    private final ServiceReviewService serviceReviewService;
    private final UserRepository userRepository;
    private final MlPredictionService mlPredictionService;

    public ServiceReviewController(ServiceReviewService serviceReviewService, UserRepository userRepository, MlPredictionService mlPredictionService) {
        this.serviceReviewService = serviceReviewService;
        this.userRepository = userRepository;
        this.mlPredictionService = mlPredictionService;
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "List reviews for a service (public)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reviews retrieved")})
    public PageResponse<ServiceReviewResponse> getByServiceId(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceReviewService.getByServiceId(serviceId, page, size);
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "List reviews for a provider filtered by booking status")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Reviews retrieved")})
    public PageResponse<ServiceReviewResponse> getByProviderIdAndBookingStatus(
            @PathVariable Long providerId,
            @RequestParam BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceReviewService.getByProviderIdAndBookingStatus(providerId, status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review found"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ServiceReviewResponse> getById(@PathVariable Long id) {
        return serviceReviewService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create review (completed booking required)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Review created")})
    public ServiceReviewResponse create(@Valid @RequestBody ServiceReviewRequest request, Authentication auth) {
        Long userId = extractUserId(auth);
        return serviceReviewService.create(request, userId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review updated"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ServiceReviewResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceReviewRequest request) {
        if (serviceReviewService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(serviceReviewService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (serviceReviewService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        serviceReviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/analyze-sentiment")
    @Operation(summary = "Analyze sentiment of review texts using trained ML model")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Sentiment analysis results")})
    public List<Map<String, Object>> analyzeSentiment(@RequestBody Map<String, List<String>> body) {
        List<String> texts = body.getOrDefault("texts", List.of());
        return mlPredictionService.analyzeSentiment(texts);
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
