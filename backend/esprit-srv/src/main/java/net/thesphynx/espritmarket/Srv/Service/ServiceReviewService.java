package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.ServiceReview;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceReviewMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceReviewRepository;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServiceReviewService {
    private final IServiceReviewRepository serviceReviewRepository;
    private final IBookingRepository bookingRepository;
    private final ServiceReviewMapper serviceReviewMapper;
    private final MlPredictionService mlPredictionService;

    public ServiceReviewService(IServiceReviewRepository serviceReviewRepository,
                                IBookingRepository bookingRepository,
                                ServiceReviewMapper serviceReviewMapper,
                                MlPredictionService mlPredictionService) {
        this.serviceReviewRepository = serviceReviewRepository;
        this.bookingRepository = bookingRepository;
        this.serviceReviewMapper = serviceReviewMapper;
        this.mlPredictionService = mlPredictionService;
    }

    public PageResponse<ServiceReviewResponse> getByServiceId(Long serviceId, int page, int size) {
        Page<ServiceReview> result = serviceReviewRepository.findByServiceId(serviceId, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public PageResponse<ServiceReviewResponse> getByProviderIdAndBookingStatus(Long providerId,
                                                                                BookingStatus status,
                                                                                int page,
                                                                                int size) {
        Page<ServiceReview> result = serviceReviewRepository
                .findByBooking_Service_Provider_IdAndBooking_Status(providerId, status, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public Optional<ServiceReviewResponse> getById(Long id) {
        return serviceReviewRepository.findById(id)
                .map(serviceReviewMapper::toResponse);
    }

    @Transactional
    public ServiceReviewResponse create(ServiceReviewRequest request, Long userId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId()));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Can only review completed bookings");
        }
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Only the booking owner can leave a review");
        }

        ServiceReview review = serviceReviewMapper.toEntity(request);

        User user = new User();
        user.setId(userId);
        review.setUser(user);

        if (request.getComment() != null && !request.getComment().trim().isEmpty()) {
            List<Map<String, Object>> sentimentResults = mlPredictionService.analyzeSentiment(Collections.singletonList(request.getComment()));
            if (sentimentResults != null && !sentimentResults.isEmpty()) {
                Map<String, Object> sentiment = sentimentResults.get(0);
                review.setSentiment((String) sentiment.get("sentiment"));
                review.setSentimentConfidence((Double) sentiment.get("confidence"));
            }
        }

        ServiceReview saved = serviceReviewRepository.save(review);
        return serviceReviewMapper.toResponse(saved);
    }

    @Transactional
    public ServiceReviewResponse update(Long id, ServiceReviewRequest request) {
        ServiceReview review = serviceReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceReview", id));
        review.setComment(request.getComment());
        review.setRating(request.getRating());

        if (request.getComment() != null && !request.getComment().trim().isEmpty()) {
            List<Map<String, Object>> sentimentResults = mlPredictionService.analyzeSentiment(Collections.singletonList(request.getComment()));
            if (sentimentResults != null && !sentimentResults.isEmpty()) {
                Map<String, Object> sentiment = sentimentResults.get(0);
                review.setSentiment((String) sentiment.get("sentiment"));
                review.setSentimentConfidence((Double) sentiment.get("confidence"));
            }
        }

        return serviceReviewMapper.toResponse(serviceReviewRepository.save(review));
    }

    @Transactional
    public void delete(Long id) {
        serviceReviewRepository.deleteById(id);
    }

    private PageResponse<ServiceReviewResponse> toPageResponse(Page<ServiceReview> page) {
        List<ServiceReviewResponse> content = page.getContent().stream()
                .map(serviceReviewMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
