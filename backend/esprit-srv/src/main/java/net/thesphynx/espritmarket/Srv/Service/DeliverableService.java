package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Event.NotificationEvent;
import net.thesphynx.espritmarket.Common.Event.StatusTransitionEvent;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.*;
import net.thesphynx.espritmarket.Srv.Entity.*;
import net.thesphynx.espritmarket.Srv.Mapper.DeliverableMapper;
import net.thesphynx.espritmarket.Srv.Repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeliverableService {

    private final IDeliverableRepository deliverableRepository;
    private final IDeliverableAttachmentRepository attachmentRepository;
    private final IDeliverableReviewRepository reviewRepository;
    private final IBookingRepository bookingRepository;
    private final IBookingAuditLogRepository auditLogRepository;
    private final IDeliverableVersionRepository deliverableVersionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final DeliverableMapper deliverableMapper;
    private final ApplicationEventPublisher eventPublisher;
    @Value("${srv.deliverable.version-retention-days:30}")
    private long versionRetentionDays;

    public DeliverableService(IDeliverableRepository deliverableRepository,
                              IDeliverableAttachmentRepository attachmentRepository,
                              IDeliverableReviewRepository reviewRepository,
                              IBookingRepository bookingRepository,
                              IBookingAuditLogRepository auditLogRepository,
                              IDeliverableVersionRepository deliverableVersionRepository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService,
                              DeliverableMapper deliverableMapper,
                              ApplicationEventPublisher eventPublisher) {
        this.deliverableRepository = deliverableRepository;
        this.attachmentRepository = attachmentRepository;
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
        this.deliverableVersionRepository = deliverableVersionRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.deliverableMapper = deliverableMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DeliverableResponse create(Long bookingId, DeliverableCreateRequest request, Long providerId) {
        Booking booking = findActiveBooking(bookingId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS && booking.getStatus() != BookingStatus.PENDING_REVIEW) {
            throw new BadRequestException("Deliverables can only be created for bookings in IN_PROGRESS or PENDING_REVIEW status");
        }

        if (booking.getProvider() == null || !booking.getProvider().getId().equals(providerId)) {
            throw new BadRequestException("Only the booking provider can create deliverables");
        }

        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", providerId));

        Deliverable deliverable = new Deliverable();
        deliverable.setBooking(booking);
        deliverable.setProvider(provider);
        deliverable.setTitle(request.getTitle());
        deliverable.setDescription(request.getDescription());
        deliverable.setStatus(DeliverableStatus.DRAFT);
        deliverable.setVersion(1);

        Deliverable saved = deliverableRepository.save(deliverable);
        return deliverableMapper.toResponse(saved);
    }

    @Transactional
    public DeliverableResponse createWithFiles(Long bookingId, DeliverableCreateRequest request,
                                                Long providerId, List<MultipartFile> files) {
        DeliverableResponse response = create(bookingId, request, providerId);
        Deliverable deliverable = deliverableRepository.findById(response.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable", response.getId()));

        if (files != null) {
            for (MultipartFile file : files) {
                addAttachment(deliverable, file);
            }
        }

        return deliverableMapper.toResponse(deliverableRepository.save(deliverable));
    }

    public List<DeliverableResponse> getByBookingId(Long bookingId) {
        return deliverableRepository.findByBookingIdAndDeletedAtIsNullOrderByCreatedAtDesc(bookingId).stream()
                .map(deliverableMapper::toSummaryResponse)
                .toList();
    }

    public DeliverableResponse getById(Long id) {
        Deliverable deliverable = findActiveDeliverable(id);
        return deliverableMapper.toResponse(deliverable);
    }

    @Transactional
    public DeliverableResponse submit(Long deliverableId, Long providerId) {
        Deliverable deliverable = findActiveDeliverable(deliverableId);

        if (deliverable.getStatus() != DeliverableStatus.DRAFT && deliverable.getStatus() != DeliverableStatus.REVISION_REQUESTED) {
            throw new BadRequestException("Only DRAFT or REVISION_REQUESTED deliverables can be submitted");
        }

        if (!deliverable.getProvider().getId().equals(providerId)) {
            throw new BadRequestException("Only the provider can submit this deliverable");
        }

        List<String> qualityWarnings = runQualityGates(deliverable);

        boolean isRevision = deliverable.getStatus() == DeliverableStatus.REVISION_REQUESTED;
        if (isRevision) {
            deliverable.setVersion(deliverable.getVersion() + 1);
        }

        deliverable.setStatus(DeliverableStatus.SUBMITTED);
        deliverable.setSubmittedAt(LocalDateTime.now());

        snapshotDeliverableVersion(deliverable);

        Booking booking = deliverable.getBooking();
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.PENDING_REVIEW);
        bookingRepository.save(booking);

        logBookingTransition(booking.getId(), oldStatus.name(), BookingStatus.PENDING_REVIEW.name(), providerId);
        eventPublisher.publishEvent(new StatusTransitionEvent(booking.getId(), "Booking", oldStatus.name(), BookingStatus.PENDING_REVIEW.name(), providerId));

        if (booking.getUser() != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                    booking.getUser().getId(), "DELIVERABLE",
                    "Deliverable Submitted",
                    "A deliverable has been submitted for booking #" + booking.getId(),
                    booking.getId(), "Booking"
            ));
        }

        Deliverable saved = deliverableRepository.save(deliverable);
        int qualityScore = computeQualityScore(deliverable);
        DeliverableResponse response = deliverableMapper.toResponse(saved);
        response.setQualityScore(qualityScore);
        response.setQualityWarnings(qualityWarnings.isEmpty() ? null : qualityWarnings);
        return response;
    }

    @Transactional
    public DeliverableResponse review(Long deliverableId, DeliverableReviewRequest request, Long reviewerId) {
        Deliverable deliverable = findActiveDeliverable(deliverableId);

        if (deliverable.getStatus() != DeliverableStatus.SUBMITTED) {
            throw new BadRequestException("Only SUBMITTED deliverables can be reviewed");
        }

        Booking booking = deliverable.getBooking();
        if (booking.getUser() == null || !booking.getUser().getId().equals(reviewerId)) {
            throw new BadRequestException("Only the booking client can review deliverables");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));

        DeliverableReview review = new DeliverableReview();
        review.setDeliverable(deliverable);
        review.setReviewer(reviewer);
        review.setDecision(request.getDecision());
        review.setComment(request.getComment());
        reviewRepository.save(review);

        BookingStatus oldBookingStatus = booking.getStatus();
        deliverable.setReviewedAt(LocalDateTime.now());

        switch (request.getDecision()) {
            case ACCEPTED -> {
                deliverable.setStatus(DeliverableStatus.ACCEPTED);
                booking.setStatus(BookingStatus.COMPLETED);
                logBookingTransition(booking.getId(), oldBookingStatus.name(), BookingStatus.COMPLETED.name(), reviewerId);
                eventPublisher.publishEvent(new StatusTransitionEvent(booking.getId(), "Booking", oldBookingStatus.name(), BookingStatus.COMPLETED.name(), reviewerId));

                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            booking.getUser().getId(), "BOOKING_STATUS",
                            "Booking Completed",
                            "Your deliverable has been accepted! Booking #" + booking.getId() + " is complete. Please leave a review!",
                            booking.getId(), "Booking"
                    ));
                }
                if (booking.getProvider() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            booking.getProvider().getId(), "DELIVERABLE",
                            "Deliverable Accepted",
                            "Your deliverable for booking #" + booking.getId() + " has been accepted!",
                            booking.getId(), "Booking"
                    ));
                }
            }
            case REVISION_REQUESTED -> {
                deliverable.setStatus(DeliverableStatus.REVISION_REQUESTED);
                booking.setStatus(BookingStatus.IN_PROGRESS);
                logBookingTransition(booking.getId(), oldBookingStatus.name(), BookingStatus.IN_PROGRESS.name(), reviewerId);
                eventPublisher.publishEvent(new StatusTransitionEvent(booking.getId(), "Booking", oldBookingStatus.name(), BookingStatus.IN_PROGRESS.name(), reviewerId));

                if (booking.getProvider() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            booking.getProvider().getId(), "DELIVERABLE",
                            "Revision Requested",
                            "A revision has been requested for your deliverable on booking #" + booking.getId(),
                            booking.getId(), "Booking"
                    ));
                }
            }
            case REJECTED -> {
                deliverable.setStatus(DeliverableStatus.REJECTED);
                booking.setStatus(BookingStatus.DISPUTED);
                logBookingTransition(booking.getId(), oldBookingStatus.name(), BookingStatus.DISPUTED.name(), reviewerId);
                eventPublisher.publishEvent(new StatusTransitionEvent(booking.getId(), "Booking", oldBookingStatus.name(), BookingStatus.DISPUTED.name(), reviewerId));

                if (booking.getProvider() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            booking.getProvider().getId(), "DELIVERABLE",
                            "Deliverable Rejected",
                            "Your deliverable for booking #" + booking.getId() + " has been rejected. The booking is now disputed.",
                            booking.getId(), "Booking"
                    ));
                }
            }
        }

        bookingRepository.save(booking);
        Deliverable saved = deliverableRepository.save(deliverable);
        return deliverableMapper.toResponse(saved);
    }

    @Transactional
    public DeliverableAttachmentResponse addAttachment(Long deliverableId, MultipartFile file, Long providerId) {
        Deliverable deliverable = findActiveDeliverable(deliverableId);

        if (deliverable.getStatus() != DeliverableStatus.DRAFT && deliverable.getStatus() != DeliverableStatus.REVISION_REQUESTED) {
            throw new BadRequestException("Attachments can only be added to DRAFT or REVISION_REQUESTED deliverables");
        }

        if (!deliverable.getProvider().getId().equals(providerId)) {
            throw new BadRequestException("Only the provider can add attachments");
        }

        DeliverableAttachment attachment = addAttachment(deliverable, file);
        deliverableRepository.save(deliverable);
        DeliverableAttachment saved = attachmentRepository.save(attachment);

        DeliverableAttachmentResponse response = new DeliverableAttachmentResponse();
        response.setId(saved.getId());
        response.setDeliverableId(deliverable.getId());
        response.setFileUrl(saved.getFileUrl());
        response.setFileName(saved.getFileName());
        response.setFileSize(saved.getFileSize());
        response.setFileType(saved.getFileType());
        response.setUploadedAt(saved.getUploadedAt());
        return response;
    }

    @Transactional
    public void deleteAttachment(Long deliverableId, Long attachmentId, Long providerId) {
        Deliverable deliverable = findActiveDeliverable(deliverableId);

        if (deliverable.getStatus() != DeliverableStatus.DRAFT) {
            throw new BadRequestException("Attachments can only be deleted from DRAFT deliverables");
        }

        if (!deliverable.getProvider().getId().equals(providerId)) {
            throw new BadRequestException("Only the provider can delete attachments");
        }

        DeliverableAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliverableAttachment", attachmentId));

        if (!attachment.getDeliverable().getId().equals(deliverableId)) {
            throw new BadRequestException("Attachment does not belong to this deliverable");
        }

        fileStorageService.delete(attachment.getFileUrl());
        deliverable.getAttachments().remove(attachment);
        attachmentRepository.delete(attachment);
    }

    public List<DeliverableReviewResponse> getHistory(Long deliverableId) {
        Deliverable deliverable = findActiveDeliverable(deliverableId);
        return reviewRepository.findByDeliverableOrderByReviewedAtDesc(deliverable).stream()
                .map(review -> {
                    DeliverableReviewResponse response = new DeliverableReviewResponse();
                    response.setId(review.getId());
                    response.setDeliverableId(deliverable.getId());
                    if (review.getReviewer() != null) {
                        response.setReviewerId(review.getReviewer().getId());
                        response.setReviewerName(review.getReviewer().getName());
                    }
                    response.setDecision(review.getDecision());
                    response.setComment(review.getComment());
                    response.setReviewedAt(review.getReviewedAt());
                    return response;
                })
                .toList();
    }

    public Optional<DeliverableAttachment> getAttachmentByFileUrl(String fileUrl) {
        return attachmentRepository.findByFileUrl(fileUrl);
    }

    public List<DeliverableVersionResponse> getVersions(Long deliverableId, Long userId) {
        Deliverable deliverable = findActiveDeliverable(deliverableId);
        if (!isParticipant(deliverable.getBooking(), userId)) {
            throw new BadRequestException("You are not authorized to view deliverable versions");
        }

        return deliverableVersionRepository.findByDeliverableIdOrderByVersionNumberDesc(deliverableId).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void cleanupOldCompletedDeliverableVersions() {
        LocalDateTime threshold = LocalDateTime.now().minus(versionRetentionDays, ChronoUnit.DAYS);
        List<Deliverable> completedDeliverables = deliverableRepository.findByBookingStatusAndDeletedAtIsNull(BookingStatus.COMPLETED);

        for (Deliverable deliverable : completedDeliverables) {
            List<DeliverableVersion> versions = deliverableVersionRepository.findByDeliverableIdOrderByVersionNumberDesc(deliverable.getId());
            if (versions.isEmpty()) continue;

            int latestVersion = versions.get(0).getVersionNumber();
            List<DeliverableVersion> oldVersions = deliverableVersionRepository
                    .findByDeliverableIdAndVersionNumberLessThanAndCreatedAtBefore(
                            deliverable.getId(), latestVersion, threshold);

            if (!oldVersions.isEmpty()) {
                deliverableVersionRepository.deleteAll(oldVersions);
            }
        }
    }

    public boolean canAccessAttachment(DeliverableAttachment attachment, Long userId) {
        if (attachment == null || attachment.getDeliverable() == null || attachment.getDeliverable().getBooking() == null) {
            return false;
        }

        Booking booking = attachment.getDeliverable().getBooking();
        if (booking.getUser() != null && booking.getUser().getId().equals(userId)) {
            return true;
        }
        return booking.getProvider() != null && booking.getProvider().getId().equals(userId);
    }

    @Transactional
    public boolean deleteAttachmentIfPhysicalFileMissing(DeliverableAttachment attachment) {
        if (attachment == null) return false;
        if (isDeliverableFilePresent(attachment.getFileUrl())) return false;
        attachmentRepository.deleteById(attachment.getId());
        return true;
    }

    private boolean isDeliverableFilePresent(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/api/srv/deliverables/files/")) {
            return false;
        }

        String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path path = fileStorageService.resolveFilePath(filename);
        return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }

    private DeliverableAttachment addAttachment(Deliverable deliverable, MultipartFile file) {
        String fileUrl = fileStorageService.store(file, deliverable.getId());

        DeliverableAttachment attachment = new DeliverableAttachment();
        attachment.setDeliverable(deliverable);
        attachment.setFileUrl(fileUrl);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        deliverable.getAttachments().add(attachment);
        return attachment;
    }

    private void snapshotDeliverableVersion(Deliverable deliverable) {
        DeliverableVersion version = new DeliverableVersion();
        version.setDeliverable(deliverable);
        version.setVersionNumber(deliverable.getVersion());
        version.setStatus(deliverable.getStatus());
        version.setSubmittedAt(deliverable.getSubmittedAt());
        version.setReviewedAt(deliverable.getReviewedAt());

        if (deliverable.getAttachments() != null) {
            for (DeliverableAttachment attachment : deliverable.getAttachments()) {
                DeliverableVersionAttachment versionAttachment = new DeliverableVersionAttachment();
                versionAttachment.setDeliverableVersion(version);
                versionAttachment.setFileUrl(attachment.getFileUrl());
                versionAttachment.setFileName(attachment.getFileName());
                versionAttachment.setFileSize(attachment.getFileSize());
                versionAttachment.setFileType(attachment.getFileType());
                versionAttachment.setUploadedAt(attachment.getUploadedAt());
                version.getAttachments().add(versionAttachment);
            }
        }

        deliverableVersionRepository.save(version);
    }

    private DeliverableVersionResponse toVersionResponse(DeliverableVersion version) {
        DeliverableVersionResponse response = new DeliverableVersionResponse();
        response.setId(version.getId());
        if (version.getDeliverable() != null) {
            response.setDeliverableId(version.getDeliverable().getId());
        }
        response.setVersionNumber(version.getVersionNumber());
        response.setStatus(version.getStatus());
        response.setSubmittedAt(version.getSubmittedAt());
        response.setReviewedAt(version.getReviewedAt());
        response.setCreatedAt(version.getCreatedAt());
        response.setAttachments(version.getAttachments().stream().map(att -> {
            DeliverableVersionAttachmentResponse ar = new DeliverableVersionAttachmentResponse();
            ar.setFileUrl(att.getFileUrl());
            ar.setFileName(att.getFileName());
            ar.setFileSize(att.getFileSize());
            ar.setFileType(att.getFileType());
            ar.setUploadedAt(att.getUploadedAt());
            return ar;
        }).toList());
        return response;
    }

    private boolean isParticipant(Booking booking, Long userId) {
        if (booking == null || userId == null) return false;
        if (booking.getUser() != null && booking.getUser().getId().equals(userId)) return true;
        return booking.getProvider() != null && booking.getProvider().getId().equals(userId);
    }

    private Booking findActiveBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private Deliverable findActiveDeliverable(Long id) {
        return deliverableRepository.findById(id)
                .filter(d -> d.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable", id));
    }

    private void logBookingTransition(Long bookingId, String fromStatus, String toStatus, Long changedBy) {
        BookingAuditLog log = new BookingAuditLog();
        log.setBookingId(bookingId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setChangedBy(changedBy);
        auditLogRepository.save(log);
    }

    private List<String> runQualityGates(Deliverable deliverable) {
        List<String> warnings = new java.util.ArrayList<>();
        List<DeliverableAttachment> attachments = deliverable.getAttachments();

        if (attachments == null || attachments.isEmpty()) {
            warnings.add("No files attached — consider adding deliverable files before submitting.");
        }

        if (deliverable.getDescription() == null || deliverable.getDescription().isBlank()) {
            warnings.add("No description provided — adding details helps the client understand the deliverable.");
        }

        if (deliverable.getTitle() == null || deliverable.getTitle().isBlank()) {
            warnings.add("No title provided — a descriptive title helps with organization.");
        }

        return warnings;
    }

    private int computeQualityScore(Deliverable deliverable) {
        int score = 0;
        int maxScore = 30;

        List<DeliverableAttachment> attachments = deliverable.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            score += 10;
            if (attachments.size() >= 2) score += 5;
            long totalSize = attachments.stream().mapToLong(a -> a.getFileSize() != null ? a.getFileSize() : 0L).sum();
            if (totalSize > 0) score += 5;
        }

        if (deliverable.getDescription() != null && deliverable.getDescription().length() >= 50) {
            score += 5;
        }

        if (deliverable.getTitle() != null && deliverable.getTitle().length() >= 5) {
            score += 5;
        }

        return Math.min(100, (score * 100) / maxScore);
    }
}
