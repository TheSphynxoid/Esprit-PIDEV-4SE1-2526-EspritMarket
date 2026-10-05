package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Entity.EquipmentReservation;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentReservationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentReservationService {
    private final IEquipmentReservationRepository equipmentReservationRepository;

    public EquipmentReservationService(IEquipmentReservationRepository equipmentReservationRepository) {
        this.equipmentReservationRepository = equipmentReservationRepository;
    }

    public List<EquipmentReservation> getAll() {
        return equipmentReservationRepository.findAll();
    }

    public Optional<EquipmentReservation> getById(Long id) {
        return equipmentReservationRepository.findById(id);
    }

    public EquipmentReservation create(EquipmentReservation equipmentReservation) {
        return equipmentReservationRepository.save(equipmentReservation);
    }

    public EquipmentReservation update(Long id, EquipmentReservation equipmentReservation) {
        equipmentReservation.setId(id);
        return equipmentReservationRepository.save(equipmentReservation);
    }

    public void delete(Long id) {
        equipmentReservationRepository.deleteById(id);
    }
}
