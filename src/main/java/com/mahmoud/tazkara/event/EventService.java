package com.mahmoud.tazkara.event;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class EventService {
    private final EventRepo eventRepo;

    public EventService(EventRepo repo){
        this.eventRepo=repo;
    }


    public EventPageDTO list(String city, int page, int size) {
        size=Math.min(size,50);

        List<Event> filtered = eventRepo.findAll()
        .stream()
        .filter(event -> event.city()==null || event.city().equalsIgnoreCase(city) )
        .toList();
        
        int from =Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());

        return new EventPageDTO(filtered.subList(from, to), page, size, filtered.size());
    }


    public Optional<Event> get(Long id) {
        return eventRepo.findById(id);
    }

    public Event create(CreateEventRequest request) {
        if (eventRepo.existsByNameorCity( request.city(),request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "an event named '" + request.name() + "' already exists in " + request.city());
        }
        Event event = new Event(null, request.name(), request.city(),
                request.category(), request.totalSeats());
        return eventRepo.save(event);
    }

    public Event update(Long id,UpdateEventRequest request){

        eventRepo.findById(id)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "event" + id + "is not found"));

        Event updatedEvent = new Event(id,request.name(),request.city(),request.category(),request.totalSeats());
        return eventRepo.save(updatedEvent);
    }
    public void delete(long id){

        eventRepo.findById(id)
        .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "event" + id + "is not found"));
        eventRepo.deleteById(id);
    }


}
