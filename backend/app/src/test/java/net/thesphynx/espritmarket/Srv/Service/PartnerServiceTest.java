package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Srv.Dto.PartnerRequest;
import net.thesphynx.espritmarket.Srv.Dto.PartnerResponse;
import net.thesphynx.espritmarket.Srv.Entity.Partner;
import net.thesphynx.espritmarket.Srv.Mapper.PartnerMapper;
import net.thesphynx.espritmarket.Srv.Repository.IPartnerRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private IPartnerRepository partnerRepository;

    @Mock
    private PartnerMapper partnerMapper;

    @InjectMocks
    private PartnerService service;

    @Test
    void getAll_shouldReturnPageOfResponses() {
        var entity = new Partner();
        var response = new PartnerResponse();
        Page<Partner> page = new PageImpl<>(List.of(entity));
        when(partnerRepository.findAllActive(PageRequest.of(0, 20))).thenReturn(page);
        when(partnerMapper.toResponse(entity)).thenReturn(response);

        var result = service.getAll(0, 20);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void create_shouldMapPersistAndReturnResponse() {
        var request = new PartnerRequest();
        var entity = new Partner();
        var response = new PartnerResponse();
        when(partnerMapper.toEntity(request)).thenReturn(entity);
        when(partnerRepository.save(entity)).thenReturn(entity);
        when(partnerMapper.toResponse(entity)).thenReturn(response);

        var result = service.create(request);

        assertEquals(response, result);
    }

    @Test
    void delete_shouldSoftDelete() {
        var id = 1L;
        var entity = new Partner();
        when(partnerRepository.findById(id)).thenReturn(Optional.of(entity));

        service.delete(id);

        assertNotNull(entity.getDeletedAt());
        verify(partnerRepository).save(entity);
    }
}
