package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Dto.ServiceUpsertRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceMapper;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private IServiceRepository serviceRepository;

    @Mock
    private IServiceTagRepository serviceTagRepository;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private ServiceService serviceService;

    @Test
    void getAll_shouldReturnPageOfResponses() {
        var entity = new Service();
        var response = new ServiceResponse();
        Page<Service> page = new PageImpl<>(List.of(entity));
        when(serviceRepository.findAllActive(PageRequest.of(0, 20))).thenReturn(page);
        when(serviceMapper.toResponse(entity)).thenReturn(response);

        var result = serviceService.getAll(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(response, result.getContent().get(0));
        verify(serviceRepository).findAllActive(PageRequest.of(0, 20));
    }

    @Test
    void getById_whenFound_shouldReturnResponse() {
        var id = 1L;
        var entity = new Service();
        var response = new ServiceResponse();
        when(serviceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(serviceMapper.toResponse(entity)).thenReturn(response);

        var result = serviceService.getById(id);

        assertTrue(result.isPresent());
        assertEquals(response, result.get());
    }

    @Test
    void getById_whenDeleted_shouldReturnEmpty() {
        var id = 1L;
        var entity = new Service();
        entity.setDeletedAt(java.time.LocalDateTime.now());
        when(serviceRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = serviceService.getById(id);

        assertFalse(result.isPresent());
    }

    @Test
    void create_shouldPersistAndReturnResponse() {
        var request = new ServiceUpsertRequest();
        request.setTags(List.of("java", "spring"));
        var entity = new Service();
        var response = new ServiceResponse();
        when(serviceMapper.toEntity(request)).thenReturn(entity);
        when(serviceRepository.save(entity)).thenReturn(entity);
        when(serviceRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(serviceMapper.toResponse(entity)).thenReturn(response);

        var result = serviceService.create(request);

        assertEquals(response, result);
        verify(serviceTagRepository, times(2)).save(any());
    }

    @Test
    void delete_shouldSoftDelete() {
        var id = 1L;
        var entity = new Service();
        when(serviceRepository.findById(id)).thenReturn(Optional.of(entity));

        serviceService.delete(id);

        assertNotNull(entity.getDeletedAt());
        verify(serviceRepository).save(entity);
    }

    @Test
    void search_shouldCallRepository() {
        var entity = new Service();
        var response = new ServiceResponse();
        Page<Service> page = new PageImpl<>(List.of(entity));
        when(serviceRepository.searchByName("test", PageRequest.of(0, 20))).thenReturn(page);
        when(serviceMapper.toResponse(entity)).thenReturn(response);

        var result = serviceService.search("test", 0, 20);

        assertEquals(1, result.getContent().size());
        verify(serviceRepository).searchByName("test", PageRequest.of(0, 20));
    }
}
