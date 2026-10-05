package net.thesphynx.espritmarket.Marketplace.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestResponse;
import net.thesphynx.espritmarket.Marketplace.Service.SellerRequestService;

@RestController
@RequestMapping("/api/marketplace/seller-requests")
@Tag(name = "Marketplace - Seller Requests")
@Validated
public class SellerRequestController {

    private final SellerRequestService sellerRequestService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final jakarta.validation.Validator validator;

    public SellerRequestController(SellerRequestService sellerRequestService,
            jakarta.validation.Validator validator) {
        this.sellerRequestService = sellerRequestService;
        this.validator = validator;
    }

    @GetMapping
    @Operation(summary = "List all seller requests (Admin)")
    public ResponseEntity<List<SellerRequestResponse>> getAll() {
        return ResponseEntity.ok(sellerRequestService.getAllRequests());
    }

    @PostMapping("/submit/{userId}")
    @Operation(summary = "Submit a new seller request")
    public ResponseEntity<SellerRequestResponse> submitRequest(
            @PathVariable Long userId,
            @Valid @RequestBody SellerRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerRequestService.createRequest(request, userId));
    }

    @PostMapping(value = "/submit-multipart/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a new seller request with student card file")
    public ResponseEntity<SellerRequestResponse> submitRequestMultipart(
            @PathVariable Long userId,
            @RequestPart(value = "payload", required = false) String payload,
            @RequestPart(value = "request", required = false) String requestPayload,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "carteEtudiantFile", required = false) MultipartFile carteEtudiantFile,
            @RequestPart(value = "numeroEtudiant", required = false) String numeroEtudiant,
            @RequestPart(value = "prenom", required = false) String prenom,
            @RequestPart(value = "nom", required = false) String nom,
            @RequestPart(value = "email", required = false) String email) {
        SellerRequestRequest request = resolveMultipartRequest(
                payload, requestPayload, numeroEtudiant, prenom, nom, email);
        validateRequest(request);
        MultipartFile effectiveFile = (carteEtudiantFile != null && !carteEtudiantFile.isEmpty())
                ? carteEtudiantFile : file;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerRequestService.createRequestWithCardFile(request, userId, effectiveFile));
    }

    // ─── Envoie le code de vérification par email ─────────────────────
    @PostMapping("/send-verification-code")
    @Operation(summary = "Send verification code to student email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code sent to email"),
            @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public ResponseEntity<Map<String, String>> sendVerificationCode(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required", "error", "EMPTY_EMAIL"));
        }

        email = email.trim().toLowerCase();

        if (!email.endsWith("@esprit.tn")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email must end with @esprit.tn",
                                 "error", "INVALID_DOMAIN"));
        }

        try {
            // ✅ Le service envoie le code par email et ne le retourne plus
            Map<String, String> result = sellerRequestService.sendVerificationCode(email);
            return ResponseEntity.ok(result);
        } catch (BadRequestException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage(), "error", "BAD_REQUEST"));
        } catch (Exception ex) {
            logger.error("Unexpected error sending verification code", ex);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Failed to send verification code: " + ex.getMessage(),
                                 "error", "SEND_FAILED"));
        }
    }

    // ─── Vérifie le code entré par l'étudiant ─────────────────────────
    @PostMapping("/verify-code/{userId}")
    @Operation(summary = "Verify the code entered by the student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired code")
    })
    public ResponseEntity<Map<String, Object>> verifyCode(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        // Securite (A5a): le chemin fournit encore {userId} pour compatibilite
        // frontend, mais il est ignore. L'identite vient du principal (email JWT).
        String email = body.get("email");
        String code = body.get("code");

        if (email == null || code == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email and code are required"));
        }

        Long callerId;
        try {
            callerId = resolveCallerId(authentication);
        } catch (BadRequestException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }

        try {
            Map<String, Object> result = sellerRequestService.verifyCode(email, code, callerId);
            return ResponseEntity.ok(result);
        } catch (BadRequestException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/activate-seller/{userId}")
    @Operation(summary = "Promote current user to SELLER and allow step 2")
    public ResponseEntity<Map<String, Object>> activateSeller(
            @PathVariable Long userId,
            Authentication authentication) {
        // Securite (A2): {userId} du chemin ignore pour compatibilite URL ; le
        // compte elu est celui du principal. Pas de @PreAuthorize SELLER ici :
        // c'est precisement le passage au role SELLER apres verification.
        return ResponseEntity.ok(
                sellerRequestService.activateSeller(resolveCallerId(authentication)));
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "Get seller request status by user ID")
    public ResponseEntity<SellerRequestResponse> getStatus(
            @PathVariable Long userId,
            Authentication authentication) {
        // Securite (A3b): {userId} du chemin ignore ; la sonde du frontend ne peut
        // consulter que sa propre demande (ResourceNotFoundException 404 conservee
        // si elle n'en a pas).
        return ResponseEntity.ok(
                sellerRequestService.getRequestByUserId(resolveCallerId(authentication)));
    }

    @GetMapping("/status")
    @Operation(summary = "Get seller request status by user ID query param")
    public ResponseEntity<SellerRequestResponse> getStatusByQuery(
            @RequestParam(value = "userId", required = false) Long userId,
            Authentication authentication) {
        // Securite (A3b): userId de la query ignore, meme traitement que /status/{id}.
        return ResponseEntity.ok(
                sellerRequestService.getRequestByUserId(resolveCallerId(authentication)));
    }

    @PostMapping("/{requestId}/approve/{adminId}")
    @Operation(summary = "Approve a seller request (Admin)")
    // Securite (A3): reserve aux roles admin. ADMIN_MARKET est inclus car le
    // dashboard d'approbation est aussi servi aux comptes portant ce role
    // (voir AuthController.ADMIN_ROLES).
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_MARKET')")
    public ResponseEntity<SellerRequestResponse> approveRequest(
            @PathVariable Long requestId,
            @PathVariable Long adminId,
            Authentication authentication) {
        // Securite (A3): {adminId} du chemin ignore — le validateur enregistre
        // (validatedBy) est l'administrateur authentifie, pas un id forgeable.
        return ResponseEntity.ok(
                sellerRequestService.approveRequest(requestId, resolvedAdminEmail(authentication)));
    }

    @PostMapping("/{requestId}/refuse/{adminId}")
    @Operation(summary = "Refuse a seller request (Admin)")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_MARKET')")
    public ResponseEntity<SellerRequestResponse> refuseRequest(
            @PathVariable Long requestId,
            @PathVariable Long adminId,
            Authentication authentication) {
        // Securite (A3): identique a approveRequest.
        return ResponseEntity.ok(
                sellerRequestService.refuseRequest(requestId, resolvedAdminEmail(authentication)));
    }

    // ─── Helpers privés ───────────────────────────────────────────────

    /**
     * Resout l'id de l'appelant depuis le principal. Authentication.getName()
     * est l'email de l'utilisateur (voir CustomUserDetailsService).
     */
    private Long resolveCallerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new BadRequestException("Authentication is required");
        }
        return sellerRequestService.resolveUserIdFromAuthentication(authentication.getName());
    }

    /** Email du principal, pour l'enregistrement de la validation admin. */
    private String resolvedAdminEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new BadRequestException("Authentication is required");
        }
        return authentication.getName();
    }

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(SellerRequestController.class);

    private SellerRequestRequest resolveMultipartRequest(String payload, String requestPayload,
            String numeroEtudiant, String prenom, String nom, String email) {
        String jsonPayload = StringUtils.hasText(payload) ? payload : requestPayload;
        if (StringUtils.hasText(jsonPayload)) {
            return parsePayload(jsonPayload);
        }
        SellerRequestRequest request = new SellerRequestRequest();
        request.setNumeroEtudiant(numeroEtudiant);
        request.setPrenom(prenom);
        request.setNom(nom);
        request.setEmail(email);
        return request;
    }

    private SellerRequestRequest parsePayload(String jsonPayload) {
        String trimmedPayload = jsonPayload.trim();
        try {
            return objectMapper.readValue(trimmedPayload, SellerRequestRequest.class);
        } catch (JsonProcessingException first) {
            try {
                String unwrapped = objectMapper.readValue(trimmedPayload, String.class);
                return objectMapper.readValue(unwrapped, SellerRequestRequest.class);
            } catch (JsonProcessingException second) {
                throw new BadRequestException("Invalid payload JSON in multipart request");
            }
        }
    }

    private void validateRequest(SellerRequestRequest request) {
        request.setCarteEtudiantUrl("uploaded");
        Set<ConstraintViolation<SellerRequestRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}