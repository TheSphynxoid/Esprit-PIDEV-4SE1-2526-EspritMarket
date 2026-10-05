package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.OrderRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Order;
import net.thesphynx.espritmarket.Marketplace.Mapper.OrderMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderRepository;
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
class OrderServiceTest {

    @Mock
    private IOrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService service;

    @Test
    void getAll_shouldReturnMappedItems() {
        var e1 = new Order();
        var e2 = new Order();
        var r1 = new OrderResponse();
        var r2 = new OrderResponse();

        when(orderRepository.findAll()).thenReturn(List.of(e1, e2));
        when(orderMapper.toResponse(e1)).thenReturn(r1);
        when(orderMapper.toResponse(e2)).thenReturn(r2);

        var result = service.getAll();

        assertEquals(2, result.size());
        assertEquals(r1, result.get(0));
        assertEquals(r2, result.get(1));
        verify(orderRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnMappedItem() {
        var id = 1L;
        var entity = new Order();
        var response = new OrderResponse();
        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderMapper.toResponse(entity)).thenReturn(response);

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(response, result.get());
        verify(orderRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(orderRepository).findById(id);
    }

    @Test
    void create_shouldMapPersistAndReturnResponse() {
        var request = new OrderRequest();
        var entity = new Order();
        var response = new OrderResponse();

        when(orderMapper.toEntity(request)).thenReturn(entity);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponse(entity)).thenReturn(response);

        var result = service.create(request);

        assertEquals(response, result);
        verify(orderMapper).toEntity(request);
        verify(orderRepository).save(entity);
        verify(orderMapper).toResponse(entity);
    }

    @Test
    void update_shouldSetIdPersistAndReturnResponse() {
        var id = 9L;
        var request = new OrderRequest();
        var entity = new Order();
        var response = new OrderResponse();

        when(orderMapper.toEntity(request)).thenReturn(entity);
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toResponse(entity)).thenReturn(response);

        var result = service.update(id, request);

        assertEquals(id, entity.getId());
        assertEquals(response, result);
        verify(orderRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 12L;

        service.delete(id);

        verify(orderRepository).deleteById(id);
    }
}
