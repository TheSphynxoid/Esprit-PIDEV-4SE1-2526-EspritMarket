package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Entity.ServiceCategory;
import net.thesphynx.espritmarket.Srv.Entity.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IServiceRepository extends JpaRepository<Service, Long> {
    Page<Service> findByProvider_Id(Long providerId, Pageable pageable);
    Page<Service> findByCategory(ServiceCategory category, Pageable pageable);
    Page<Service> findByStatus(ServiceStatus status, Pageable pageable);
    Page<Service> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.price BETWEEN :minPrice AND :maxPrice AND s.deletedAt IS NULL")
    Page<Service> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.rating >= :minRating AND s.deletedAt IS NULL")
    Page<Service> findByRatingGreaterThanEqual(BigDecimal minRating, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Service s JOIN s.tags t WHERE t.tag IN :tags AND s.deletedAt IS NULL")
    Page<Service> findByTagsIn(List<String> tags, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.deletedAt IS NULL")
    Page<Service> findAllActive(Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.provider.id = :providerId AND s.deletedAt IS NULL")
    Page<Service> findActiveByProviderId(Long providerId, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.name ILIKE %:keyword% AND s.deletedAt IS NULL")
    Page<Service> searchByName(String keyword, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.deletedAt IS NULL AND " +
           "(:category IS NULL OR s.category = :category) AND " +
           "(:minPrice IS NULL OR s.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR s.price <= :maxPrice) AND " +
           "(:location IS NULL OR LOWER(s.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    Page<Service> findByFilters(ServiceCategory category, BigDecimal minPrice, BigDecimal maxPrice,
                                String location, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.deletedAt IS NULL AND s.allowProjectParticipation = true")
    Page<Service> findProjectEligible(Pageable pageable);
}
