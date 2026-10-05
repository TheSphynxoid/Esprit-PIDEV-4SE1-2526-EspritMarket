package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.ServiceReview;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceReviewMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceReviewRepository;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceReviewServiceTest {

    @Mock
    private IServiceReviewRepository serviceReviewRepository;

    @Mock
    private IBookingRepository bookingRepository;

    @Mock
    private ServiceReviewMapper serviceReviewMapper;

    @InjectMocks
    private ServiceReviewService service;

    @Test
    void create_whenBookingCompleted_shouldSucceed() {
        var request = new ServiceReviewRequest();
        request.setBookingId(1L);
        request.setRating(BigDecimal.valueOf(4.5));

        var booking = new Booking();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUser(new User());
        booking.getUser().setId(10L);

        var review = new ServiceReview();
        var response = new ServiceReviewResponse();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(serviceReviewMapper.toEntity(request)).thenReturn(review);
        when(serviceReviewRepository.save(review)).thenReturn(review);
        when(serviceReviewMapper.toResponse(review)).thenReturn(response);

        var result = service.create(request, 10L);

        assertEquals(response, result);
    }

    @Test
    void create_whenBookingNotCompleted_shouldThrow() {
        var request = new ServiceReviewRequest();
        request.setBookingId(1L);

        var booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setUser(new User());
        booking.getUser().setId(10L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> service.create(request, 10L));
    }

    @Test
    void create_whenNotBookingOwner_shouldThrow() {
        var request = new ServiceReviewRequest();
        request.setBookingId(1L);

        var booking = new Booking();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUser(new User());
        booking.getUser().setId(10L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> service.create(request, 99L));
    }

    @Test
    void create_whenBookingNotFound_shouldThrow() {
        var request = new ServiceReviewRequest();
        request.setBookingId(999L);

        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(request, 10L));
    }
}
