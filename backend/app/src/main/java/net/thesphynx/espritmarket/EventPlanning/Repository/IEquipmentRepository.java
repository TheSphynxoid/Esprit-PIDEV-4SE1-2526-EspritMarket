package net.thesphynx.espritmarket.EventPlanning.Repository;

import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEquipmentRepository extends JpaRepository<Equipment, Long> {
    
    /**
     * Find all equipments for events created by a specific user
     */
    @Query("SELECT eq FROM Equipment eq WHERE eq.event.creator.id = :userId ORDER BY eq.event.date DESC")
    List<Equipment> findEquipmentsByCreatorId(@Param("userId") Long userId);

    /**
     * Find all equipments for a specific event
     */
    @Query("SELECT eq FROM Equipment eq WHERE eq.event.id = :eventId ORDER BY eq.name")
    List<Equipment> findEquipmentsByEventId(@Param("eventId") Long eventId);
}