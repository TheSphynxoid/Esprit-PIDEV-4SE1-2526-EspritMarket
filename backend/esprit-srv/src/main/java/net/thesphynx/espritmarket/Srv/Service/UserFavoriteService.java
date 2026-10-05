package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceResponse;
import net.thesphynx.espritmarket.Srv.Entity.UserFavorite;
import net.thesphynx.espritmarket.Srv.Mapper.ServiceMapper;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import net.thesphynx.espritmarket.Srv.Repository.IUserFavoriteRepository;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserFavoriteService {
    private final IUserFavoriteRepository userFavoriteRepository;
    private final IServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;

    public UserFavoriteService(IUserFavoriteRepository userFavoriteRepository,
                               IServiceRepository serviceRepository,
                               ServiceMapper serviceMapper) {
        this.userFavoriteRepository = userFavoriteRepository;
        this.serviceRepository = serviceRepository;
        this.serviceMapper = serviceMapper;
    }

    public PageResponse<ServiceResponse> getUserFavorites(Long userId, int page, int size) {
        Page<UserFavorite> result = userFavoriteRepository.findByUserId(userId, PageRequest.of(page, size));
        List<ServiceResponse> content = result.getContent().stream()
                .map(uf -> serviceMapper.toResponse(uf.getService()))
                .toList();
        return PageResponse.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public void addFavorite(Long userId, Long serviceId) {
        if (userFavoriteRepository.existsByUserIdAndServiceId(userId, serviceId)) {
            throw new BadRequestException("Service already in favorites");
        }
        if (!serviceRepository.existsById(serviceId)) {
            throw new ResourceNotFoundException("Service", serviceId);
        }
        UserFavorite favorite = new UserFavorite();
        User user = new User();
        user.setId(userId);
        favorite.setUser(user);
        net.thesphynx.espritmarket.Srv.Entity.Service service = new net.thesphynx.espritmarket.Srv.Entity.Service();
        service.setId(serviceId);
        favorite.setService(service);
        userFavoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long serviceId) {
        userFavoriteRepository.deleteByUserIdAndServiceId(userId, serviceId);
    }

    public boolean isFavorite(Long userId, Long serviceId) {
        return userFavoriteRepository.existsByUserIdAndServiceId(userId, serviceId);
    }
}
