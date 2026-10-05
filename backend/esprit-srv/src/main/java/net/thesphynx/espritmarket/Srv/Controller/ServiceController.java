package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Srv.Dto.CompatibilityResponse;
import net.thesphynx.espritmarket.Srv.Dto.ProviderStandingResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceProjectParticipationRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProviderProjectParticipationRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceUpsertRequest;
import net.thesphynx.espritmarket.Srv.Dto.BookingPredictionResponse;
import net.thesphynx.espritmarket.Srv.Dto.CompatibilityResponse;
import net.thesphynx.espritmarket.Srv.Dto.ProviderProjectParticipationRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProviderStandingResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceComparisonResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceUpsertRequest;
import net.thesphynx.espritmarket.Srv.Dto.SurgePricingResponse;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Entity.ServiceCategory;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Service.ServiceService;
import net.thesphynx.espritmarket.Srv.Service.MlPredictionService;
import net.thesphynx.espritmarket.Srv.Service.SurgePricingService;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/srv/services")
@Tag(name = "Srv - Services")
public class ServiceController {
    private final ServiceService serviceService;
    private final MlPredictionService mlPredictionService;
    private final SurgePricingService surgePricingService;

    public ServiceController(ServiceService serviceService, MlPredictionService mlPredictionService, SurgePricingService surgePricingService) {
        this.serviceService = serviceService;
        this.mlPredictionService = mlPredictionService;
        this.surgePricingService = surgePricingService;
    }

    @GetMapping
    @Operation(summary = "Browse services (public)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Services retrieved")})
    public PageResponse<ServiceResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceService.getAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service by id (public)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service found"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<ServiceResponse> getById(@PathVariable Long id) {
        return serviceService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "List services by provider (public)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Services retrieved")})
    public PageResponse<ServiceResponse> getByProviderId(
            @PathVariable Long providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceService.getByProviderId(providerId, page, size);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Filter by category (public)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Services retrieved")})
    public PageResponse<ServiceResponse> getByCategory(
            @PathVariable ServiceCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceService.getByCategory(category, page, size);
    }

    @GetMapping("/search")
    @Operation(summary = "Search services by keyword (public)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Services retrieved")})
    public PageResponse<ServiceResponse> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceService.search(keyword, page, size);
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter services (public)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Services retrieved")})
    public PageResponse<ServiceResponse> filter(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceService.getByFilters(category, minPrice, maxPrice, location, page, size);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Get related services (same category)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Related services retrieved")})
    public PageResponse<ServiceResponse> getRelated(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return serviceService.getRelated(id, page, size);
    }

    @PostMapping
    @Operation(summary = "Create service")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service created")})
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ServiceResponse create(@Valid @RequestBody ServiceUpsertRequest request) {
        return serviceService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update service")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service updated"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<ServiceResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceUpsertRequest request) {
        if (serviceService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(serviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete service (soft)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Service deleted"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (serviceService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/project-participation")
    @Operation(summary = "Toggle whether a service can participate in projects")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Service participation updated")})
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<ServiceResponse> updateServiceProjectParticipation(
            @PathVariable Long id,
            @Valid @RequestBody ServiceProjectParticipationRequest request) {
        if (serviceService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(serviceService.updateProjectParticipation(id, request.getAllowProjectParticipation()));
    }

    @PatchMapping("/provider/project-participation")
    @Operation(summary = "Toggle project participation for all services of a provider")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Provider services participation updated")})
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<Integer> updateProviderProjectParticipation(
            @Valid @RequestBody ProviderProjectParticipationRequest request) {
        int affected = serviceService.updateProviderProjectParticipation(
                request.getProviderId(),
                request.getAllowProjectParticipation()
        );
        return ResponseEntity.ok(affected);
    }

    @GetMapping("/provider/{providerId}/standing")
    @Operation(summary = "Get provider standing with stats and ML reliability score")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Provider standing retrieved")})
    public ProviderStandingResponse getProviderStanding(@PathVariable Long providerId) {
        ProviderStandingResponse standing = serviceService.getProviderStanding(providerId);

        List<Service> providerSvcs = serviceService.getProviderActiveServices(providerId);
        if (!providerSvcs.isEmpty()) {
            Service svc = providerSvcs.get(0);
            try {
                BookingPredictionResponse pred = mlPredictionService.predictBookingCompletionForService(svc);
                standing.setMlReliabilityScore(pred.getCompletionProbability() * 100);
                standing.setMlRiskLevel(pred.getRiskLevel());
                standing.setMlConfidence(pred.getConfidence());
                standing.setMlKeyFactors(pred.getKeyFactors());
                standing.setMlRecommendation(pred.getRecommendation());
            } catch (Exception e) {
                standing.setMlReliabilityScore(0.0);
                standing.setMlRiskLevel("UNKNOWN");
                standing.setMlConfidence("N/A");
                standing.setMlRecommendation("ML analysis unavailable");
            }
        }

        return standing;
    }

    @GetMapping("/provider/{providerId}/compatibility/{clientId}")
    @Operation(summary = "Get provider-client compatibility score")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Compatibility score retrieved")})
    public CompatibilityResponse getCompatibility(@PathVariable Long providerId, @PathVariable Long clientId) {
        return serviceService.getCompatibility(providerId, clientId);
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare multiple services side-by-side")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Comparison data retrieved")})
    public List<ServiceComparisonResponse> compareServices(@RequestBody List<Long> serviceIds) {
        return serviceService.compareServices(serviceIds);
    }

    @GetMapping("/surge-pricing")
    @Operation(summary = "Get dynamic surge pricing data for categories and providers")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Surge pricing data retrieved")})
    public SurgePricingResponse getSurgePricing() {
        return surgePricingService.getSurgeData();
    }
}
