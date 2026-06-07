package net.thesphynx.espritmarket.EventPlanning.Repository;

import net.thesphynx.espritmarket.EventPlanning.Entity.EquipmentReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IEquipmentReservationRepository extends JpaRepository<EquipmentReservation, Long> {
}