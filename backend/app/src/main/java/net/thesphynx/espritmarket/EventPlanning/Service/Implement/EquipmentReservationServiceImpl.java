package net.thesphynx.espritmarket.EventPlanning.Service.Implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.EquipmentReservation;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentReservationRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.Interface.IEquipmentReservationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentReservationServiceImpl implements IEquipmentReservationService {

    private final IEquipmentReservationRepository equipmentReservationRepository;

    @Override
    public EquipmentReservation addEquipmentReservation(EquipmentReservation equipmentReservation) {
        log.info("Adding EquipmentReservation: {}", equipmentReservation);
        return equipmentReservationRepository.save(equipmentReservation);
    }

    @Override
    public EquipmentReservation updateEquipmentReservation(EquipmentReservation equipmentReservation) {
        log.info("Updating EquipmentReservation: {}", equipmentReservation);
        return equipmentReservationRepository.save(equipmentReservation);
    }

    @Override
    public void deleteEquipmentReservation(Long id) {
        log.info("Deleting EquipmentReservation with id: {}", id);
        equipmentReservationRepository.deleteById(id);
    }

    @Override
    public Optional<EquipmentReservation> getEquipmentReservationById(Long id) {
        log.info("Getting EquipmentReservation with id: {}", id);
        return equipmentReservationRepository.findById(id);
    }

    @Override
    public List<EquipmentReservation> getAllEquipmentReservations() {
        log.info("Getting all EquipmentReservations");
        return equipmentReservationRepository.findAll();
    }
}