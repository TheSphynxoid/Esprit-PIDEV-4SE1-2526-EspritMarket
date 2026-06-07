package net.thesphynx.espritmarket.Delivery.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Delivery.Dto.CourierProfileUpdateRequest;
import net.thesphynx.espritmarket.Delivery.Dto.CourierProfileResponse;
import net.thesphynx.espritmarket.Delivery.Dto.DeliveryCourierContactResponse;
import net.thesphynx.espritmarket.Delivery.Dto.VerificationResponse;
import net.thesphynx.espritmarket.Delivery.Entity.Courier;
import net.thesphynx.espritmarket.Delivery.Service.CourierService;
import net.thesphynx.espritmarket.Delivery.Service.FlaskVerificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/delivery/courier-profile")
@Tag(name = "Delivery - Courier Profile")
public class CourierProfileController {

    private final CourierService courierService;
    private final FlaskVerificationService flaskVerificationService;

    public CourierProfileController(CourierService courierService,
                                    FlaskVerificationService flaskVerificationService) {
        this.courierService = courierService;
        this.flaskVerificationService = flaskVerificationService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get authenticated courier profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier profile retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CourierProfileResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(courierService.getMyProfile(authentication.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update authenticated courier profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CourierProfileResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody CourierProfileUpdateRequest request) {
        return ResponseEntity.ok(courierService.updateMyPhoneNumber(authentication.getName(), request));
    }

    @PutMapping(value = "/me/permit-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Upload permit image for authenticated courier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permit image saved"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CourierProfileResponse> uploadPermitImage(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(courierService.uploadPermitImage(authentication.getName(), file));
    }

    @GetMapping("/me/permit-image")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Download permit image for authenticated courier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permit image returned"),
            @ApiResponse(responseCode = "404", description = "No permit image uploaded"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<byte[]> getPermitImage(Authentication authentication) {
        Courier courier = courierService.getMyCourierWithImage(authentication.getName());

        if (courier.getPermitImage() == null || courier.getPermitImage().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = courier.getPermitContentType() != null
                ? courier.getPermitContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        String filename = courier.getPermitFileName() != null
                ? courier.getPermitFileName()
                : "permit-image";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(courier.getPermitImage());
    }

    @GetMapping("/by-vehicule/{vehiculeId}")
    @Operation(summary = "Get courier contact by vehicule id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier contact returned"),
            @ApiResponse(responseCode = "404", description = "Courier not found for vehicule")
    })
    public ResponseEntity<DeliveryCourierContactResponse> getCourierByVehiculeId(@PathVariable Long vehiculeId) {
        return courierService.getCourierContactByVehiculeId(vehiculeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── NOUVEAU ENDPOINT IA ──────────────────────────────────────────────────

    @PostMapping(value = "/me/verify-permit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Verify permit identity: compare stored permit image with a selfie via AI")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification result returned"),
            @ApiResponse(responseCode = "400", description = "No permit uploaded or invalid selfie"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<VerificationResponse> verifyPermitIdentity(
            Authentication authentication,
            @RequestPart("selfie") MultipartFile selfieFile) throws IOException {

        // Récupérer le courier avec le permis déjà stocké en DB
        Courier courier = courierService.getMyCourierWithImage(authentication.getName());

        // Vérifier que le permis a bien été uploadé
        if (courier.getPermitImage() == null || courier.getPermitImage().length == 0) {
            return ResponseEntity.badRequest().build();
        }

        // Envoyer permis (DB) + selfie (upload) à Flask IA
        VerificationResponse result = flaskVerificationService.verifyPermitWithSelfie(
                courier.getPermitImage(),
                courier.getPermitContentType(),
                selfieFile.getBytes(),
                String.valueOf(courier.getId())
        );

                if (result.isFaceVerified()) {
                        courierService.markProfileCompletedAfterFaceVerification(authentication.getName());
                }

        return ResponseEntity.ok(result);
    }
}