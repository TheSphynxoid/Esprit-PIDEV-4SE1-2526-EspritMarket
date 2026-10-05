package net.thesphynx.espritmarket.Marketplace.Mapper;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Marketplace.Dto.ReviewRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ReviewResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import net.thesphynx.espritmarket.Marketplace.Entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewRequest request) {
        if (request == null) return null;
        Review review = new Review();
        review.setComment(request.getComment());
        review.setRating(request.getRating());
        if (request.getProductId() != null) {
            Product product = new Product();
            product.setId(request.getProductId());
            review.setProduct(product);
        }
        if (request.getUserId() != null) {
            User user = new User();
            user.setId(request.getUserId());
            review.setUser(user);
        }
        return review;
    }

    public ReviewResponse toResponse(Review review) {
        if (review == null) return null;
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setComment(review.getComment());
        response.setRating(review.getRating());
        if (review.getProduct() != null) {
            response.setProductId(review.getProduct().getId());
            response.setProductName(review.getProduct().getName());
        }
        if (review.getUser() != null) {
            response.setUserId(review.getUser().getId());
        }
        return response;
    }
}
