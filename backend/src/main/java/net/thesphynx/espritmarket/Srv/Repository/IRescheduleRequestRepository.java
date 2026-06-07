package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.RescheduleRequest;
import net.thesphynx.espritmarket.Srv.Entity.RescheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IRescheduleRequestRepository extends JpaRepository<RescheduleRequest, Long> {
    Optional<RescheduleRequest> findByBookingAndStatus(Booking booking, RescheduleStatus status);
    List<RescheduleRequest> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    boolean existsByBookingAndStatus(Booking booking, RescheduleStatus status);
}
