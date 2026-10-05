package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateBatchRequest;
import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateRequest;
import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateResponse;
import net.thesphynx.espritmarket.Srv.Service.WeeklyTemplateService;
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
@RequestMapping("/api/srv/availability/templates")
@Tag(name = "Srv - Weekly Templates")
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
public class WeeklyTemplateController {
    private final WeeklyTemplateService weeklyTemplateService;

    public WeeklyTemplateController(WeeklyTemplateService weeklyTemplateService) {
        this.weeklyTemplateService = weeklyTemplateService;
    }

    @GetMapping
    @Operation(summary = "List all templates for a provider")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Templates retrieved")})
    public List<WeeklyTemplateResponse> getByProvider(@RequestParam Long providerId) {
        return weeklyTemplateService.getByProvider(providerId);
    }

    @GetMapping("/global")
    @Operation(summary = "List global (default) templates for a provider")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Templates retrieved")})
    public List<WeeklyTemplateResponse> getGlobalByProvider(@RequestParam Long providerId) {
        return weeklyTemplateService.getGlobalByProvider(providerId);
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "List templates for a provider+service")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Templates retrieved")})
    public List<WeeklyTemplateResponse> getByProviderAndService(
            @RequestParam Long providerId, @PathVariable Long serviceId) {
        return weeklyTemplateService.getByProviderAndService(providerId, serviceId);
    }

    @PostMapping
    @Operation(summary = "Create weekly template")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Template created")})
    public WeeklyTemplateResponse create(@Valid @RequestBody WeeklyTemplateRequest request) {
        return weeklyTemplateService.create(request);
    }

    @PutMapping("/batch")
    @Operation(summary = "Replace all templates for a provider (optionally for a specific service)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Templates replaced")})
    public List<WeeklyTemplateResponse> batchReplace(@Valid @RequestBody WeeklyTemplateBatchRequest request) {
        return weeklyTemplateService.batchReplace(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update weekly template")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template updated"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ResponseEntity<WeeklyTemplateResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody WeeklyTemplateRequest request) {
        return weeklyTemplateService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete weekly template")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Template deleted"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return weeklyTemplateService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
