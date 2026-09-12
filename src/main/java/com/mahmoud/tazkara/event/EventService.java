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


    public List<Event> list(String city) {
        List<Event> all = eventRepo.findAll();
        if (city == null) {
            return all;
        }
        return all.stream()
                .filter(event -> event.city().equalsIgnoreCase(city))
                .toList();
    }


    public Optional<Event> get(Long id) {
        return eventRepo.findById(id);
    }

    public Event create(CreateEventRequest request) {
        if (eventRepo.existsByNameorCity(request.name(), request.city())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "an event named '" + request.name() + "' already exists in " + request.city());
        }
        Event event = new Event(null, request.name(), request.city(),
                request.category(), request.totalSeats());
        return eventRepo.save(event);
    }
}
