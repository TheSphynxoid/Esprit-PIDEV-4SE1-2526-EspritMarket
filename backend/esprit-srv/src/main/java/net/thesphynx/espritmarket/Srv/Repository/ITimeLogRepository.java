package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITimeLogRepository extends JpaRepository<TimeLog, Long> {

    List<TimeLog> findByBookingIdOrderByStartTimeDesc(Long bookingId);

    @Query("SELECT tl FROM TimeLog tl WHERE tl.booking.id = :bookingId AND tl.endTime IS NULL")
    List<TimeLog> findActiveByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT SUM(COALESCE(tl.durationMinutes, 0)) FROM TimeLog tl WHERE tl.booking.id = :bookingId")
    Long totalMinutesByBookingId(@Param("bookingId") Long bookingId);
}
