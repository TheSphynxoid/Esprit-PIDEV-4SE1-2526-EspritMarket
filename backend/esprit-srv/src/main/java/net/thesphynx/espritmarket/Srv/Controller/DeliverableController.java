package net.thesphynx.espritmarket.Srv.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.*;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableAttachment;
import net.thesphynx.espritmarket.Srv.Service.DeliverableService;
import net.thesphynx.espritmarket.Srv.Service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/srv")
@Tag(name = "Srv - Deliverables")
@PreAuthorize("isAuthenticated()")
public class DeliverableController {

    private final DeliverableService deliverableService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public DeliverableController(DeliverableService deliverableService,
                                 FileStorageService fileStorageService,
                                 UserRepository userRepository) {
        this.deliverableService = deliverableService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @PostMapping("/bookings/{bookingId}/deliverables")
    @Operation(summary = "Create deliverable for a booking (provider)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deliverable created"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public DeliverableResponse create(
            @PathVariable Long bookingId,
            @Valid @RequestPart("metadata") DeliverableCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication auth) {
        Long providerId = extractUserId(auth);
        return deliverableService.createWithFiles(bookingId, request, providerId, files);
    }

    @GetMapping("/bookings/{bookingId}/deliverables")
    @Operation(summary = "List deliverables for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Deliverables retrieved")})
    public List<DeliverableResponse> getByBookingId(@PathVariable Long bookingId) {
        return deliverableService.getByBookingId(bookingId);
    }

    @GetMapping("/deliverables/{id}")
    @Operation(summary = "Get deliverable detail with attachments and reviews", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deliverable found"),
            @ApiResponse(responseCode = "404", description = "Deliverable not found")
    })
    public ResponseEntity<DeliverableResponse> getById(@PathVariable Long id) {
        return withLegacyHeaders(ResponseEntity.ok(deliverableService.getById(id)));
    }

    @PatchMapping("/deliverables/{id}/submit")
    @Operation(summary = "Submit draft deliverable for review (provider)", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deliverable submitted"),
            @ApiResponse(responseCode = "404", description = "Deliverable not found")
    })
    public ResponseEntity<DeliverableResponse> submit(@PathVariable Long id, Authentication auth) {
        Long providerId = extractUserId(auth);
        return withLegacyHeaders(ResponseEntity.ok(deliverableService.submit(id, providerId)));
    }

    @PostMapping("/deliverables/{id}/review")
    @Operation(summary = "Review a deliverable (client: accept / revision / reject)", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review submitted"),
            @ApiResponse(responseCode = "404", description = "Deliverable not found")
    })
    public ResponseEntity<DeliverableResponse> review(
            @PathVariable Long id,
            @Valid @RequestBody DeliverableReviewRequest request,
            Authentication auth) {
        Long reviewerId = extractUserId(auth);
        return withLegacyHeaders(ResponseEntity.ok(deliverableService.review(id, request, reviewerId)));
    }

    @PostMapping("/deliverables/{id}/attachments")
    @Operation(summary = "Add attachment to a deliverable (provider)", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment added"),
            @ApiResponse(responseCode = "404", description = "Deliverable not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<DeliverableAttachmentResponse> addAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        Long providerId = extractUserId(auth);
        return withLegacyHeaders(ResponseEntity.ok(deliverableService.addAttachment(id, file, providerId)));
    }

    @DeleteMapping("/deliverables/{id}/attachments/{attachmentId}")
    @Operation(summary = "Remove attachment from deliverable (provider, draft only)", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment removed"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId,
            Authentication auth) {
        Long providerId = extractUserId(auth);
        deliverableService.deleteAttachment(id, attachmentId, providerId);
        return withLegacyHeaders(ResponseEntity.ok().build());
    }

    @GetMapping("/deliverables/{id}/history")
    @Operation(summary = "Get review history for a deliverable", deprecated = true)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Review history retrieved")})
    public ResponseEntity<List<DeliverableReviewResponse>> getHistory(@PathVariable Long id) {
        return withLegacyHeaders(ResponseEntity.ok(deliverableService.getHistory(id)));
    }

    @GetMapping("/deliverables/{id}/versions")
    @Operation(summary = "Get immutable deliverable versions", deprecated = true)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Deliverable versions retrieved")})
    public ResponseEntity<List<DeliverableVersionResponse>> getVersions(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        return withLegacyHeaders(ResponseEntity.ok(deliverableService.getVersions(id, userId)));
    }

    @GetMapping("/deliverables/files/{filename}")
    @Operation(summary = "Serve deliverable attachment file", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File returned"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Resource> getFile(@PathVariable String filename, Authentication auth) {
        try {
            String fileUrl = "/api/srv/deliverables/files/" + filename;
            DeliverableAttachment attachment = deliverableService.getAttachmentByFileUrl(fileUrl)
                    .orElseThrow(() -> new IllegalArgumentException("File not found"));

            Long userId = extractUserId(auth);
            if (!deliverableService.canAccessAttachment(attachment, userId)) {
                return ResponseEntity.status(403).build();
            }

            Path filePath = fileStorageService.resolveFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                deliverableService.deleteAttachmentIfPhysicalFileMissing(attachment);
                return ResponseEntity.notFound().build();
            }
            String contentType = fileStorageService.getContentType(filePath);
            return withLegacyHeaders(ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static final String SUNSET_DATE = "Mon, 30 Jun 2026 00:00:00 GMT";

    private <T> ResponseEntity<T> withLegacyHeaders(ResponseEntity<T> response) {
        return ResponseEntity.status(response.getStatusCode())
                .header("Deprecation", "true")
                .header("Sunset", SUNSET_DATE)
                .headers(response.getHeaders())
                .body(response.getBody());
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
