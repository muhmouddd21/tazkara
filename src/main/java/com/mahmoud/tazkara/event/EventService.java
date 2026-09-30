package com.mahmoud.tazkara.event;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class EventService {
    private final EventRepo eventRepo;
    private static final int FREE_PLAN_EVENT_LIMIT = 3;

    public EventService(EventRepo repo){
        this.eventRepo=repo;
    }


    public EventPageDTO list(String city, int page, int size) {
        size=Math.min(size,50);

        List<Event> filtered = eventRepo.findAll()
        .stream()
        .filter(event -> city ==null || event.city().equalsIgnoreCase(city) )
        .toList();
        
        int from =Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());

        return new EventPageDTO(filtered.subList(from, to), page, size, filtered.size());
    }


    public Optional<Event> get(Long id) {
        return eventRepo.findById(id);
    }

    public Event create(CreateEventRequest request,String email,boolean isPro) {
        if (!isPro && eventRepo.countByOrganizerEmailIgnoreCase(email) >= FREE_PLAN_EVENT_LIMIT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "free plan allows " + FREE_PLAN_EVENT_LIMIT + " events — upgrade to PRO for unlimited");
        }



        if (eventRepo.existsByNameIgnoreCaseAndCityIgnoreCase( request.city(),request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "an event named '" + request.name() + "' already exists in " + request.city());
        }
        Event event = new Event(null,email,request.name(), request.city(),
                request.category(), request.totalSeats());
        return eventRepo.save(event);
    }

    public Event update(Long id,UpdateEventRequest request,String callerEmail,boolean plan){

        Event existing = eventRepo.findById(id)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "event" + id + "is not found"));

        if (!existing.organizerEmail().equalsIgnoreCase(callerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "only the organizer may modify this event");
        }

        Event updatedEvent = new Event(id,callerEmail,request.name(),request.city(),request.category(),request.totalSeats());
        return eventRepo.save(updatedEvent);
    }
    public void delete(long id,String callerEmail,boolean plan){

        Event existing = eventRepo.findById(id)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "event" + id + "is not found"));

        if (!existing.organizerEmail().equalsIgnoreCase(callerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "only the organizer may modify this event");
        }

        eventRepo.deleteById(id);
    }


    public EventStats stats() {
        List<Event> all = eventRepo.findAll();
        int seats = all.stream().mapToInt(Event::totalSeats).sum();
        return new EventStats(all.size(), seats);
    }


}
