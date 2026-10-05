package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.UserFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    Page<UserFavorite> findByUserId(Long userId, Pageable pageable);
    boolean existsByUserIdAndServiceId(Long userId, Long serviceId);
    void deleteByUserIdAndServiceId(Long userId, Long serviceId);
}
