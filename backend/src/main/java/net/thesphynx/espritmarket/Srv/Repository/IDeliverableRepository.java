package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.Deliverable;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDeliverableRepository extends JpaRepository<Deliverable, Long> {
    List<Deliverable> findByBookingIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long bookingId);
    List<Deliverable> findByBookingAndStatusAndDeletedAtIsNull(Booking booking, DeliverableStatus status);
    List<Deliverable> findByBookingIdAndDeletedAtIsNull(Long bookingId);
    List<Deliverable> findByBookingStatusAndDeletedAtIsNull(BookingStatus bookingStatus);
}
