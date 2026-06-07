package net.thesphynx.espritmarket.EventPlanning.Service.Interface;

import net.thesphynx.espritmarket.EventPlanning.Entity.Ticket;
import java.util.List;
import java.util.Optional;

public interface ITicketService {
    Ticket addTicket(Ticket ticket);
    Ticket updateTicket(Ticket ticket);
    void deleteTicket(Long id);
    Optional<Ticket> getTicketById(Long id);
    List<Ticket> getAllTickets();
}