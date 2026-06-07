package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.DisputeMediation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDisputeMediationRepository extends JpaRepository<DisputeMediation, Long> {
    Optional<DisputeMediation> findByBookingId(Long bookingId);
    List<DisputeMediation> findByStatusIn(List<DisputeMediation.MediationStatus> statuses);
    boolean existsByBookingId(Long bookingId);
}
