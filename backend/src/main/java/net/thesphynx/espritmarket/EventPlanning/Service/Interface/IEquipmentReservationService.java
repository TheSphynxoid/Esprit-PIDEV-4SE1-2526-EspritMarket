package net.thesphynx.espritmarket.EventPlanning.Service.Interface;

import net.thesphynx.espritmarket.EventPlanning.Entity.EquipmentReservation;
import java.util.List;
import java.util.Optional;

public interface IEquipmentReservationService {
    EquipmentReservation addEquipmentReservation(EquipmentReservation equipmentReservation);
    EquipmentReservation updateEquipmentReservation(EquipmentReservation equipmentReservation);
    void deleteEquipmentReservation(Long id);
    Optional<EquipmentReservation> getEquipmentReservationById(Long id);
    List<EquipmentReservation> getAllEquipmentReservations();
}