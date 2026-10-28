package com.mahmoud.tazkara.event;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mahmoud.tazkara.ticket.CancelResponseDTO;
import com.mahmoud.tazkara.ticket.TicketResponseDTO;
import com.mahmoud.tazkara.ticket.TicketsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final EventService eventService;
    private final TicketsService ticketsService;

    public EventsController(EventService eventService,TicketsService ticketsService){
        this.eventService = eventService;
        this.ticketsService=ticketsService;
    }


    @GetMapping
    public  EventPageDTO list(@RequestParam (required = false) String city,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam (defaultValue = "10")int size
    ) {
        return eventService.list(city,page,size);
        

    }   

    @PostMapping
    public ResponseEntity<EventResponseDTO> create( @RequestBody @Valid CreateEventRequest request,Authentication authentication) {
        String email = authentication.getName();

        boolean isPro = authentication.getAuthorities()
        .stream()
        .anyMatch(a -> a.getAuthority().equals("PLAN_PRO"));


        EventResponseDTO newEvent = eventService.create(request,email,isPro);

        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }
    @PutMapping ("/{id}")
    public EventResponseDTO update(@PathVariable Long id,
            @RequestBody @Valid UpdateEventRequest request,Authentication authentication
    ) {
        String email = authentication.getName();

        boolean isPro = authentication.getAuthorities()
        .stream()
        .anyMatch(a -> a.getAuthority().equals("PLAN_PRO"));


        return EventResponseDTO.from(eventService.update(id,request,email,isPro));
    }
    
    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,Authentication authentication){

        String email = authentication.getName();

        boolean isPro = authentication.getAuthorities()
        .stream()
        .anyMatch(a -> a.getAuthority().equals("PLAN_PRO"));

        eventService.delete(id,email,isPro);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }



    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PLAN_PRO')")
    public EventStats stats() {
        return eventService.stats();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> get(@PathVariable Long id) {
        EventResponseDTO event = eventService.get(id);
        return ResponseEntity.ok()
            .eTag("\"" + event.version() + "\"")                             
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
            .body(event);
    }

    @PostMapping("/{id}/tickets")
    public ResponseEntity<TicketResponseDTO> createTicket(
            @PathVariable Long id,
            Authentication authentication) {
    
        String email = authentication.getName();
    
        TicketResponseDTO ticket =
                ticketsService.purchase(email, id);
    
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticket);
    }
    @PatchMapping ("/{eventId}/tickets/{ticketId}/cancellation")
    public ResponseEntity<CancelResponseDTO>  cancelATicket(@PathVariable Long eventId,@PathVariable Long ticketId ,Authentication authentication) {
        String email = authentication.getName();
        CancelResponseDTO ticket =ticketsService.cancel(email, ticketId, eventId);

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/{eventId}/availableseats")
    public NumberOfSeatsDTO getNumbersOfAvailableSeats(@PathVariable Long eventId) {
        return eventService.AvailableSeats(eventId);
    }

    @GetMapping("/tickets")
    public List<TicketResponseDTO> getTickets(Authentication authentication) {
        String email = authentication.getName();

        return this.ticketsService.myTickets(email);
    }
    

    
    
}