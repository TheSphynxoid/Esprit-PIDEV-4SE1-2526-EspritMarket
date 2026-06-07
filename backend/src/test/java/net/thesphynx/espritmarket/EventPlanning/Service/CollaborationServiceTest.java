package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Entity.Collaboration;
import net.thesphynx.espritmarket.EventPlanning.Repository.ICollaborationRepository;
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
class CollaborationServiceTest {

    @Mock
    private ICollaborationRepository collaborationRepository;

    @InjectMocks
    private CollaborationService service;

    @Test
    void getAll_shouldReturnAllItems() {
        var expected = List.of(new Collaboration(), new Collaboration());
        when(collaborationRepository.findAll()).thenReturn(expected);

        var result = service.getAll();

        assertEquals(expected, result);
        verify(collaborationRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnItem() {
        var id = 1L;
        var entity = new Collaboration();
        when(collaborationRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(collaborationRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(collaborationRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(collaborationRepository).findById(id);
    }

    @Test
    void create_shouldPersistItem() {
        var entity = new Collaboration();
        when(collaborationRepository.save(entity)).thenReturn(entity);

        var result = service.create(entity);

        assertEquals(entity, result);
        verify(collaborationRepository).save(entity);
    }

    @Test
    void update_shouldSetIdAndPersistItem() {
        var id = 22L;
        var entity = new Collaboration();
        entity.setId(999L);
        when(collaborationRepository.save(entity)).thenReturn(entity);

        var result = service.update(id, entity);

        assertEquals(id, entity.getId());
        assertEquals(entity, result);
        verify(collaborationRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 77L;

        service.delete(id);

        verify(collaborationRepository).deleteById(id);
    }
}
