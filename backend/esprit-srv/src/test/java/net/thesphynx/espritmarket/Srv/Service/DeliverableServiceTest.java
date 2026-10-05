package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.DeliverableCreateRequest;
import net.thesphynx.espritmarket.Srv.Dto.DeliverableResponse;
import net.thesphynx.espritmarket.Srv.Dto.DeliverableReviewRequest;
import net.thesphynx.espritmarket.Srv.Dto.DeliverableReviewResponse;
import net.thesphynx.espritmarket.Srv.Entity.*;
import net.thesphynx.espritmarket.Srv.Mapper.DeliverableMapper;
import net.thesphynx.espritmarket.Srv.Repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverableServiceTest {

    @Mock
    private IDeliverableRepository deliverableRepository;

    @Mock
    private IDeliverableAttachmentRepository attachmentRepository;

    @Mock
    private IDeliverableReviewRepository reviewRepository;

    @Mock
    private IBookingRepository bookingRepository;

    @Mock
    private IBookingAuditLogRepository auditLogRepository;

    @Mock
    private IDeliverableVersionRepository deliverableVersionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DeliverableMapper deliverableMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeliverableService service;

    @Test
    void create_whenBookingInProgress_shouldCreateDeliverable() {
        var bookingId = 1L;
        var providerId = 2L;

        User provider = new User();
        provider.setId(providerId);
        provider.setName("Provider");

        User client = new User();
        client.setId(3L);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setProvider(provider);
        booking.setUser(client);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));

        Deliverable saved = new Deliverable();
        saved.setId(10L);
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(saved);

        DeliverableResponse expected = new DeliverableResponse();
        expected.setId(10L);
        when(deliverableMapper.toResponse(any(Deliverable.class))).thenReturn(expected);

        var request = new DeliverableCreateRequest();
        request.setTitle("Test Deliverable");
        request.setDescription("Description");

        var result = service.create(bookingId, request, providerId);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(deliverableRepository).save(any(Deliverable.class));
    }

    @Test
    void create_whenNotProvider_shouldThrow() {
        var bookingId = 1L;
        var wrongProviderId = 99L;

        User provider = new User();
        provider.setId(2L);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setProvider(provider);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        var request = new DeliverableCreateRequest();
        request.setTitle("Test");

        assertThrows(BadRequestException.class, () -> service.create(bookingId, request, wrongProviderId));
    }

    @Test
    void create_whenBookingNotInProgress_shouldThrow() {
        var bookingId = 1L;
        var providerId = 2L;

        User provider = new User();
        provider.setId(providerId);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setProvider(provider);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        var request = new DeliverableCreateRequest();
        request.setTitle("Test");

        assertThrows(BadRequestException.class, () -> service.create(bookingId, request, providerId));
    }

    @Test
    void submit_whenDraft_shouldSetStatusToSubmittedAndBookingToPendingReview() {
        var deliverableId = 10L;
        var providerId = 2L;

        User provider = new User();
        provider.setId(providerId);

        User client = new User();
        client.setId(3L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setProvider(provider);
        booking.setUser(client);

        Deliverable deliverable = new Deliverable();
        deliverable.setId(deliverableId);
        deliverable.setStatus(DeliverableStatus.DRAFT);
        deliverable.setVersion(1);
        deliverable.setProvider(provider);
        deliverable.setBooking(booking);

        when(deliverableRepository.findById(deliverableId)).thenReturn(Optional.of(deliverable));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);

        DeliverableResponse expected = new DeliverableResponse();
        expected.setId(deliverableId);
        when(deliverableMapper.toResponse(any(Deliverable.class))).thenReturn(expected);

        var result = service.submit(deliverableId, providerId);

        assertNotNull(result);
        verify(auditLogRepository).save(any(BookingAuditLog.class));
    }

    @Test
    void submit_whenRevisionRequested_shouldIncrementVersion() {
        var deliverableId = 10L;
        var providerId = 2L;

        User provider = new User();
        provider.setId(providerId);

        User client = new User();
        client.setId(3L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setProvider(provider);
        booking.setUser(client);

        Deliverable deliverable = new Deliverable();
        deliverable.setId(deliverableId);
        deliverable.setStatus(DeliverableStatus.REVISION_REQUESTED);
        deliverable.setVersion(2);
        deliverable.setProvider(provider);
        deliverable.setBooking(booking);

        when(deliverableRepository.findById(deliverableId)).thenReturn(Optional.of(deliverable));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);

        DeliverableResponse expected = new DeliverableResponse();
        when(deliverableMapper.toResponse(any(Deliverable.class))).thenReturn(expected);

        service.submit(deliverableId, providerId);

        verify(deliverableRepository).save(any(Deliverable.class));
    }

    @Test
    void review_whenAccepted_shouldSetBookingToCompleted() {
        var deliverableId = 10L;
        var clientId = 3L;

        User provider = new User();
        provider.setId(2L);
        provider.setName("Provider");

        User client = new User();
        client.setId(clientId);
        client.setName("Client");

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING_REVIEW);
        booking.setProvider(provider);
        booking.setUser(client);

        Deliverable deliverable = new Deliverable();
        deliverable.setId(deliverableId);
        deliverable.setStatus(DeliverableStatus.SUBMITTED);
        deliverable.setProvider(provider);
        deliverable.setBooking(booking);

        when(deliverableRepository.findById(deliverableId)).thenReturn(Optional.of(deliverable));
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(reviewRepository.save(any(DeliverableReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);

        DeliverableResponse expected = new DeliverableResponse();
        when(deliverableMapper.toResponse(any(Deliverable.class))).thenReturn(expected);

        var reviewRequest = new DeliverableReviewRequest();
        reviewRequest.setDecision(ReviewDecision.ACCEPTED);
        reviewRequest.setComment("Looks good!");

        var result = service.review(deliverableId, reviewRequest, clientId);

        assertNotNull(result);
        verify(auditLogRepository).save(any(BookingAuditLog.class));
    }

    @Test
    void review_whenRevisionRequested_shouldSetBookingToInProgress() {
        var deliverableId = 10L;
        var clientId = 3L;

        User provider = new User();
        provider.setId(2L);

        User client = new User();
        client.setId(clientId);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING_REVIEW);
        booking.setProvider(provider);
        booking.setUser(client);

        Deliverable deliverable = new Deliverable();
        deliverable.setId(deliverableId);
        deliverable.setStatus(DeliverableStatus.SUBMITTED);
        deliverable.setProvider(provider);
        deliverable.setBooking(booking);

        when(deliverableRepository.findById(deliverableId)).thenReturn(Optional.of(deliverable));
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(reviewRepository.save(any(DeliverableReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);

        DeliverableResponse expected = new DeliverableResponse();
        when(deliverableMapper.toResponse(any(Deliverable.class))).thenReturn(expected);

        var reviewRequest = new DeliverableReviewRequest();
        reviewRequest.setDecision(ReviewDecision.REVISION_REQUESTED);

        service.review(deliverableId, reviewRequest, clientId);

        verify(auditLogRepository).save(any(BookingAuditLog.class));
    }

    @Test
    void review_whenRejected_shouldSetBookingToDisputed() {
        var deliverableId = 10L;
        var clientId = 3L;

        User provider = new User();
        provider.setId(2L);
        provider.setName("Provider");

        User client = new User();
        client.setId(clientId);
        client.setName("Client");

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING_REVIEW);
        booking.setProvider(provider);
        booking.setUser(client);

        Deliverable deliverable = new Deliverable();
        deliverable.setId(deliverableId);
        deliverable.setStatus(DeliverableStatus.SUBMITTED);
        deliverable.setProvider(provider);
        deliverable.setBooking(booking);

        when(deliverableRepository.findById(deliverableId)).thenReturn(Optional.of(deliverable));
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(reviewRepository.save(any(DeliverableReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);

        DeliverableResponse expected = new DeliverableResponse();
        when(deliverableMapper.toResponse(any(Deliverable.class))).thenReturn(expected);

        var reviewRequest = new DeliverableReviewRequest();
        reviewRequest.setDecision(ReviewDecision.REJECTED);

        service.review(deliverableId, reviewRequest, clientId);

        verify(auditLogRepository).save(any(BookingAuditLog.class));
    }

    @Test
    void review_whenNotClient_shouldThrow() {
        var deliverableId = 10L;
        var wrongUserId = 99L;

        User provider = new User();
        provider.setId(2L);

        User client = new User();
        client.setId(3L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setProvider(provider);
        booking.setUser(client);

        Deliverable deliverable = new Deliverable();
        deliverable.setId(deliverableId);
        deliverable.setStatus(DeliverableStatus.SUBMITTED);
        deliverable.setProvider(provider);
        deliverable.setBooking(booking);

        when(deliverableRepository.findById(deliverableId)).thenReturn(Optional.of(deliverable));

        var reviewRequest = new DeliverableReviewRequest();
        reviewRequest.setDecision(ReviewDecision.ACCEPTED);

        assertThrows(BadRequestException.class, () -> service.review(deliverableId, reviewRequest, wrongUserId));
    }

    @Test
    void getById_whenFound_shouldReturnResponse() {
        var id = 10L;

        Deliverable deliverable = new Deliverable();
        deliverable.setId(id);
        when(deliverableRepository.findById(id)).thenReturn(Optional.of(deliverable));

        DeliverableResponse expected = new DeliverableResponse();
        expected.setId(id);
        when(deliverableMapper.toResponse(deliverable)).thenReturn(expected);

        var result = service.getById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void getById_whenNotFound_shouldThrow() {
        when(deliverableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(999L));
    }

    @Test
    void getByBookingId_shouldReturnListOfSummaries() {
        var bookingId = 1L;

        Deliverable d1 = new Deliverable();
        d1.setId(10L);
        when(deliverableRepository.findByBookingIdAndDeletedAtIsNullOrderByCreatedAtDesc(bookingId))
                .thenReturn(List.of(d1));

        DeliverableResponse expected = new DeliverableResponse();
        expected.setId(10L);
        when(deliverableMapper.toSummaryResponse(d1)).thenReturn(expected);

        var result = service.getByBookingId(bookingId);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void getHistory_shouldReturnReviewResponses() {
        var deliverableId = 10L;

        Deliverable deliverable = new Deliverable();
        deliverable.setId(deliverableId);
        when(deliverableRepository.findById(deliverableId)).thenReturn(Optional.of(deliverable));

        User reviewer = new User();
        reviewer.setId(3L);
        reviewer.setName("Client");

        DeliverableReview review = new DeliverableReview();
        review.setId(1L);
        review.setDeliverable(deliverable);
        review.setReviewer(reviewer);
        review.setDecision(ReviewDecision.ACCEPTED);
        review.setComment("Great work!");

        when(reviewRepository.findByDeliverableOrderByReviewedAtDesc(deliverable))
                .thenReturn(List.of(review));

        var result = service.getHistory(deliverableId);

        assertEquals(1, result.size());
        assertEquals(ReviewDecision.ACCEPTED, result.get(0).getDecision());
    }
}
