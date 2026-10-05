package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.CategoryRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.CategoryResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Mapper.CategoryMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ICategoryRepository;
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
class CategoryServiceTest {

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService service;

    @Test
    void getAll_shouldReturnMappedItems() {
        var e1 = new Category();
        var e2 = new Category();
        var r1 = new CategoryResponse();
        var r2 = new CategoryResponse();

        when(categoryRepository.findAll()).thenReturn(List.of(e1, e2));
        when(categoryMapper.toResponse(e1)).thenReturn(r1);
        when(categoryMapper.toResponse(e2)).thenReturn(r2);

        var result = service.getAll();

        assertEquals(2, result.size());
        assertEquals(r1, result.get(0));
        assertEquals(r2, result.get(1));
        verify(categoryRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnMappedItem() {
        var id = 1L;
        var entity = new Category();
        var response = new CategoryResponse();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(response, result.get());
        verify(categoryRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(categoryRepository).findById(id);
    }

    @Test
    void create_shouldMapPersistAndReturnResponse() {
        var request = new CategoryRequest();
        var entity = new Category();
        var response = new CategoryResponse();

        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        var result = service.create(request);

        assertEquals(response, result);
        verify(categoryMapper).toEntity(request);
        verify(categoryRepository).save(entity);
        verify(categoryMapper).toResponse(entity);
    }

    @Test
    void update_shouldSetIdPersistAndReturnResponse() {
        var id = 9L;
        var request = new CategoryRequest();
        var entity = new Category();
        var response = new CategoryResponse();

        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        var result = service.update(id, request);

        assertEquals(id, entity.getId());
        assertEquals(response, result);
        verify(categoryRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 12L;

        service.delete(id);

        verify(categoryRepository).deleteById(id);
    }
}
