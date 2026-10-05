package com.mahmoud.tazkara.ticket;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;

import jakarta.transaction.Transactional;

@Service 
public class TicketsService {
    
    private final TicketRepo ticketRepo;
    private final EventRepo eventRepo;



    public TicketsService(TicketRepo ticketRepo, EventRepo eventRepo) {
        this.ticketRepo = ticketRepo;
        this.eventRepo = eventRepo;
    }

    @Transactional 
    public TicketResponseDTO purchese(String buyerEmail,Long eventId){
        Event event = this.eventRepo.findById(eventId)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        

        long sold = ticketRepo.countByEventId(eventId);          
        if (sold >= event.totalSeats()) {                       
            throw new ResponseStatusException(HttpStatus.CONFLICT, "event is sold out");
        }

        return TicketResponseDTO.from(this.ticketRepo.save(new Ticket(event,buyerEmail,Instant.now())));
    }

    @Transactional
    public List<TicketResponseDTO> myTickets(String buyerEmail) {
        return ticketRepo.findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(buyerEmail)
                .stream().map(TicketResponseDTO::from).toList();
    }
}
