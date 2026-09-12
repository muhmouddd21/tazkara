package com.mahmoud.tazkara;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    record Event(Long id, String name, String city, String category, int totalSeats) {}

    private Map<Long,Event> events = new HashMap<>();


    @GetMapping
    public  List<Event> list(@RequestParam (required = false) String city) {
        List<Event> result = new ArrayList<>();

        for(Event event : events.values()) {
            if(city == null || event.city().equals(city)) {
                result.add(event);
            }
        }

        return result;

    }   

    @PostMapping
    public ResponseEntity<Event> create(@RequestBody Event event) {
        Long id = events.size() + 1L; 
        Event newEvent = new Event(id, event.name(), event.city(), event.category(), event.totalSeats());   
        events.put(id, newEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }
    


    @GetMapping("/{id}")
    public ResponseEntity<Event> get(@PathVariable Long id) {
        Event event = events.get(id);
        if(event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }
    
}