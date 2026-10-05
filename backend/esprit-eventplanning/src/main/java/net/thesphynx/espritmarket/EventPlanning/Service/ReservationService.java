package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Dto.ReservationWithEquipmentRequest;
import net.thesphynx.espritmarket.EventPlanning.Entity.*;
import net.thesphynx.espritmarket.EventPlanning.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ReservationService {
    private final IReservationRepository reservationRepository;
    private final IEquipmentReservationRepository equipmentReservationRepository;
    private final IEventRepository eventRepository;
    private final IEquipmentRepository equipmentRepository;

    public ReservationService(
            IReservationRepository reservationRepository,
            IEquipmentReservationRepository equipmentReservationRepository,
            IEventRepository eventRepository,
            IEquipmentRepository equipmentRepository) {
        this.reservationRepository = reservationRepository;
        this.equipmentReservationRepository = equipmentReservationRepository;
        this.eventRepository = eventRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public List<Reservation> getAll() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> getById(Long id) {
        return reservationRepository.findById(id);
    }

    public Reservation create(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    public Reservation update(Long id, Reservation reservation) {
        reservation.setId(id);
        return reservationRepository.save(reservation);
    }

    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }

    /**
     * ✅ NEW METHOD: Create reservation WITH equipment reservations in a single transaction
     * This allows creating a reservation and its equipment reservations all at once
     */
    @Transactional
    public Reservation createWithEquipment(ReservationWithEquipmentRequest request) {
        log.info("📝 Creating reservation with {} equipment items", 
            request.getEquipments() != null ? request.getEquipments().size() : 0);
        
        // Step 1: Find the event
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> {
                log.error("❌ Event not found: {}", request.getEventId());
                return new IllegalArgumentException("Event not found: " + request.getEventId());
            });
        
        // Step 2: Create the reservation
        Reservation reservation = new Reservation();
        reservation.setName(request.getName());
        reservation.setDate(request.getDate());
        reservation.setEvent(event);
        
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("✅ Reservation created: {} (id={})", savedReservation.getName(), savedReservation.getId());
        
        // Step 3: Create equipment reservations if provided
        if (request.getEquipments() != null && !request.getEquipments().isEmpty()) {
            for (ReservationWithEquipmentRequest.EquipmentWithQuantity equipmentItem : request.getEquipments()) {
                
                // Find equipment
                Equipment equipment = equipmentRepository.findById(equipmentItem.getEquipmentId())
                    .orElseThrow(() -> {
                        log.error("❌ Equipment not found: {}", equipmentItem.getEquipmentId());
                        return new IllegalArgumentException("Equipment not found: " + equipmentItem.getEquipmentId());
                    });
                
                // Create equipment reservation with QUANTITY ✅
                EquipmentReservation equipmentReservation = new EquipmentReservation();
                equipmentReservation.setReservation(savedReservation);
                equipmentReservation.setEquipment(equipment);
                equipmentReservation.setQuantity(equipmentItem.getQuantity());  // ✅ QUANTITY ADDED!
                
                EquipmentReservation saved = equipmentReservationRepository.save(equipmentReservation);
                log.info("✅ Equipment reserved: {} x{} for reservation id={}",
                    equipment.getName(), equipmentItem.getQuantity(), savedReservation.getId());
            }
        }
        
        log.info("✅ Reservation complete with all equipment reservations");
        return savedReservation;
    }
}
