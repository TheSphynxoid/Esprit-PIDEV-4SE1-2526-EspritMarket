package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Srv.Dto.PartnerRequest;
import net.thesphynx.espritmarket.Srv.Dto.PartnerResponse;
import net.thesphynx.espritmarket.Srv.Service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/srv/partners")
@Tag(name = "Srv - Partners")
@PreAuthorize("isAuthenticated()")
public class PartnerController {
    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping
    @Operation(summary = "List partners")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Partners retrieved")})
    public PageResponse<PartnerResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return partnerService.getAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get partner by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partner found"),
        @ApiResponse(responseCode = "404", description = "Partner not found")
    })
    public ResponseEntity<PartnerResponse> getById(@PathVariable Long id) {
        return partnerService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create partner")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Partner created")})
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public PartnerResponse create(@Valid @RequestBody PartnerRequest request) {
        return partnerService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update partner")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partner updated"),
        @ApiResponse(responseCode = "404", description = "Partner not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public ResponseEntity<PartnerResponse> update(@PathVariable Long id, @Valid @RequestBody PartnerRequest request) {
        if (partnerService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(partnerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete partner (soft)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Partner deleted"),
        @ApiResponse(responseCode = "404", description = "Partner not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (partnerService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        partnerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
