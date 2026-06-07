package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ServiceReview;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IServiceReviewRepository extends JpaRepository<ServiceReview, Long> {
    @Query("SELECT sr FROM ServiceReview sr WHERE sr.booking.service.id = :serviceId")
    Page<ServiceReview> findByServiceId(Long serviceId, Pageable pageable);

    Page<ServiceReview> findByBooking_Service_Provider_IdAndBooking_Status(Long providerId,
                                                                            BookingStatus status,
                                                                            Pageable pageable);

    Page<ServiceReview> findByBookingId(Long bookingId, Pageable pageable);

    @Query("SELECT AVG(sr.rating) FROM ServiceReview sr WHERE sr.booking.service.provider.id = :providerId")
    Double averageRatingByProviderId(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(sr) FROM ServiceReview sr WHERE sr.booking.service.provider.id = :providerId")
    long countByProviderId(@Param("providerId") Long providerId);
}
