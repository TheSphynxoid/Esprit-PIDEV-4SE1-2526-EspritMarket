package net.thesphynx.espritmarket.EventPlanning.Service.Interface;

import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import java.util.List;
import java.util.Optional;

public interface IEquipmentService {
    Equipment addEquipment(Equipment equipment);
    Equipment updateEquipment(Equipment equipment);
    void deleteEquipment(Long id);
    Optional<Equipment> getEquipmentById(Long id);
    List<Equipment> getAllEquipments();
}