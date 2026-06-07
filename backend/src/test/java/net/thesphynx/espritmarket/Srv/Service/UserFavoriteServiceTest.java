package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Entity.UserFavorite;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceMapper;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import net.thesphynx.espritmarket.Srv.Repository.IUserFavoriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFavoriteServiceTest {

    @Mock
    private IUserFavoriteRepository userFavoriteRepository;

    @Mock
    private IServiceRepository serviceRepository;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private UserFavoriteService userFavoriteService;

    @Test
    void getUserFavorites_shouldReturnPageOfServiceResponses() {
        var favorite = new UserFavorite();
        var svc = new Service();
        favorite.setService(svc);
        var response = new ServiceResponse();
        Page<UserFavorite> page = new PageImpl<>(List.of(favorite));

        when(userFavoriteRepository.findByUserId(1L, PageRequest.of(0, 20))).thenReturn(page);
        when(serviceMapper.toResponse(svc)).thenReturn(response);

        var result = userFavoriteService.getUserFavorites(1L, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(response, result.getContent().get(0));
        verify(userFavoriteRepository).findByUserId(1L, PageRequest.of(0, 20));
    }

    @Test
    void addFavorite_whenNotAlreadyFavorited_shouldSave() {
        when(userFavoriteRepository.existsByUserIdAndServiceId(1L, 10L)).thenReturn(false);
        when(serviceRepository.existsById(10L)).thenReturn(true);

        userFavoriteService.addFavorite(1L, 10L);

        verify(userFavoriteRepository).save(any(UserFavorite.class));
    }

    @Test
    void addFavorite_whenAlreadyFavorited_shouldThrow() {
        when(userFavoriteRepository.existsByUserIdAndServiceId(1L, 10L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userFavoriteService.addFavorite(1L, 10L));
    }

    @Test
    void addFavorite_whenServiceNotFound_shouldThrow() {
        when(userFavoriteRepository.existsByUserIdAndServiceId(1L, 99L)).thenReturn(false);
        when(serviceRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userFavoriteService.addFavorite(1L, 99L));
    }

    @Test
    void removeFavorite_shouldDelegateToRepository() {
        userFavoriteService.removeFavorite(1L, 10L);

        verify(userFavoriteRepository).deleteByUserIdAndServiceId(1L, 10L);
    }

    @Test
    void isFavorite_whenExists_shouldReturnTrue() {
        when(userFavoriteRepository.existsByUserIdAndServiceId(1L, 10L)).thenReturn(true);

        assertTrue(userFavoriteService.isFavorite(1L, 10L));
    }

    @Test
    void isFavorite_whenNotExists_shouldReturnFalse() {
        when(userFavoriteRepository.existsByUserIdAndServiceId(1L, 10L)).thenReturn(false);

        assertFalse(userFavoriteService.isFavorite(1L, 10L));
    }
}
