package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Entity.MapTracking;
import net.thesphynx.espritmarket.Delivery.Repository.IMapTrackingRepository;
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
class MapTrackingServiceTest {

    @Mock
    private IMapTrackingRepository mapTrackingRepository;

    @InjectMocks
    private MapTrackingService service;

    @Test
    void getAll_shouldReturnAllItems() {
        var expected = List.of(new MapTracking(), new MapTracking());
        when(mapTrackingRepository.findAll()).thenReturn(expected);

        var result = service.getAll();

        assertEquals(expected, result);
        verify(mapTrackingRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnItem() {
        var id = 1L;
        var entity = new MapTracking();
        when(mapTrackingRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(mapTrackingRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(mapTrackingRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(mapTrackingRepository).findById(id);
    }

    @Test
    void create_shouldPersistItem() {
        var entity = new MapTracking();
        when(mapTrackingRepository.save(entity)).thenReturn(entity);

        var result = service.create(entity);

        assertEquals(entity, result);
        verify(mapTrackingRepository).save(entity);
    }

    @Test
    void update_shouldSetIdAndPersistItem() {
        var id = 22L;
        var entity = new MapTracking();
        entity.setId(999L);
        when(mapTrackingRepository.save(entity)).thenReturn(entity);

        var result = service.update(id, entity);

        assertEquals(id, entity.getId());
        assertEquals(entity, result);
        verify(mapTrackingRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 77L;

        service.delete(id);

        verify(mapTrackingRepository).deleteById(id);
    }
}
