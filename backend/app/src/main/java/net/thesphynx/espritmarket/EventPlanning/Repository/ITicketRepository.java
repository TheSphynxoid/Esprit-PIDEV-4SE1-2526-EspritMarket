package net.thesphynx.espritmarket.EventPlanning.Repository;

import net.thesphynx.espritmarket.EventPlanning.Entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ITicketRepository extends JpaRepository<Ticket, Long> {
    
    @Query("SELECT t FROM Ticket t WHERE t.stripeSessionId = :sessionId")
    Optional<Ticket> findByStripeSessionId(@Param("sessionId") String sessionId);
    
    @Query("SELECT t FROM Ticket t WHERE t.stripePaymentIntentId = :paymentIntentId")
    Optional<Ticket> findByStripePaymentIntentId(@Param("paymentIntentId") String paymentIntentId);
}