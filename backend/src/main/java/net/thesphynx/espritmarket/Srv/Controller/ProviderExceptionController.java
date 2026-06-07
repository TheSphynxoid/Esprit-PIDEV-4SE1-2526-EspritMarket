package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Srv.Dto.ProviderExceptionRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProviderExceptionResponse;
import net.thesphynx.espritmarket.Srv.Service.ProviderExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/srv/availability/exceptions")
@Tag(name = "Srv - Provider Exceptions")
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
public class ProviderExceptionController {
    private final ProviderExceptionService providerExceptionService;

    public ProviderExceptionController(ProviderExceptionService providerExceptionService) {
        this.providerExceptionService = providerExceptionService;
    }

    @GetMapping
    @Operation(summary = "List exceptions for a provider")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Exceptions retrieved")})
    public List<ProviderExceptionResponse> getByProvider(@RequestParam Long providerId) {
        return providerExceptionService.getByProvider(providerId);
    }

    @GetMapping("/date-range")
    @Operation(summary = "List exceptions for a date range")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Exceptions retrieved")})
    public List<ProviderExceptionResponse> getByDateRange(
            @RequestParam Long providerId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return providerExceptionService.getByDateRange(providerId, startDate, endDate);
    }

    @PostMapping
    @Operation(summary = "Create exception")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Exception created")})
    public ProviderExceptionResponse create(@Valid @RequestBody ProviderExceptionRequest request) {
        return providerExceptionService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update exception")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Exception updated"),
        @ApiResponse(responseCode = "404", description = "Exception not found")
    })
    public ResponseEntity<ProviderExceptionResponse> update(@PathVariable Long id,
                                                             @Valid @RequestBody ProviderExceptionRequest request) {
        return providerExceptionService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete exception")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Exception deleted"),
        @ApiResponse(responseCode = "404", description = "Exception not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return providerExceptionService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
