package net.thesphynx.espritmarket.EventPlanning.Service.Implement;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Equipment;
import net.thesphynx.espritmarket.EventPlanning.Repository.IEquipmentRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.Interface.IEquipmentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentServiceImpl implements IEquipmentService {

    private final IEquipmentRepository equipmentRepository;

    @Override
    public Equipment addEquipment(Equipment equipment) {
        log.info("Adding Equipment: {}", equipment);
        return equipmentRepository.save(equipment);
    }

    @Override
    public Equipment updateEquipment(Equipment equipment) {
        log.info("Updating Equipment: {}", equipment);
        return equipmentRepository.save(equipment);
    }

    @Override
    public void deleteEquipment(Long id) {
        log.info("Deleting Equipment with id: {}", id);
        equipmentRepository.deleteById(id);
    }

    @Override
    public Optional<Equipment> getEquipmentById(Long id) {
        log.info("Getting Equipment with id: {}", id);
        return equipmentRepository.findById(id);
    }

    @Override
    public List<Equipment> getAllEquipments() {
        log.info("Getting all Equipments");
        return equipmentRepository.findAll();
    }
}
