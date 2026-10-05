package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.ReviewRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ReviewResponse;
import net.thesphynx.espritmarket.Marketplace.Mapper.ReviewMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private final IReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(IReviewRepository reviewRepository, ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
    }

    public List<ReviewResponse> getAll() {
        return reviewRepository.findAll()
                .stream()
                .map(reviewMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<ReviewResponse> getById(Long id) {
        return reviewRepository.findById(id)
                .map(reviewMapper::toResponse);
    }

    public ReviewResponse create(ReviewRequest request) {
        return reviewMapper.toResponse(
                reviewRepository.save(reviewMapper.toEntity(request))
        );
    }

    public ReviewResponse update(Long id, ReviewRequest request) {
        var entity = reviewMapper.toEntity(request);
        entity.setId(id);
        return reviewMapper.toResponse(reviewRepository.save(entity));
    }

    public void delete(Long id) {
        reviewRepository.deleteById(id);
    }
}
