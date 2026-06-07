package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.EscrowHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IEscrowHoldRepository extends JpaRepository<EscrowHold, Long> {
    Optional<EscrowHold> findByBookingId(Long bookingId);

    @Query("SELECT e FROM EscrowHold e WHERE e.status = :status AND e.releaseDeadline IS NOT NULL AND e.releaseDeadline < :deadline")
    List<EscrowHold> findOverdueHolds(@Param("status") String status, @Param("deadline") LocalDateTime deadline);

    boolean existsByBookingId(Long bookingId);
}
