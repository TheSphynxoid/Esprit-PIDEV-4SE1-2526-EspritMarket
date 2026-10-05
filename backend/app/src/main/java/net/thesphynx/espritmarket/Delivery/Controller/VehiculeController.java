package net.thesphynx.espritmarket.Delivery.Controller;

import net.thesphynx.espritmarket.Delivery.Dto.VehiculeDto;
import net.thesphynx.espritmarket.Delivery.Dto.VehiculeRequestDto;
import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import net.thesphynx.espritmarket.Delivery.Service.VehiculeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/delivery/vehicules")
@Tag(name = "Delivery - Vehicules")
public class VehiculeController {
    private final VehiculeService vehiculeService;

    public VehiculeController(VehiculeService vehiculeService) {
        this.vehiculeService = vehiculeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    @Operation(summary = "List all vehicules (ADMIN_DELIVERY only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicules retrieved"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<Vehicule> getAllVehiculesForAdmin() {
        return vehiculeService.getAllVehicules();
    }

    @GetMapping("/voitures")
    @Operation(summary = "List vehicules")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Vehicules retrieved")})
    public List<Vehicule> getVoitures(@AuthenticationPrincipal UserDetails userDetails) {
        return vehiculeService.getVoituresByUserEmail(userDetails.getUsername());
    }

    @GetMapping("/voitures/{userId}")
    @Operation(summary = "List vehicules (deprecated)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Vehicules retrieved")})
    public List<Vehicule> getVoituresLegacy(@PathVariable Long userId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        return vehiculeService.getVoituresByUserEmail(userDetails.getUsername());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicule by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicule found"),
        @ApiResponse(responseCode = "404", description = "Vehicule not found")
    })
    public ResponseEntity<Vehicule> getById(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        return vehiculeService.getByIdForUser(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create vehicule")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Vehicule created")})
    public Vehicule create(@Valid @RequestBody VehiculeRequestDto request,
                           @AuthenticationPrincipal UserDetails userDetails) {
        return vehiculeService.create(toEntity(request), userDetails.getUsername());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create vehicule with images")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Vehicule created")})
    public ResponseEntity<Vehicule> createMultipart(@Valid @ModelAttribute VehiculeDto request,
                                                    @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        Vehicule created = vehiculeService.create(toEntity(request), userDetails.getUsername());

        vehiculeService.uploadVehiclePhotoForUser(created.getId(), userDetails.getUsername(), request.getVehiclePhoto());
        vehiculeService.uploadRegistrationCardFrontForUser(created.getId(), userDetails.getUsername(), request.getRegistrationCardFront());
        vehiculeService.uploadRegistrationCardBackForUser(created.getId(), userDetails.getUsername(), request.getRegistrationCardBack());

        return vehiculeService.getByIdForUser(created.getId(), userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(created));
    }

    @PostMapping("/{userId}")
    @Operation(summary = "Create vehicule (deprecated)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Vehicule created")})
    public Vehicule createLegacy(@Valid @RequestBody VehiculeRequestDto request,
                                 @PathVariable Long userId,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        return vehiculeService.create(toEntity(request), userDetails.getUsername());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update vehicule")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicule updated"),
        @ApiResponse(responseCode = "404", description = "Vehicule not found")
    })
    public ResponseEntity<Vehicule> update(@PathVariable Long id,
                           @Valid @RequestBody VehiculeRequestDto request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return vehiculeService.updateForUser(id, toEntity(request), userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update vehicule with images")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicule updated"),
        @ApiResponse(responseCode = "404", description = "Vehicule not found")
    })
    public ResponseEntity<Vehicule> updateMultipart(@PathVariable Long id,
                                                    @Valid @ModelAttribute VehiculeDto request,
                                @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        ResponseEntity<Vehicule> updatedResponse = vehiculeService.updateForUser(id, toEntity(request), userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());

        if (!updatedResponse.getStatusCode().is2xxSuccessful()) {
            return updatedResponse;
        }

        vehiculeService.uploadVehiclePhotoForUser(id, userDetails.getUsername(), request.getVehiclePhoto());
        vehiculeService.uploadRegistrationCardFrontForUser(id, userDetails.getUsername(), request.getRegistrationCardFront());
        vehiculeService.uploadRegistrationCardBackForUser(id, userDetails.getUsername(), request.getRegistrationCardBack());

        return vehiculeService.getByIdForUser(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vehicule")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Vehicule deleted"),
        @ApiResponse(responseCode = "404", description = "Vehicule not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (!vehiculeService.deleteForUser(id, userDetails.getUsername())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = {"/{id}/photo", "/{id}/vehicle-photo"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload vehicle photo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle photo uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "404", description = "Vehicule not found")
    })
    public ResponseEntity<Vehicule> uploadVehiclePhoto(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestPart("file") MultipartFile file) throws IOException {
        return vehiculeService.uploadVehiclePhotoForUser(id, userDetails.getUsername(), file)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(value = {"/{id}/carte-grise/recto", "/{id}/registration-card/front"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload registration card front image")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration front uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "404", description = "Vehicule not found")
    })
    public ResponseEntity<Vehicule> uploadRegistrationCardFront(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetails userDetails,
                                                                @RequestPart("file") MultipartFile file) throws IOException {
        return vehiculeService.uploadRegistrationCardFrontForUser(id, userDetails.getUsername(), file)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(value = {"/{id}/carte-grise/verso", "/{id}/registration-card/back"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload registration card back image")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration back uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "404", description = "Vehicule not found")
    })
    public ResponseEntity<Vehicule> uploadRegistrationCardBack(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserDetails userDetails,
                                                               @RequestPart("file") MultipartFile file) throws IOException {
        return vehiculeService.uploadRegistrationCardBackForUser(id, userDetails.getUsername(), file)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({"/{id}/photo", "/{id}/vehicle-photo"})
    @Operation(summary = "Download vehicle photo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle photo returned"),
        @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    public ResponseEntity<byte[]> getVehiclePhoto(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails,
                                                   Authentication authentication) {
        return resolveVehiculeForImageAccess(id, userDetails, authentication)
                .filter(v -> v.getVehiclePhoto() != null && v.getVehiclePhoto().length > 0)
                .map(v -> buildImageResponse(v.getVehiclePhoto(), v.getVehiclePhotoContentType(), v.getVehiclePhotoFileName(), "vehicle-photo"))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({"/{id}/carte-grise/recto", "/{id}/registration-card/front"})
    @Operation(summary = "Download registration card front image")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration front returned"),
        @ApiResponse(responseCode = "404", description = "Image not found")
    })
    public ResponseEntity<byte[]> getRegistrationCardFront(@PathVariable Long id,
                                                            @AuthenticationPrincipal UserDetails userDetails,
                                                            Authentication authentication) {
        return resolveVehiculeForImageAccess(id, userDetails, authentication)
                .filter(v -> v.getRegistrationCardFront() != null && v.getRegistrationCardFront().length > 0)
                .map(v -> buildImageResponse(v.getRegistrationCardFront(), v.getRegistrationCardFrontContentType(), v.getRegistrationCardFrontFileName(), "carte-grise-recto"))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({"/{id}/carte-grise/verso", "/{id}/registration-card/back"})
    @Operation(summary = "Download registration card back image")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration back returned"),
        @ApiResponse(responseCode = "404", description = "Image not found")
    })
    public ResponseEntity<byte[]> getRegistrationCardBack(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails userDetails,
                                                           Authentication authentication) {
        return resolveVehiculeForImageAccess(id, userDetails, authentication)
                .filter(v -> v.getRegistrationCardBack() != null && v.getRegistrationCardBack().length > 0)
                .map(v -> buildImageResponse(v.getRegistrationCardBack(), v.getRegistrationCardBackContentType(), v.getRegistrationCardBackFileName(), "carte-grise-verso"))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private java.util.Optional<Vehicule> resolveVehiculeForImageAccess(Long id,
                                                                        UserDetails userDetails,
                                                                        Authentication authentication) {
        boolean isAdminDelivery = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN_DELIVERY".equals(a.getAuthority()));

        if (isAdminDelivery) {
            return vehiculeService.getById(id);
        }

        return vehiculeService.getByIdForUser(id, userDetails.getUsername());
    }

    private Vehicule toEntity(VehiculeRequestDto request) {
        Vehicule vehicule = new Vehicule();
        vehicule.setType(request.getType());
        vehicule.setRegistrationnumbers(request.getRegistrationnumbers());
        vehicule.setCapacity(request.getCapacity());
        vehicule.setStatus(request.getStatus());
        return vehicule;
    }

    private Vehicule toEntity(VehiculeDto request) {
        Vehicule vehicule = new Vehicule();
        vehicule.setType(request.getType());
        vehicule.setRegistrationnumbers(request.getRegistrationnumbers());
        vehicule.setCapacity(request.getCapacity());
        vehicule.setStatus(request.getStatus());
        return vehicule;
    }

    private ResponseEntity<byte[]> buildImageResponse(byte[] image,
                                                      String contentType,
                                                      String fileName,
                                                      String defaultFileName) {
        String safeContentType = contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String safeFileName = fileName != null ? fileName : defaultFileName;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(safeContentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName + "\"")
                .body(image);
    }
}
