package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Srv.Dto.TimeSlotDto;
import net.thesphynx.espritmarket.Srv.Dto.SlotScoringMode;
import net.thesphynx.espritmarket.Srv.Dto.SlotSuggestionResponse;
import net.thesphynx.espritmarket.Srv.Dto.SlotAllocationAuditResponse;
import net.thesphynx.espritmarket.Srv.Service.AvailabilityService;
import net.thesphynx.espritmarket.Srv.Service.SlotAllocationAuditService;
import net.thesphynx.espritmarket.Srv.Service.SlotScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/srv/availability")
@Tag(name = "Srv - Availability")
public class AvailabilityController {
    private final AvailabilityService availabilityService;
    private final SlotScoringService slotScoringService;
    private final SlotAllocationAuditService slotAllocationAuditService;

    public AvailabilityController(AvailabilityService availabilityService,
                                  SlotScoringService slotScoringService,
                                  SlotAllocationAuditService slotAllocationAuditService) {
        this.availabilityService = availabilityService;
        this.slotScoringService = slotScoringService;
        this.slotAllocationAuditService = slotAllocationAuditService;
    }

    @GetMapping("/{serviceId}/slots")
    @Operation(summary = "Get available time slots for a service (public)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Slots retrieved")})
    public List<TimeSlotDto> getAvailableSlots(
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (endDate == null) {
            endDate = startDate;
        }
        return availabilityService.getAvailableSlots(serviceId, startDate, endDate);
    }

    @GetMapping("/{serviceId}/slots/suggestions")
    @Operation(summary = "Get scored slot suggestions (Phase 4 baseline)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Scored slot suggestions retrieved")})
    public SlotSuggestionResponse getScoredSlotSuggestions(
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "PROJECT_FIRST") SlotScoringMode mode,
            @RequestParam(defaultValue = "10") int limit) {
        if (endDate == null) {
            endDate = startDate;
        }
        SlotSuggestionResponse response = slotScoringService.suggestSlots(serviceId, startDate, endDate, projectId, mode, limit);
        slotAllocationAuditService.recordTopSuggestions(response);
        return response;
    }

    @GetMapping("/allocation-audit")
    @Operation(summary = "Get slot allocation audit entries with filters")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Allocation audit entries retrieved")})
    public List<SlotAllocationAuditResponse> getAllocationAudit(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String reasonCode) {
        if (projectId != null) {
            return slotAllocationAuditService.getByProjectId(projectId);
        }
        if (serviceId != null) {
            return slotAllocationAuditService.getByServiceId(serviceId);
        }
        return slotAllocationAuditService.getRecent(100);
    }

    @GetMapping("/allocation-audit/project/{projectId}")
    @Operation(summary = "Get latest slot allocation audit entries for a project")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Allocation audit entries retrieved")})
    public List<SlotAllocationAuditResponse> getAllocationAuditByProject(@PathVariable Long projectId) {
        return slotAllocationAuditService.getByProjectId(projectId);
    }

    @GetMapping("/allocation-audit/service/{serviceId}")
    @Operation(summary = "Get latest slot allocation audit entries for a service")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Allocation audit entries retrieved")})
    public List<SlotAllocationAuditResponse> getAllocationAuditByService(@PathVariable Long serviceId) {
        return slotAllocationAuditService.getByServiceId(serviceId);
    }

    @GetMapping("/{serviceId}/overbooked")
    @Operation(summary = "Check if service is overbooked")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Status returned")})
    public boolean isServiceOverbooked(
            @PathVariable Long serviceId,
            @RequestParam Long providerId) {
        return availabilityService.isServiceOverbooked(providerId, serviceId);
    }

    @GetMapping("/provider/{providerId}/overbooked")
    @Operation(summary = "Check if provider is overbooked")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Status returned")})
    public boolean isProviderOverbooked(@PathVariable Long providerId) {
        return availabilityService.isProviderOverbooked(providerId);
    }
}
