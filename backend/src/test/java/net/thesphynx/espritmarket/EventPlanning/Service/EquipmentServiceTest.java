package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentRepository;
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
class EquipmentServiceTest {

    @Mock
    private IEquipmentRepository equipmentRepository;

    @InjectMocks
    private EquipmentService service;

    @Test
    void getAll_shouldReturnAllItems() {
        var expected = List.of(new Equipment(), new Equipment());
        when(equipmentRepository.findAll()).thenReturn(expected);

        var result = service.getAll();

        assertEquals(expected, result);
        verify(equipmentRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnItem() {
        var id = 1L;
        var entity = new Equipment();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(equipmentRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(equipmentRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(equipmentRepository).findById(id);
    }

    @Test
    void create_shouldPersistItem() {
        var entity = new Equipment();
        when(equipmentRepository.save(entity)).thenReturn(entity);

        var result = service.create(entity);

        assertEquals(entity, result);
        verify(equipmentRepository).save(entity);
    }

    @Test
    void update_shouldSetIdAndPersistItem() {
        var id = 22L;
        var entity = new Equipment();
        entity.setId(999L);
        when(equipmentRepository.save(entity)).thenReturn(entity);

        var result = service.update(id, entity);

        assertEquals(id, entity.getId());
        assertEquals(entity, result);
        verify(equipmentRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 77L;

        service.delete(id);

        verify(equipmentRepository).deleteById(id);
    }
}
