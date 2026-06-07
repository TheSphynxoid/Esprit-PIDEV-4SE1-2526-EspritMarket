package net.thesphynx.espritmarket.EventPlanning.Repository;

import net.thesphynx.espritmarket.EventPlanning.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {
    
    /**
     * Find all events with date before today
     */
    @Query("SELECT e FROM Event e WHERE e.date < :today")
    List<Event> findEventsBefore(@Param("today") LocalDate today);
    
    /**
     * Find all events with date equal to today
     */
    @Query("SELECT e FROM Event e WHERE e.date = :today")
    List<Event> findEventsOnDate(@Param("today") LocalDate today);
    
    /**
     * Find all events with related stalls before a given date
     * Uses JOIN with Stall entity and DISTINCT to avoid duplicates
     */
    @Query("SELECT DISTINCT e FROM Event e JOIN e.stalls s WHERE e.date < :date")
    List<Event> findEventsWithStallsBefore(@Param("date") LocalDate date);
    
    /**
     * Find all events with their tickets and associated users (participants)
     * Uses DISTINCT to avoid duplicates from multiple tickets per user
     * Eager loads tickets and users with JOIN FETCH
     */
    @Query("SELECT DISTINCT e FROM Event e " +
           "LEFT JOIN FETCH e.tickets t " +
           "LEFT JOIN FETCH t.user u " +
           "ORDER BY e.date DESC")
    List<Event> findEventsWithTicketsAndUsers();
    
    /**
     * Find a specific event with all its tickets and associated users
     * Uses JOIN FETCH for eager loading to avoid N+1 queries
     */
    @Query("SELECT e FROM Event e " +
           "LEFT JOIN FETCH e.tickets t " +
           "LEFT JOIN FETCH t.user u " +
           "WHERE e.id = :id")
    Optional<Event> findEventWithTicketsAndUsersById(@Param("id") Long id);

    /**
     * Find all events created by a specific user
     */
    @Query("SELECT e FROM Event e WHERE e.creator.id = :userId ORDER BY e.date DESC")
    List<Event> findEventsByCreatorId(@Param("userId") Long userId);

    /**
     * Find all events created by a user with their equipments
     */
    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.equipments WHERE e.creator.id = :userId ORDER BY e.date DESC")
    List<Event> findEventsByCreatorIdWithEquipments(@Param("userId") Long userId);
}