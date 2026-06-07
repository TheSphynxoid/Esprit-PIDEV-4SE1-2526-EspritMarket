package net.thesphynx.espritmarket.EventPlanning.Repository;

import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStallRepository extends JpaRepository<Stall, Long> {
    
    /**
     * Find all stalls that are linked to FINISHED events
     */
    @Query("SELECT s FROM Stall s WHERE s.event.status = 'FINISHED'")
    List<Stall> findStallsByFinishedEvents();
}