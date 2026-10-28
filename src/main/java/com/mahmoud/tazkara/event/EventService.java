package com.mahmoud.tazkara.event;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mahmoud.tazkara.ticket.TicketRepo;
import com.mahmoud.tazkara.ticket.TicketStatus;


@Service 
public class EventService {
    private final EventRepo eventRepo;
    private final TicketRepo ticketrepo;
    private static final int FREE_PLAN_EVENT_LIMIT = 3;
    private static final String UNIQUE_NAME_CITY = "uq_events_name_city";

    public EventService(EventRepo repo,TicketRepo ticketrepo){
        this.eventRepo=repo;
        this.ticketrepo=ticketrepo;
    }


    public EventPageDTO list(String city, int page, int size) {
        size=Math.min(size,50);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));

        Page<Event> result = (city == null) ? eventRepo.findAll(pageable) : eventRepo.findByCityIgnoreCase(city, pageable);
                                

        return new EventPageDTO(result.getContent().stream().map(EventResponseDTO::from).toList(), page, size, result.getTotalElements());
    }


    @Cacheable(cacheNames = "events", key = "#id")
    public EventResponseDTO get(Long id) {
        return EventResponseDTO.from(eventRepo.findById(id)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "event " + id + " not found")));
    }

    @Transactional 
    @CacheEvict (cacheNames = "stats",key = "'event-stats'")
    public EventResponseDTO create(CreateEventRequest request,String email,boolean isPro) {
        if (!isPro && eventRepo.countByOrganizerEmailIgnoreCase(email) >= FREE_PLAN_EVENT_LIMIT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "free plan allows " + FREE_PLAN_EVENT_LIMIT + " events — upgrade to PRO for unlimited");
        }



        if (eventRepo.existsByNameIgnoreCaseAndCityIgnoreCase(request.name(), request.city())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "an event named '" + request.name() + "' already exists in " + request.city());
        }
        Event event = new Event(null,email,request.name(), request.city(),
                request.category(), request.totalSeats(),request.startAt(),request.endAt());

        try {
            return EventResponseDTO.from(eventRepo.save(event));

        } catch (DataIntegrityViolationException e) {
            if (violates(e, UNIQUE_NAME_CITY)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"an event named '" + request.name() + "' already exists in " + request.city());
            }
            throw e;
        }
    }

    private static boolean violates(DataIntegrityViolationException e, String constraintName) {
        return e.getCause() instanceof ConstraintViolationException cve
                && constraintName.equalsIgnoreCase(cve.getConstraintName());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "events", key = "#id"),
        @CacheEvict(cacheNames = "stats", key = "'event-stats'")
    })
    public Event update(Long id,UpdateEventRequest request,String callerEmail,boolean plan){



        Event existing = eventRepo.findById(id)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "event" + id + "is not found"));

        if (!existing.organizerEmail().equalsIgnoreCase(callerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "only the organizer may modify this event");
        }

        if (eventRepo.existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(request.name(), request.city(), id)) {
         throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "An event with this name already exists in this city"
            );
        }

        if (!request.version().equals(existing.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "event was modified by someone else — reload and try again");
        }



        existing.applyEdit(request.name(), request.city(), request.category(), request.totalSeats());
        return existing;  
    }
    @Transactional 
    @Caching (evict = {
        @CacheEvict(cacheNames = "events", key = "#id"),
        @CacheEvict(cacheNames = "stats", key = "'event-stats'")
    })
    public void delete(long id,String callerEmail,boolean plan){

        Event existing = eventRepo.findWithLockById(id)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "event" + id + "is not found"));



        if (!existing.organizerEmail().equalsIgnoreCase(callerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "only the organizer may modify this event");
        }

        if(this.ticketrepo.countByEventId(existing.id()) > 0){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot delete an event with sold tickets");
        }

        eventRepo.deleteById(id);
    }

    @Cacheable(cacheNames = "stats", key = "'event-stats'")
    public EventStats stats() {
        Long count = eventRepo.count();
        Long seats = eventRepo.sumTotalSeats();
        Long cancellations =ticketrepo.countByStatus(TicketStatus.CANCELLED);
        return new EventStats(count, seats,cancellations);
    }

    public NumberOfSeatsDTO AvailableSeats(Long eventId){
        Event event = eventRepo.findById(eventId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Event not found"
        ));

        Long sold = ticketrepo.countByEventIdAndStatus(
            eventId,
            TicketStatus.PURCHASED
        );

        return new NumberOfSeatsDTO(
            eventId,
            event.name(),
            event.totalSeats() - sold
        );

    }


}
