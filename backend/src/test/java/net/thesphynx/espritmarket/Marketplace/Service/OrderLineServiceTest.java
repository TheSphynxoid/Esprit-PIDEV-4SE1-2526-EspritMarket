package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.OrderLine;
import net.thesphynx.espritmarket.Marketplace.Mapper.OrderLineMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderLineRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;
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
class OrderLineServiceTest {

    @Mock
    private IOrderLineRepository orderLineRepository;

    @Mock
    private OrderLineMapper orderLineMapper;

    @Mock
    private IOrderRepository orderRepository;

    @Mock
    private IProductRepository productRepository;

    @InjectMocks
    private OrderLineService service;

    @Test
    void getAll_shouldReturnMappedItems() {
        var e1 = new OrderLine();
        var e2 = new OrderLine();
        var r1 = new OrderLineResponse();
        var r2 = new OrderLineResponse();

        when(orderLineRepository.findAll()).thenReturn(List.of(e1, e2));
        when(orderLineMapper.toResponse(e1)).thenReturn(r1);
        when(orderLineMapper.toResponse(e2)).thenReturn(r2);

        var result = service.getAll();

        assertEquals(2, result.size());
        assertEquals(r1, result.get(0));
        assertEquals(r2, result.get(1));
        verify(orderLineRepository).findAll();
    }

    @Test
    void getById_whenFound_shouldReturnMappedItem() {
        var id = 1L;
        var entity = new OrderLine();
        var response = new OrderLineResponse();
        when(orderLineRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderLineMapper.toResponse(entity)).thenReturn(response);

        var result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals(response, result.get());
        verify(orderLineRepository).findById(id);
    }

    @Test
    void getById_whenMissing_shouldReturnEmpty() {
        var id = 404L;
        when(orderLineRepository.findById(id)).thenReturn(Optional.empty());

        var result = service.getById(id);

        assertFalse(result.isPresent());
        verify(orderLineRepository).findById(id);
    }

    @Test
    void create_shouldMapPersistAndReturnResponse() {
        var request = new OrderLineRequest();
        request.setOrderId(1L);
        var entity = new OrderLine();
        var order = new net.thesphynx.espritmarket.Marketplace.Entity.Order();
        var response = new OrderLineResponse();
        entity.setPrice(12.5);
        entity.setQuantity(4);
        when(orderLineMapper.toEntity(request)).thenReturn(entity);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderLineRepository.save(entity)).thenReturn(entity);
        when(orderLineMapper.toResponse(entity)).thenReturn(response);

        var result = service.create(request);

        assertEquals(response, result);
        assertEquals(50.0, entity.getSubtotal());        verify(orderLineMapper).toEntity(request);
        verify(orderLineRepository).save(entity);
        verify(orderLineMapper).toResponse(entity);
    }

    @Test
    void update_shouldSetIdPersistAndReturnResponse() {
        var id = 9L;
        var request = new OrderLineRequest();
        var entity = new OrderLine();
        var response = new OrderLineResponse();
        entity.setPrice(3.0);
        entity.setQuantity(5);
        when(orderLineMapper.toEntity(request)).thenReturn(entity);
        when(orderLineRepository.save(entity)).thenReturn(entity);
        when(orderLineMapper.toResponse(entity)).thenReturn(response);

        var result = service.update(id, request);

        assertEquals(id, entity.getId());
        assertEquals(response, result);
        assertEquals(15.0, entity.getSubtotal());        verify(orderLineRepository).save(entity);
    }

    @Test
    void delete_shouldDeleteById() {
        var id = 12L;

        service.delete(id);

        verify(orderLineRepository).deleteById(id);
    }
}
