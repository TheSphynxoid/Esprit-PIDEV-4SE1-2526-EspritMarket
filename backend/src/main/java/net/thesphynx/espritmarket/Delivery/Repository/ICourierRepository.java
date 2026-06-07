package net.thesphynx.espritmarket.Delivery.Repository;

import net.thesphynx.espritmarket.Delivery.Entity.Courier;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;
import net.thesphynx.espritmarket.Delivery.Dto.CourierStatisticsResponse;
import net.thesphynx.espritmarket.Delivery.Dto.TopCourierResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICourierRepository extends JpaRepository<Courier, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<Courier> findAllBy();

    Optional<Courier> findByUserEmail(String email);

    Optional<Courier> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<Courier> findWithUserById(Long id);

    List<Courier> findByStatus(CourierStatus status);

    @Query("""
            SELECT new net.thesphynx.espritmarket.Delivery.Dto.CourierStatisticsResponse(
                c.id,
                u.id,
                u.name,
                u.email,
                c.status,
                COUNT(DISTINCT v.id),
                COUNT(DISTINCT d.id),
                SUM(CASE WHEN UPPER(COALESCE(d.status, '')) IN ('DELIVERED', 'LIVREE', 'LIVRE') THEN 1 ELSE 0 END),
                SUM(CASE WHEN UPPER(COALESCE(d.status, '')) IN ('PENDING', 'EN_ATTENTE') THEN 1 ELSE 0 END),
                SUM(CASE WHEN UPPER(COALESCE(d.status, '')) IN ('CANCELLED', 'ANNULEE', 'ANNULE') THEN 1 ELSE 0 END),
                COALESCE(SUM(d.distanceKm), 0),
                COALESCE(AVG(d.distanceKm), 0)
            )
            FROM Courier c
            JOIN c.user u
            LEFT JOIN Vehicule v ON v.user.id = u.id
            LEFT JOIN v.deliveries d
            WHERE c.id = :courierId
            GROUP BY c.id, u.id, u.name, u.email, c.status
            """)
    Optional<CourierStatisticsResponse> findCourierStatisticsByCourierId(@Param("courierId") Long courierId);

    @Query("""
            SELECT new net.thesphynx.espritmarket.Delivery.Dto.TopCourierResponse(
                c.id,
                u.id,
                u.name,
                u.email,
                SUM(CASE WHEN UPPER(COALESCE(d.status, '')) IN ('DELIVERED', 'LIVREE', 'LIVRE') THEN 1 ELSE 0 END)
            )
            FROM Courier c
            JOIN c.user u
            LEFT JOIN Vehicule v ON v.user.id = u.id
            LEFT JOIN v.deliveries d
            GROUP BY c.id, u.id, u.name, u.email
            HAVING SUM(CASE WHEN UPPER(COALESCE(d.status, '')) IN ('DELIVERED', 'LIVREE', 'LIVRE') THEN 1 ELSE 0 END) > 0
            ORDER BY SUM(CASE WHEN UPPER(COALESCE(d.status, '')) IN ('DELIVERED', 'LIVREE', 'LIVRE') THEN 1 ELSE 0 END) DESC, u.name ASC
            """)
    List<TopCourierResponse> findTopCouriersByDeliveredDeliveries(Pageable pageable);
}
