package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Common.Event.NotificationEvent;
import net.thesphynx.espritmarket.Common.Event.StatusTransitionEvent;
import net.thesphynx.espritmarket.Srv.Dto.BookingRequest;
import net.thesphynx.espritmarket.Srv.Dto.BookingResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingAuditLog;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.PricingType;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Entity.ServicePackage;
import net.thesphynx.espritmarket.Srv.Mapper.BookingMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingAuditLogRepository;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServicePackageRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
public class BookingService {
    private static final Map<BookingStatus, List<BookingStatus>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry(BookingStatus.PENDING, List.of(BookingStatus.PENDING_EVALUATION, BookingStatus.APPROVED, BookingStatus.REJECTED, BookingStatus.CANCELLED)),
            Map.entry(BookingStatus.PENDING_EVALUATION, List.of(BookingStatus.TENTATIVE, BookingStatus.REJECTED)),
            Map.entry(BookingStatus.TENTATIVE, List.of(BookingStatus.CONFIRMED, BookingStatus.APPROVED, BookingStatus.CANCELLED)),
            Map.entry(BookingStatus.APPROVED, List.of(BookingStatus.IN_PROGRESS, BookingStatus.CONFIRMED, BookingStatus.CANCELLED)),
            Map.entry(BookingStatus.CONFIRMED, List.of(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED)),
            Map.entry(BookingStatus.IN_PROGRESS, List.of(BookingStatus.COMPLETED, BookingStatus.PENDING_REVIEW)),
            Map.entry(BookingStatus.PENDING_REVIEW, List.of(BookingStatus.COMPLETED, BookingStatus.IN_PROGRESS, BookingStatus.DISPUTED)),
            Map.entry(BookingStatus.REJECTED, List.of()),
            Map.entry(BookingStatus.COMPLETED, List.of()),
            Map.entry(BookingStatus.CANCELLED, List.of()),
            Map.entry(BookingStatus.DISPUTED, List.of())
    );

    private final IBookingRepository bookingRepository;
    private final IBookingAuditLogRepository auditLogRepository;
    private final IServiceRepository serviceRepository;
    private final IProjectRepository projectRepository;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AvailabilityService availabilityService;
    private final MlPredictionService mlPredictionService;
    private final IServicePackageRepository packageRepository;

    public BookingService(IBookingRepository bookingRepository, IBookingAuditLogRepository auditLogRepository,
                          IServiceRepository serviceRepository, IProjectRepository projectRepository,
                          BookingMapper bookingMapper,
                          ApplicationEventPublisher eventPublisher, AvailabilityService availabilityService,
                          MlPredictionService mlPredictionService, IServicePackageRepository packageRepository) {
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
        this.serviceRepository = serviceRepository;
        this.projectRepository = projectRepository;
        this.bookingMapper = bookingMapper;
        this.eventPublisher = eventPublisher;
        this.availabilityService = availabilityService;
        this.mlPredictionService = mlPredictionService;
        this.packageRepository = packageRepository;
    }

    public PageResponse<BookingResponse> getAll(int page, int size) {
        Page<Booking> result = bookingRepository.findAll(PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public Optional<BookingResponse> getById(Long id) {
        return bookingRepository.findById(id)
                .filter(b -> b.getDeletedAt() == null)
                .map(bookingMapper::toResponse);
    }

    public Booking findEntityById(Long id) {
        return bookingRepository.findById(id)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    public PageResponse<BookingResponse> getByUserId(Long userId, int page, int size) {
        Page<Booking> result = bookingRepository.findByUserId(userId, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<BookingResponse> getByProviderId(Long providerId, int page, int size) {
        Page<Booking> result = bookingRepository.findActiveByProviderId(providerId, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<BookingResponse> getByProviderIdAndStatus(Long providerId, BookingStatus status, int page, int size) {
        Page<Booking> result = bookingRepository.findByProviderIdAndStatus(providerId, status, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<BookingResponse> getByProjectId(Long projectId, int page, int size) {
        Page<Booking> result = bookingRepository.findByProjectId(projectId, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    @Transactional
    public BookingResponse create(BookingRequest request, Long userId) {
        Booking booking = bookingMapper.toEntity(request);

        User user = new User();
        user.setId(userId);
        booking.setUser(user);

        var serviceId = request.getServiceId();
        if (serviceId != null) {
            Service svc = serviceRepository.findById(serviceId)
                    .filter(s -> s.getDeletedAt() == null)
                    .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
            booking.setService(svc);

            if (svc.getProvider() != null) {
                booking.setProvider(svc.getProvider());
            }

            BigDecimal unitPrice = svc.getPrice();

            if (request.getPackageId() != null) {
                ServicePackage pkg = packageRepository.findById(request.getPackageId())
                        .filter(p -> p.getService().getId().equals(serviceId))
                        .orElseThrow(() -> new BadRequestException("Invalid package for this service"));
                unitPrice = pkg.getPrice();
                if (svc.getPricingType() == PricingType.PACKAGED) {
                    booking.setDuration(0);
                }
            }

            if (unitPrice != null && request.getDuration() > 0) {
                BigDecimal price = svc.getPricingType() == PricingType.HOURLY
                        ? unitPrice.multiply(BigDecimal.valueOf(request.getDuration())).setScale(2, java.math.RoundingMode.HALF_UP)
                        : unitPrice;

                if (request.isHighPriority()) {
                    BigDecimal markup = price.multiply(BigDecimal.valueOf(0.12)).setScale(2, java.math.RoundingMode.HALF_UP);
                    booking.setPriorityMarkup(markup);
                    price = price.add(markup);
                }

                booking.setTotalPrice(price);
            } else if (unitPrice != null && request.getDuration() == 0) {
                booking.setTotalPrice(unitPrice);
                if (request.isHighPriority()) {
                    BigDecimal markup = unitPrice.multiply(BigDecimal.valueOf(0.12)).setScale(2, java.math.RoundingMode.HALF_UP);
                    booking.setPriorityMarkup(markup);
                    booking.setTotalPrice(unitPrice.add(markup));
                }
            }

            if (booking.getDate() != null && booking.getDuration() > 0 && svc.getPricingType() != PricingType.PACKAGED) {
                availabilityService.validateBookingAvailability(serviceId, booking.getDate(), booking.getDuration());
            }
        }

        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .filter(p -> p.getDeletedAt() == null)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));
            booking.setProject(project);
        }

        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);

        logTransition(saved.getId(), null, BookingStatus.PENDING.name(), userId);

        eventPublisher.publishEvent(new StatusTransitionEvent(saved.getId(), "Booking", null, BookingStatus.PENDING.name(), userId));

        return bookingMapper.toResponse(saved);
    }

    @Transactional
    public BookingResponse updateStatus(Long bookingId, BookingStatus newStatus, Long changedBy) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        BookingStatus currentStatus = booking.getStatus();
        validateTransition(currentStatus, newStatus);

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);

        logTransition(bookingId, currentStatus.name(), newStatus.name(), changedBy);

        eventPublisher.publishEvent(new StatusTransitionEvent(bookingId, "Booking", currentStatus.name(), newStatus.name(), changedBy));

        publishNotification(booking, newStatus);

        if (newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.DISPUTED) {
            mlPredictionService.recordBookingOutcome(saved);
        }

        return bookingMapper.toResponse(saved);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, Long userId) {
        return updateStatus(bookingId, BookingStatus.CANCELLED, userId);
    }

    public List<BookingAuditLog> getAuditLog(Long bookingId) {
        return auditLogRepository.findByBookingIdOrderByChangedAtDesc(bookingId);
    }

    private void validateTransition(BookingStatus from, BookingStatus to) {
        List<BookingStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BadRequestException("Cannot transition booking from " + from + " to " + to);
        }
    }

    private void logTransition(Long bookingId, String fromStatus, String toStatus, Long changedBy) {
        BookingAuditLog log = new BookingAuditLog();
        log.setBookingId(bookingId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setChangedBy(changedBy);
        auditLogRepository.save(log);
    }

    private void publishNotification(Booking booking, BookingStatus newStatus) {
        String title;
        String message;
        String svcName = booking.getService() != null ? booking.getService().getName() : "a service";

        switch (newStatus) {
            case PENDING_EVALUATION -> {
                title = "Booking Under Review";
                message = "Your booking for " + svcName + " is being evaluated.";
                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getUser().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            case TENTATIVE -> {
                title = "Booking Tentatively Accepted";
                message = "Your booking for " + svcName + " has been tentatively accepted.";
                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getUser().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            case APPROVED -> {
                title = "Booking Approved";
                message = "Your booking for " + svcName + " has been approved.";
                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getUser().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            case CONFIRMED -> {
                title = "Booking Confirmed";
                message = "Your booking for " + svcName + " has been confirmed.";
                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getUser().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            case REJECTED -> {
                title = "Booking Rejected";
                message = "Your booking for " + svcName + " has been rejected.";
                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getUser().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            case COMPLETED -> {
                title = "Booking Completed";
                message = "Your booking has been completed. Please leave a review!";
                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getUser().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            case PENDING_REVIEW -> {
                title = "Deliverable Pending Review";
                message = "A deliverable has been submitted for your booking for " + svcName + ". Please review it.";
                if (booking.getUser() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getUser().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            case DISPUTED -> {
                title = "Booking Disputed";
                message = "The deliverable for booking for " + svcName + " has been rejected. The booking is now disputed.";
                if (booking.getProvider() != null) {
                    eventPublisher.publishEvent(new NotificationEvent(booking.getProvider().getId(), "BOOKING_STATUS", title, message, booking.getId(), "Booking"));
                }
            }
            default -> {}
        }
    }

    private PageResponse<BookingResponse> toPageResponse(Page<Booking> page) {
        List<BookingResponse> content = page.getContent().stream()
                .map(bookingMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
