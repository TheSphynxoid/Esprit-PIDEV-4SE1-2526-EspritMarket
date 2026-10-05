package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.ProjectRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProjectResponse;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Mapper.ProjectMapper;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceMapper;
import net.thesphynx.espritmarket.Srv.Repository.IProjectRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
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
class ProjectServiceTest {

    @Mock
    private IProjectRepository projectRepository;

    @Mock
    private IServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private ProjectService service;

    @Test
    void getAll_shouldReturnPageOfResponses() {
        var entity = new Project();
        var response = new ProjectResponse();
        Page<Project> page = new PageImpl<>(List.of(entity));
        when(projectRepository.findAllActive(PageRequest.of(0, 20))).thenReturn(page);
        when(projectMapper.toResponse(entity)).thenReturn(response);

        var result = service.getAll(0, 20);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void create_shouldMapPersistAndReturnResponse() {
        var request = new ProjectRequest();
        var entity = new Project();
        var response = new ProjectResponse();
        var creator = new User();
        creator.setId(1L);
        when(projectMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(projectRepository.save(entity)).thenReturn(entity);
        when(projectMapper.toResponse(entity)).thenReturn(response);

        var result = service.create(request, 1L);

        assertEquals(response, result);
    }

    @Test
    void delete_shouldSoftDelete() {
        var id = 1L;
        var entity = new Project();
        when(projectRepository.findById(id)).thenReturn(Optional.of(entity));

        service.delete(id);

        assertNotNull(entity.getDeletedAt());
        verify(projectRepository).save(entity);
    }
}
