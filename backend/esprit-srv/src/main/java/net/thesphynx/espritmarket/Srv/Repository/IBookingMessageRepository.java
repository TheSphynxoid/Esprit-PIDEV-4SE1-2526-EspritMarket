package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.BookingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBookingMessageRepository extends JpaRepository<BookingMessage, Long> {
    List<BookingMessage> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    @Query("SELECT m FROM BookingMessage m WHERE m.booking.id = :bookingId ORDER BY m.id DESC")
    List<BookingMessage> findLatestBatch(@Param("bookingId") Long bookingId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT m FROM BookingMessage m WHERE m.booking.id = :bookingId AND m.id < :beforeId ORDER BY m.id DESC")
    List<BookingMessage> findBatchBeforeId(@Param("bookingId") Long bookingId,
                                           @Param("beforeId") Long beforeId,
                                           org.springframework.data.domain.Pageable pageable);

    @Query("SELECT m FROM BookingMessage m WHERE m.booking.id = :bookingId AND m.id > :afterId ORDER BY m.id ASC")
    List<BookingMessage> findBatchAfterId(@Param("bookingId") Long bookingId,
                                          @Param("afterId") Long afterId,
                                          org.springframework.data.domain.Pageable pageable);
}
