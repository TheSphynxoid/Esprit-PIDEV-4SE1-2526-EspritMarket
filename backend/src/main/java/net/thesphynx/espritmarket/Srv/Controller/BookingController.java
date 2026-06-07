package net.thesphynx.espritmarket.Srv.Controller;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.BookingAttachmentResponse;
import net.thesphynx.espritmarket.Srv.Dto.BookingMessageBatchResponse;
import net.thesphynx.espritmarket.Srv.Dto.BookingMessageRequest;
import net.thesphynx.espritmarket.Srv.Dto.BookingMessageResponse;
import net.thesphynx.espritmarket.Srv.Dto.BookingPredictionResponse;
import net.thesphynx.espritmarket.Srv.Dto.BookingRequest;
import net.thesphynx.espritmarket.Srv.Dto.BookingResponse;
import net.thesphynx.espritmarket.Srv.Dto.BookingStatusUpdateRequest;
import net.thesphynx.espritmarket.Srv.Entity.BookingAuditLog;
import net.thesphynx.espritmarket.Srv.Entity.BookingAttachment;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Service.BookingAttachmentService;
import net.thesphynx.espritmarket.Srv.Service.BookingMessageService;
import net.thesphynx.espritmarket.Srv.Service.BookingService;
import net.thesphynx.espritmarket.Srv.Service.FileStorageService;
import net.thesphynx.espritmarket.Srv.Service.MlPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/srv/bookings")
@Tag(name = "Srv - Bookings")
@PreAuthorize("isAuthenticated()")
public class BookingController {
    private final BookingService bookingService;
    private final BookingAttachmentService bookingAttachmentService;
    private final BookingMessageService bookingMessageService;
    private final FileStorageService fileStorageService;
    private final MlPredictionService mlPredictionService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService,
                             BookingAttachmentService bookingAttachmentService,
                             BookingMessageService bookingMessageService,
                             FileStorageService fileStorageService,
                             MlPredictionService mlPredictionService,
                             UserRepository userRepository) {
        this.bookingService = bookingService;
        this.bookingAttachmentService = bookingAttachmentService;
        this.bookingMessageService = bookingMessageService;
        this.fileStorageService = fileStorageService;
        this.mlPredictionService = mlPredictionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Booking created")})
    public BookingResponse create(@Valid @RequestBody BookingRequest request, Authentication auth) {
        Long userId = extractUserId(auth);
        return bookingService.create(request, userId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's bookings")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bookings retrieved")})
    public PageResponse<BookingResponse> getByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingService.getByUserId(userId, page, size);
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get provider's bookings")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bookings retrieved")})
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public PageResponse<BookingResponse> getByProviderId(
            @PathVariable Long providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingService.getByProviderId(providerId, page, size);
    }

    @GetMapping("/provider/{providerId}/status/{status}")
    @Operation(summary = "Get provider's bookings by status")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bookings retrieved")})
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public PageResponse<BookingResponse> getByProviderIdAndStatus(
            @PathVariable Long providerId,
            @PathVariable BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingService.getByProviderIdAndStatus(providerId, status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking found"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> getById(@PathVariable Long id) {
        return bookingService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update booking status (provider accepts/rejects)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request,
            Authentication auth) {
        Long providerId = extractUserId(auth);
        return ResponseEntity.ok(bookingService.updateStatus(id, request.getStatus(), providerId));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking (user)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(bookingService.cancel(id, userId));
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Get booking audit log")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Audit log retrieved")})
    public List<BookingAuditLog> getAuditLog(@PathVariable Long id) {
        return bookingService.getAuditLog(id);
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "Get shared files for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Attachments retrieved")})
    public List<BookingAttachmentResponse> getAttachments(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        return bookingAttachmentService.getByBookingId(id, userId);
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "Upload a shared file to a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "File uploaded")})
    public BookingAttachmentResponse uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return bookingAttachmentService.upload(id, userId, file);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @Operation(summary = "Delete a shared file from a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "File deleted")})
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId, Authentication auth) {
        Long userId = extractUserId(auth);
        bookingAttachmentService.delete(id, attachmentId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/attachments/orphans")
    @Operation(summary = "Clean orphan shared file records for a booking")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Orphan records cleaned")})
    public ResponseEntity<Integer> cleanupOrphanAttachments(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        int removed = bookingAttachmentService.cleanupOrphanAttachmentsForBooking(id, userId);
        return ResponseEntity.ok(removed);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get booking chat messages")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Messages retrieved")})
    public List<BookingMessageResponse> getMessages(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        return bookingMessageService.getByBookingId(id, userId);
    }

    @GetMapping("/{id}/messages/batch")
    @Operation(summary = "Get booking chat messages in cursor-based batches")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Message batch retrieved")})
    public BookingMessageBatchResponse getMessagesBatch(@PathVariable Long id,
                                                        @RequestParam(required = false) Long beforeId,
                                                        @RequestParam(defaultValue = "30") int limit,
                                                        Authentication auth) {
        Long userId = extractUserId(auth);
        return bookingMessageService.getBatch(id, userId, beforeId, limit);
    }

    @GetMapping("/{id}/messages/newer")
    @Operation(summary = "Get messages newer than given ID for sync/polling")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Newer messages retrieved")})
    public List<BookingMessageResponse> getNewerMessages(@PathVariable Long id,
                                                          @RequestParam Long afterId,
                                                          @RequestParam(defaultValue = "30") int limit,
                                                          Authentication auth) {
        Long userId = extractUserId(auth);
        return bookingMessageService.getNewer(id, userId, afterId, limit);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a booking chat message")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Message sent")})
    public BookingMessageResponse sendMessage(@PathVariable Long id,
                                              @Valid @RequestBody BookingMessageRequest request,
                                              Authentication auth) {
        Long userId = extractUserId(auth);
        return bookingMessageService.send(id, userId, request);
    }

    @GetMapping("/files/{filename}")
    @Operation(summary = "Serve booking shared file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File returned"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Resource> getBookingFile(@PathVariable String filename, Authentication auth) {
        Long userId = extractUserId(auth);
        String fileUrl = "/api/srv/bookings/files/" + filename;

        BookingAttachment attachment = bookingAttachmentService.getByFileUrl(fileUrl)
                .orElseThrow(() -> new BadRequestException("File not found"));

        if (!bookingAttachmentService.canAccessFile(attachment, userId)) {
            throw new BadRequestException("You are not authorized to access this file");
        }

        try {
            Path filePath = fileStorageService.resolveBookingFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                bookingAttachmentService.deleteIfPhysicalFileMissing(attachment);
                return ResponseEntity.notFound().build();
            }
            String contentType = fileStorageService.getContentType(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/ml-prediction")
    @Operation(summary = "Predict booking completion probability using trained ML model")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Prediction retrieved")})
    public BookingPredictionResponse predictBooking(@PathVariable Long id) {
        return mlPredictionService.predictBookingCompletion(bookingService.findEntityById(id));
    }

    private Long extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email))
                .getId();
    }
}
