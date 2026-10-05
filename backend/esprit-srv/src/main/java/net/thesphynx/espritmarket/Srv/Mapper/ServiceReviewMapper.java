package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceReviewResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.ServiceReview;
import org.springframework.stereotype.Component;

@Component
public class ServiceReviewMapper {
    public ServiceReview toEntity(ServiceReviewRequest request) {
        if (request == null) return null;
        ServiceReview review = new ServiceReview();
        review.setComment(request.getComment());
        review.setRating(request.getRating());

        Booking booking = new Booking();
        booking.setId(request.getBookingId());
        review.setBooking(booking);

        return review;
    }

    public ServiceReviewResponse toResponse(ServiceReview review) {
        if (review == null) return null;
        ServiceReviewResponse response = new ServiceReviewResponse();
        response.setId(review.getId());
        response.setComment(review.getComment());
        response.setRating(review.getRating());
        if (review.getUser() != null) {
            response.setUserId(review.getUser().getId());
            response.setUserName(review.getUser().getName());
        }
        if (review.getBooking() != null) {
            response.setBookingId(review.getBooking().getId());
            if (review.getBooking().getService() != null) {
                response.setServiceId(review.getBooking().getService().getId());
            }
        }
        response.setSentiment(review.getSentiment());
        response.setSentimentConfidence(review.getSentimentConfidence());
        return response;
    }
}
