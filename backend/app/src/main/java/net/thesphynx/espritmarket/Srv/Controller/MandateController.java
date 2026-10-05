package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Srv.Dto.ProviderMandateRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProviderMandateResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceMandateRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceMandateResponse;
import net.thesphynx.espritmarket.Srv.Service.MandateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/srv/mandates")
@Tag(name = "Srv - Mandates")
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
public class MandateController {
    private final MandateService mandateService;

    public MandateController(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @GetMapping("/service")
    @Operation(summary = "List service mandates for a provider")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Mandates retrieved")})
    public List<ServiceMandateResponse> getServiceMandates(@RequestParam Long providerId) {
        return mandateService.getServiceMandates(providerId);
    }

    @PostMapping("/service")
    @Operation(summary = "Create/update service mandate")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Mandate created")})
    public ServiceMandateResponse createServiceMandate(@Valid @RequestBody ServiceMandateRequest request) {
        return mandateService.createServiceMandate(request);
    }

    @DeleteMapping("/service/{id}")
    @Operation(summary = "Delete service mandate")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Mandate deleted")})
    public ResponseEntity<Void> deleteServiceMandate(@PathVariable Long id) {
        return mandateService.deleteServiceMandate(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/provider")
    @Operation(summary = "Get provider mandate")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Mandate retrieved")})
    public ResponseEntity<ProviderMandateResponse> getProviderMandate(@RequestParam Long providerId) {
        return mandateService.getProviderMandate(providerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(null));
    }

    @PostMapping("/provider")
    @Operation(summary = "Create/update provider mandate")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Mandate created")})
    public ProviderMandateResponse createProviderMandate(@Valid @RequestBody ProviderMandateRequest request) {
        return mandateService.createProviderMandate(request);
    }

    @DeleteMapping("/provider/{id}")
    @Operation(summary = "Delete provider mandate")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Mandate deleted")})
    public ResponseEntity<Void> deleteProviderMandate(@PathVariable Long id) {
        return mandateService.deleteProviderMandate(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
