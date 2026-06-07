package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Entity.Ticket;
import net.thesphynx.espritmarket.EventPlanning.Repository.ITicketRepository;
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
class TicketServiceTest {

    @Mock
    private ITicketRepository ticketRepository;

    @InjectMocks
    private TicketService service;

    @Test
    void getAll_shouldReturnAllItems() {
        var expected = List.of(new Ticket(), new Ticket());
        when(ticketRepository.findAll()).thenReturn(expected);

        var result = service.getAll();

        assertEquals(expected, result);
        verify(ticketRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnItem() {
        var id = 1L;
        var entity = new Ticket();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(ticketRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(ticketRepository).findById(id);
    }

    @Test
    void create_shouldPersistItem() {
        var entity = new Ticket();
        when(ticketRepository.save(entity)).thenReturn(entity);

        var result = service.create(entity);

        assertEquals(entity, result);
        verify(ticketRepository).save(entity);
    }

    @Test
    void update_shouldSetIdAndPersistItem() {
        var id = 22L;
        var entity = new Ticket();
        entity.setId(999L);
        when(ticketRepository.save(entity)).thenReturn(entity);

        var result = service.update(id, entity);

        assertEquals(id, entity.getId());
        assertEquals(entity, result);
        verify(ticketRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 77L;

        service.delete(id);

        verify(ticketRepository).deleteById(id);
    }
}
