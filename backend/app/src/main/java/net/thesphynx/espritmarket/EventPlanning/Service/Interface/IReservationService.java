package net.thesphynx.espritmarket.EventPlanning.Service.Interface;

import net.thesphynx.espritmarket.EventPlanning.Entity.Reservation;
import java.util.List;
import java.util.Optional;

public interface IReservationService {
    Reservation addReservation(Reservation reservation);
    Reservation updateReservation(Reservation reservation);
    void deleteReservation(Long id);
    Optional<Reservation> getReservationById(Long id);
    List<Reservation> getAllReservations();
}