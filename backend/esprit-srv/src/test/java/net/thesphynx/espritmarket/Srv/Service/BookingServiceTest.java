package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Dto.BookingRequest;
import net.thesphynx.espritmarket.Srv.Dto.BookingResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.PricingType;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Mapper.BookingMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingAuditLogRepository;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import net.thesphynx.espritmarket.Common.Entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private IBookingRepository bookingRepository;

    @Mock
    private IBookingAuditLogRepository auditLogRepository;

    @Mock
    private IServiceRepository serviceRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private MlPredictionService mlPredictionService;

    @Mock
    private IProjectRepository projectRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void create_shouldSetPendingAndSaveAndCalculateTotal() {
        var request = new BookingRequest();
        request.setServiceId(1L);
        request.setDuration(2);
        var entity = new Booking();
        var response = new BookingResponse();

        var svc = new Service();
        svc.setId(1L);
        svc.setPrice(java.math.BigDecimal.valueOf(50));
        svc.setPricingType(PricingType.HOURLY);
        var provider = new User();
        provider.setId(99L);
        svc.setProvider(provider);

        when(bookingMapper.toEntity(request)).thenReturn(entity);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(svc));
        when(bookingRepository.save(entity)).thenReturn(entity);
        when(bookingMapper.toResponse(entity)).thenReturn(response);

        var result = bookingService.create(request, 10L);

        assertEquals(response, result);
        assertEquals(BookingStatus.PENDING, entity.getStatus());
        assertNotNull(entity.getUser());
        assertEquals(10L, entity.getUser().getId());
        assertEquals(99L, entity.getProvider().getId());
        assertEquals(0, java.math.BigDecimal.valueOf(100).compareTo(entity.getTotalPrice()));
        verify(auditLogRepository).save(any());
    }

    @Test
    void updateStatus_pendingToApproved_shouldSucceed() {
        var booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        var response = new BookingResponse();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        var result = bookingService.updateStatus(1L, BookingStatus.APPROVED, 5L);

        assertEquals(BookingStatus.APPROVED, booking.getStatus());
        assertEquals(response, result);
    }

    @Test
    void updateStatus_invalidTransition_shouldThrow() {
        var booking = new Booking();
        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () ->
                bookingService.updateStatus(1L, BookingStatus.APPROVED, 5L));
    }

    @Test
    void updateStatus_notFound_shouldThrow() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                bookingService.updateStatus(999L, BookingStatus.APPROVED, 5L));
    }

    @Test
    void cancel_shouldTransitionToCancelled() {
        var booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        var response = new BookingResponse();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        bookingService.cancel(1L, 10L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void getByUserId_shouldReturnPage() {
        var booking = new Booking();
        var response = new BookingResponse();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findByUserId(1L, PageRequest.of(0, 20))).thenReturn(page);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        var result = bookingService.getByUserId(1L, 0, 20);

        assertEquals(1, result.getContent().size());
    }
}
