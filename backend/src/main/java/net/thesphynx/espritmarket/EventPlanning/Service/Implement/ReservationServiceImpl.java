package net.thesphynx.espritmarket.EventPlanning.Service.Implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Reservation;
import net.thesphynx.espritmarket.EventPlanning.Repository.IReservationRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.Interface.IReservationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements IReservationService {

    private final IReservationRepository reservationRepository;

    @Override
    public Reservation addReservation(Reservation reservation) {
        log.info("Adding Reservation: {}", reservation);
        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation updateReservation(Reservation reservation) {
        log.info("Updating Reservation: {}", reservation);
        return reservationRepository.save(reservation);
    }

    @Override
    public void deleteReservation(Long id) {
        log.info("Deleting Reservation with id: {}", id);
        reservationRepository.deleteById(id);
    }

    @Override
    public Optional<Reservation> getReservationById(Long id) {
        log.info("Getting Reservation with id: {}", id);
        return reservationRepository.findById(id);
    }

    @Override
    public List<Reservation> getAllReservations() {
        log.info("Getting all Reservations");
        return reservationRepository.findAll();
    }
}
