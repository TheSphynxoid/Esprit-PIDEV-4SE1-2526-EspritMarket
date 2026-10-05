package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import net.thesphynx.espritmarket.Delivery.Repository.IVehiculeRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculeServiceTest {

    @Mock
    private IVehiculeRepository vehiculeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VehiculeService service;

    @Test
    void getAllVehicules_shouldReturnAllItems() {
        var expected = List.of(new Vehicule(), new Vehicule());
        when(vehiculeRepository.findAll()).thenReturn(expected);

        var result = service.getAllVehicules();

        assertEquals(expected, result);
        verify(vehiculeRepository).findAll();
    }

    @Test
    void getByIdForUser_whenFound_shouldReturnItem() {
        var id = 1L;
        var email = "courier@example.com";
        var entity = new Vehicule();
        when(vehiculeRepository.findByIdAndUserEmail(id, email)).thenReturn(Optional.of(entity));

        var result = service.getByIdForUser(id, email);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(vehiculeRepository).findByIdAndUserEmail(id, email);
    }

    @Test
    void getByIdForUser_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        var email = "courier@example.com";
        when(vehiculeRepository.findByIdAndUserEmail(id, email)).thenReturn(Optional.empty());

        var result = service.getByIdForUser(id, email);

        assertFalse(result.isPresent());
        verify(vehiculeRepository).findByIdAndUserEmail(id, email);
    }

    @Test
    void create_shouldPersistItem() {
        var entity = new Vehicule();
        entity.setRegistrationnumbers("123 TUN 456");
        var email = "courier@example.com";
        var user = new User();

        when(vehiculeRepository.existsByRegistrationnumbers("123 TUN 456")).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(vehiculeRepository.save(any(Vehicule.class))).thenReturn(entity);

        var result = service.create(entity, email);

        assertEquals(entity, result);
        verify(vehiculeRepository).existsByRegistrationnumbers("123 TUN 456");
        verify(userRepository).findByEmail(email);
        verify(vehiculeRepository).save(any(Vehicule.class));
    }

    @Test
    void updateForUser_whenFound_shouldPersistItem() {
        var id = 22L;
        var email = "courier@example.com";

        var existing = new Vehicule();
        existing.setId(id);

        var updates = new Vehicule();
        updates.setRegistrationnumbers("999 TUN 000");

        when(vehiculeRepository.findByIdAndUserEmail(id, email)).thenReturn(Optional.of(existing));
        when(vehiculeRepository.existsByRegistrationnumbersAndIdNot("999 TUN 000", id)).thenReturn(false);
        when(vehiculeRepository.save(existing)).thenReturn(existing);

        var result = service.updateForUser(id, updates, email);

        assertTrue(result.isPresent());
        assertEquals(existing, result.get());
        verify(vehiculeRepository).findByIdAndUserEmail(id, email);
        verify(vehiculeRepository).existsByRegistrationnumbersAndIdNot("999 TUN 000", id);
        verify(vehiculeRepository).save(existing);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 77L;

        service.delete(id);

        verify(vehiculeRepository).deleteById(id);
    }
}
