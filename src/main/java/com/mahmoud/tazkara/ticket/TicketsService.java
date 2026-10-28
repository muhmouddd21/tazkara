package com.mahmoud.tazkara.ticket;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;


@Service 
public class TicketsService {
    
    private final TicketRepo ticketRepo;
    private final EventRepo eventRepo;
    private final ApplicationEventPublisher events;

    @PersistenceContext
    private EntityManager entityManager;

    public TicketsService(TicketRepo ticketRepo, EventRepo eventRepo,ApplicationEventPublisher events) {
        this.ticketRepo = ticketRepo;
        this.eventRepo = eventRepo;
        this.events =events;
    }

    @Transactional 
    @CacheEvict (cacheNames = "stats",key = "'event-stats'")
    public TicketResponseDTO purchase(String buyerEmail,Long eventId){
        Event event = this.eventRepo.findWithLockById(eventId)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"event not found"));
        

        long sold = ticketRepo.countByEventIdAndStatus(
            eventId,
            TicketStatus.PURCHASED
        );
                 
        if (sold >= event.totalSeats()) {                       
            throw new ResponseStatusException(HttpStatus.CONFLICT, "event is sold out");
        }
        Ticket saved = ticketRepo.save(new Ticket(event, buyerEmail, Instant.now()));
        events.publishEvent(new TicketPurchasedMessage(saved.id(), eventId, event.name(), buyerEmail));
        return TicketResponseDTO.from(saved);
    }

    @Transactional
    public List<TicketResponseDTO> myTickets(String buyerEmail) {
        return ticketRepo.findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(buyerEmail)
                .stream().map(TicketResponseDTO::from).toList();
    }

    @Transactional 
    @CacheEvict (cacheNames = "stats",key = "'event-stats'")
    public CancelResponseDTO cancel(String requesterEmail, Long ticketId, Long eventId){
        Event event = this.eventRepo.findWithLockById(eventId)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"event not found"));
        
        Ticket ticket =this.ticketRepo.findById(ticketId)
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"ticket not found"));

        if (!event.organizerEmail().equalsIgnoreCase(requesterEmail) && !ticket.buyerEmail().equalsIgnoreCase(requesterEmail)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You are not allowed to do this"
            );
        }

        if (!ticket.event().id().equals(eventId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Ticket does not belong to this event"
            );
        }

        Instant now = Instant.now();
        Instant cancellationDeadline = event.startAt().minus(Duration.ofHours(48));

        if (!now.isBefore(cancellationDeadline)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "You can't cancel a ticket less than 48 hours before the event"
            );
        }
        Instant cancelledAt = Instant.now();

        int updated = ticketRepo.cancelTicket(
            ticketId, requesterEmail, cancelledAt
        );
    
        if (updated == 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ticket is already cancelled or cannot be cancelled"
            );
        }
    
        entityManager.refresh(ticket);
    
        events.publishEvent(new TicketCancelledMessage(
            ticket.id(),
            event.id(),
            event.name(),
            ticket.buyerEmail(),
            ticket.cancelledBy(),
            ticket.cancelledAt()
        ));
    
        return new CancelResponseDTO(
            ticket.id(),
            event.id(),
            ticket.status(),
            ticket.cancelledAt(),
            ticket.cancelledBy()
        );

    }
}
