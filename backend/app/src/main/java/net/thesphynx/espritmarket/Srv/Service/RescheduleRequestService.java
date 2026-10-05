package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Event.NotificationEvent;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.RescheduleDecisionDto;
import net.thesphynx.espritmarket.Srv.Dto.RescheduleRequestDto;
import net.thesphynx.espritmarket.Srv.Dto.RescheduleResponse;
import net.thesphynx.espritmarket.Srv.Entity.*;
import net.thesphynx.espritmarket.Srv.Mapper.RescheduleRequestMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingAuditLogRepository;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IRescheduleRequestRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class RescheduleRequestService {
    private static final Set<BookingStatus> RESCHEDULABLE_STATUSES = Set.of(
            BookingStatus.PENDING, BookingStatus.PENDING_EVALUATION, BookingStatus.TENTATIVE,
            BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS
    );

    private final IRescheduleRequestRepository rescheduleRepository;
    private final IBookingRepository bookingRepository;
    private final IBookingAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;
    private final RescheduleRequestMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public RescheduleRequestService(IRescheduleRequestRepository rescheduleRepository,
                                     IBookingRepository bookingRepository,
                                     IBookingAuditLogRepository auditLogRepository,
                                     UserRepository userRepository,
                                     AvailabilityService availabilityService,
                                     RescheduleRequestMapper mapper,
                                     ApplicationEventPublisher eventPublisher) {
        this.rescheduleRepository = rescheduleRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RescheduleResponse createRequest(Long bookingId, RescheduleRequestDto dto, Long requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!RESCHEDULABLE_STATUSES.contains(booking.getStatus())) {
            throw new BadRequestException("Cannot reschedule a booking with status " + booking.getStatus());
        }

        if (rescheduleRepository.existsByBookingAndStatus(booking, RescheduleStatus.PENDING)) {
            throw new BadRequestException("A pending reschedule request already exists for this booking");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        boolean isProvider = booking.getProvider() != null && booking.getProvider().getId().equals(requesterId);
        boolean isClient = booking.getUser() != null && booking.getUser().getId().equals(requesterId);
        if (!isProvider && !isClient) {
            throw new BadRequestException("Only the booking provider or client can request a reschedule");
        }

        availabilityService.validateBookingAvailability(
                booking.getService().getId(), dto.getProposedDate(), dto.getProposedDuration());

        RescheduleReason reason;
        try {
            reason = dto.getReason() != null ? RescheduleReason.valueOf(dto.getReason()) : RescheduleReason.OTHER;
        } catch (IllegalArgumentException e) {
            reason = RescheduleReason.OTHER;
        }

        RescheduleRequest request = new RescheduleRequest();
        request.setBooking(booking);
        request.setRequestedBy(requester);
        request.setOriginalDate(booking.getDate());
        request.setOriginalDuration(booking.getDuration());
        request.setProposedDate(dto.getProposedDate());
        request.setProposedDuration(dto.getProposedDuration());
        request.setReason(reason);
        request.setMessage(dto.getMessage());
        request.setStatus(RescheduleStatus.PENDING);

        RescheduleRequest saved = rescheduleRepository.save(request);

        Long notifyUserId = isProvider ? booking.getUser().getId() : booking.getProvider().getId();
        if (notifyUserId != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                    notifyUserId, "RESCHEDULE",
                    "Reschedule Request",
                    "A reschedule has been requested for booking #" + bookingId,
                    bookingId, "Booking"
            ));
        }

        return mapper.toResponse(saved);
    }

    @Transactional
    public RescheduleResponse acceptRequest(Long requestId, RescheduleDecisionDto dto, Long responderId) {
        RescheduleRequest request = rescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RescheduleRequest", requestId));

        if (request.getStatus() != RescheduleStatus.PENDING) {
            throw new BadRequestException("Only pending reschedule requests can be accepted");
        }

        Booking booking = request.getBooking();
        boolean isProvider = booking.getProvider() != null && booking.getProvider().getId().equals(responderId);
        boolean isClient = booking.getUser() != null && booking.getUser().getId().equals(responderId);
        Long requesterId = request.getRequestedBy().getId();

        if ((isProvider && requesterId.equals(booking.getProvider().getId())) ||
                (isClient && requesterId.equals(booking.getUser().getId()))) {
        } else {
            boolean isOtherParty = (isProvider && requesterId.equals(booking.getUser().getId())) ||
                    (isClient && requesterId.equals(booking.getProvider().getId()));
            if (!isOtherParty) {
                throw new BadRequestException("Only the other party in this booking can respond to the reschedule request");
            }
        }

        availabilityService.validateBookingAvailability(
                booking.getService().getId(), request.getProposedDate(), request.getProposedDuration());

        String oldDate = booking.getDate().toString();
        booking.setDate(request.getProposedDate());
        booking.setDuration(request.getProposedDuration());
        bookingRepository.save(booking);

        User responder = userRepository.findById(responderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", responderId));
        request.setStatus(RescheduleStatus.ACCEPTED);
        request.setRespondedBy(responder);
        request.setRespondedAt(LocalDateTime.now());
        request.setResponseMessage(dto != null ? dto.getResponseMessage() : null);

        RescheduleRequest saved = rescheduleRepository.save(request);

        BookingAuditLog auditLog = new BookingAuditLog();
        auditLog.setBookingId(booking.getId());
        auditLog.setFromStatus("RESCHEDULE_REQUESTED:" + oldDate);
        auditLog.setToStatus("RESCHEDULE_ACCEPTED:" + request.getProposedDate());
        auditLog.setChangedBy(responderId);
        auditLogRepository.save(auditLog);

        eventPublisher.publishEvent(new NotificationEvent(
                requesterId, "RESCHEDULE",
                "Reschedule Accepted",
                "Your reschedule request for booking #" + booking.getId() + " has been accepted",
                booking.getId(), "Booking"
        ));

        return mapper.toResponse(saved);
    }

    @Transactional
    public RescheduleResponse rejectRequest(Long requestId, RescheduleDecisionDto dto, Long responderId) {
        RescheduleRequest request = rescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RescheduleRequest", requestId));

        if (request.getStatus() != RescheduleStatus.PENDING) {
            throw new BadRequestException("Only pending reschedule requests can be rejected");
        }

        User responder = userRepository.findById(responderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", responderId));
        request.setStatus(RescheduleStatus.REJECTED);
        request.setRespondedBy(responder);
        request.setRespondedAt(LocalDateTime.now());
        request.setResponseMessage(dto != null ? dto.getResponseMessage() : null);

        RescheduleRequest saved = rescheduleRepository.save(request);

        eventPublisher.publishEvent(new NotificationEvent(
                request.getRequestedBy().getId(), "RESCHEDULE",
                "Reschedule Rejected",
                "Your reschedule request for booking #" + request.getBooking().getId() + " has been rejected",
                request.getBooking().getId(), "Booking"
        ));

        return mapper.toResponse(saved);
    }

    @Transactional
    public RescheduleResponse cancelRequest(Long requestId, Long requesterId) {
        RescheduleRequest request = rescheduleRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RescheduleRequest", requestId));

        if (request.getStatus() != RescheduleStatus.PENDING) {
            throw new BadRequestException("Only pending reschedule requests can be cancelled");
        }

        if (!request.getRequestedBy().getId().equals(requesterId)) {
            throw new BadRequestException("Only the requester can cancel a reschedule request");
        }

        request.setStatus(RescheduleStatus.CANCELLED);
        return mapper.toResponse(rescheduleRepository.save(request));
    }

    public RescheduleResponse getActiveRequest(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        return rescheduleRepository.findByBookingAndStatus(booking, RescheduleStatus.PENDING)
                .map(mapper::toResponse)
                .orElse(null);
    }

    public List<RescheduleResponse> getHistory(Long bookingId) {
        return rescheduleRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
