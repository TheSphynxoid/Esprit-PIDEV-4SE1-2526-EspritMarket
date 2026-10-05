package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Entity.EquipmentReservation;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentReservationRepository;
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
class EquipmentReservationServiceTest {

    @Mock
    private IEquipmentReservationRepository equipmentReservationRepository;

    @InjectMocks
    private EquipmentReservationService service;

    @Test
    void getAll_shouldReturnAllItems() {
        var expected = List.of(new EquipmentReservation(), new EquipmentReservation());
        when(equipmentReservationRepository.findAll()).thenReturn(expected);

        var result = service.getAll();

        assertEquals(expected, result);
        verify(equipmentReservationRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnItem() {
        var id = 1L;
        var entity = new EquipmentReservation();
        when(equipmentReservationRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(equipmentReservationRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(equipmentReservationRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(equipmentReservationRepository).findById(id);
    }

    @Test
    void create_shouldPersistItem() {
        var entity = new EquipmentReservation();
        when(equipmentReservationRepository.save(entity)).thenReturn(entity);

        var result = service.create(entity);

        assertEquals(entity, result);
        verify(equipmentReservationRepository).save(entity);
    }

    @Test
    void update_shouldSetIdAndPersistItem() {
        var id = 22L;
        var entity = new EquipmentReservation();
        entity.setId(999L);
        when(equipmentReservationRepository.save(entity)).thenReturn(entity);

        var result = service.update(id, entity);

        assertEquals(id, entity.getId());
        assertEquals(entity, result);
        verify(equipmentReservationRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 77L;

        service.delete(id);

        verify(equipmentReservationRepository).deleteById(id);
    }
}
