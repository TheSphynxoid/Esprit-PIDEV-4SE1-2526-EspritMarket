package net.thesphynx.espritmarket.Delivery.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Delivery.Dto.AdminCourierInfoResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierInterviewDateRequest;
import net.thesphynx.espritmarket.Delivery.Dto.CourierInterviewDateResponse;
import net.thesphynx.espritmarket.Delivery.Dto.CourierStatisticsResponse;
import net.thesphynx.espritmarket.Delivery.Dto.TopCourierResponse;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;
import net.thesphynx.espritmarket.Delivery.Service.CourierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping({"/api/admin/livreurs", "/api/delivery/couriers"})
@Tag(name = "Delivery - Admin Couriers")
@Validated
public class CourierAdminController {

    private final CourierService courierService;

    public CourierAdminController(CourierService courierService) {
        this.courierService = courierService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "List couriers for admin delivery table")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier data retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<AdminCourierInfoResponse> getCouriersForAdmin() {
        return courierService.getCouriersForAdminTable();
    }

    @GetMapping("/{courierId}/statistics")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "Get courier statistics (JPQL join aggregation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier statistics retrieved"),
            @ApiResponse(responseCode = "404", description = "Courier not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CourierStatisticsResponse getCourierStatistics(@PathVariable Long courierId) {
        return courierService.getCourierStatistics(courierId);
    }

    @GetMapping("/top-couriers")
    @PreAuthorize("hasAnyRole('ADMIN_DELIVERY','COURIER')")
    @Operation(summary = "Get top couriers ranked by delivered deliveries (JPQL keywords: JOIN/GROUP BY/HAVING/ORDER BY)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top couriers retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<TopCourierResponse> getTopCouriersByDeliveredDeliveries(
            @RequestParam(defaultValue = "5") int limit) {
        return courierService.getTopCouriersByDeliveredDeliveries(limit);
    }

    @PutMapping("/{courierId}/status")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "Update courier status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Courier not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public AdminCourierInfoResponse updateCourierStatus(
            @PathVariable Long courierId,
            @RequestParam CourierStatus status) {
        return courierService.updateCourierStatus(courierId, status);
    }

    @PostMapping("/{courierId}/interview-date")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "Create interview date (day and hour) for a courier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview date created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Courier not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CourierInterviewDateResponse createInterviewDate(
            @PathVariable Long courierId,
            @Valid @RequestBody CourierInterviewDateRequest request) {
        return courierService.createInterviewDate(courierId, request);
    }

    @GetMapping("/{courierId}/interview-date")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "Get interview date (day and hour) for a courier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview date returned"),
            @ApiResponse(responseCode = "404", description = "Courier not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CourierInterviewDateResponse getInterviewDate(@PathVariable Long courierId) {
        return courierService.getInterviewDate(courierId);
    }

    @PutMapping("/{courierId}/interview-date")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "Update interview date (day and hour) for a courier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview date updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Courier not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CourierInterviewDateResponse updateInterviewDate(
            @PathVariable Long courierId,
            @Valid @RequestBody CourierInterviewDateRequest request) {
        return courierService.updateInterviewDate(courierId, request);
    }

    @DeleteMapping("/{courierId}/interview-date")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "Delete interview date for a courier")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Interview date deleted"),
            @ApiResponse(responseCode = "404", description = "Courier not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deleteInterviewDate(@PathVariable Long courierId) {
        courierService.deleteInterviewDate(courierId);
        return ResponseEntity.noContent().build();
    }
}