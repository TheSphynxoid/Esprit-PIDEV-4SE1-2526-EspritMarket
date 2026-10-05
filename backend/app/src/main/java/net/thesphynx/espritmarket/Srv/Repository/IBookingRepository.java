package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface IBookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByUserId(Long userId, Pageable pageable);
    Page<Booking> findByProviderId(Long providerId, Pageable pageable);
    Page<Booking> findByProviderIdAndStatus(Long providerId, BookingStatus status, Pageable pageable);
    Page<Booking> findByServiceId(Long serviceId, Pageable pageable);
    List<Booking> findByServiceIdAndStatus(Long serviceId, BookingStatus status);
    List<Booking> findByProviderIdAndStatusIn(Long providerId, Collection<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.provider.id = :providerId AND b.deletedAt IS NULL")
    Page<Booking> findActiveByProviderId(Long providerId, Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndProviderId(Long id, Long providerId);

    @Query(value = "SELECT COUNT(*) FROM booking b WHERE b.provider_id = :providerId " +
            "AND b.service_id = :serviceId AND b.status IN (:statuses) " +
            "AND b.date < :slotEnd AND (b.date + (b.duration || ' hour')::interval) > :slotStart",
            nativeQuery = true)
    long countOverlappingBookings(@Param("providerId") Long providerId,
                                  @Param("serviceId") Long serviceId,
                                  @Param("statuses") Collection<String> statuses,
                                  @Param("slotStart") LocalDateTime slotStart,
                                  @Param("slotEnd") LocalDateTime slotEnd);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.provider.id = :providerId AND b.status IN :statuses")
    long countActiveByProviderAndStatuses(@Param("providerId") Long providerId,
                                           @Param("statuses") Collection<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.provider.id = :providerId " +
            "AND b.service.id = :serviceId AND b.status IN :statuses")
    long countActiveByProviderServiceAndStatuses(@Param("providerId") Long providerId,
                                                   @Param("serviceId") Long serviceId,
                                                   @Param("statuses") Collection<BookingStatus> statuses);

    Page<Booking> findByProjectId(Long projectId, Pageable pageable);

    List<Booking> findAllByProjectId(Long projectId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.provider.id = :providerId AND b.deletedAt IS NULL")
    long countActiveByProviderId(@Param("providerId") Long providerId);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoff);
}
