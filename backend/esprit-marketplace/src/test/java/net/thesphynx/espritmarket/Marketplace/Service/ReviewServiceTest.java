package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.ReviewRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ReviewResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Review;
import net.thesphynx.espritmarket.Marketplace.Mapper.ReviewMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private IReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService service;

    @Test
    void getAll_shouldReturnMappedItems() {
        var e1 = new Review();
        var e2 = new Review();
        var r1 = new ReviewResponse();
        var r2 = new ReviewResponse();

        when(reviewRepository.findAll()).thenReturn(List.of(e1, e2));
        when(reviewMapper.toResponse(e1)).thenReturn(r1);
        when(reviewMapper.toResponse(e2)).thenReturn(r2);

        var result = service.getAll();

        assertEquals(2, result.size());
        assertEquals(r1, result.get(0));
        assertEquals(r2, result.get(1));
        verify(reviewRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnMappedItem() {
        var id = 1L;
        var entity = new Review();
        var response = new ReviewResponse();
        when(reviewRepository.findById(id)).thenReturn(Optional.of(entity));
        when(reviewMapper.toResponse(entity)).thenReturn(response);

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(response, result.get());
        verify(reviewRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(reviewRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(reviewRepository).findById(id);
    }

    @Test
    void create_shouldMapPersistAndReturnResponse() {
        var request = new ReviewRequest();
        var entity = new Review();
        var response = new ReviewResponse();

        when(reviewMapper.toEntity(request)).thenReturn(entity);
        when(reviewRepository.save(entity)).thenReturn(entity);
        when(reviewMapper.toResponse(entity)).thenReturn(response);

        var result = service.create(request);

        assertEquals(response, result);
        verify(reviewMapper).toEntity(request);
        verify(reviewRepository).save(entity);
        verify(reviewMapper).toResponse(entity);
    }

    @Test
    void update_shouldSetIdPersistAndReturnResponse() {
        var id = 9L;
        var request = new ReviewRequest();
        var entity = new Review();
        var response = new ReviewResponse();

        when(reviewMapper.toEntity(request)).thenReturn(entity);
        when(reviewRepository.save(entity)).thenReturn(entity);
        when(reviewMapper.toResponse(entity)).thenReturn(response);

        var result = service.update(id, request);

        assertEquals(id, entity.getId());
        assertEquals(response, result);
        verify(reviewRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 12L;

        service.delete(id);

        verify(reviewRepository).deleteById(id);
    }
}
