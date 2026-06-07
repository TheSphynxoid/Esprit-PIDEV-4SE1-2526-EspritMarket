package net.thesphynx.espritmarket.EventPlanning.Service.Implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Ticket;
import net.thesphynx.espritmarket.EventPlanning.Repository.ITicketRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.Interface.ITicketService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements ITicketService {

    private final ITicketRepository ticketRepository;

    @Override
    public Ticket addTicket(Ticket ticket) {
        log.info("Adding Ticket: {}", ticket);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket updateTicket(Ticket ticket) {
        log.info("Updating Ticket: {}", ticket);
        return ticketRepository.save(ticket);
    }

    @Override
    public void deleteTicket(Long id) {
        log.info("Deleting Ticket with id: {}", id);
        ticketRepository.deleteById(id);
    }

    @Override
    public Optional<Ticket> getTicketById(Long id) {
        log.info("Getting Ticket with id: {}", id);
        return ticketRepository.findById(id);
    }

    @Override
    public List<Ticket> getAllTickets() {
        log.info("Getting all Tickets");
        return ticketRepository.findAll();
    }
}