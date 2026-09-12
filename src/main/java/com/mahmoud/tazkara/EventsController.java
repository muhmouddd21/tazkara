package com.mahmoud.tazkara;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;

@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    record Event(Long id, String name, String city, String category, int totalSeats) {}

    record CreateEventRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "City is required")
        String city,
        @NotBlank(message = "Category is required")
        @Pattern(regexp = "^[a-zA-Z]+$", message = "Category must be a valid category name")
        String category,
        @Min(value = 1, message = "Total seats must be at least 1")
        @Max(value = 1000, message = "Total seats must be less than 1000")
        int totalSeats) {}
    private Map<Long,Event> events = new HashMap<>();
    private final AtomicLong counter = new AtomicLong();


    @GetMapping
    public  List<Event> list(@RequestParam (required = false) String city) {
        List<Event> result = new ArrayList<>();

        for(Event event : events.values()) {
            if(city == null || event.city().equalsIgnoreCase(city)) {
                result.add(event);
            }
        }

        return result;

    }   

    @PostMapping
    public ResponseEntity<Event> create( @RequestBody @Valid CreateEventRequest request) {
        Long id = counter.incrementAndGet();

        Event newEvent = new Event(id, request.name(), request.city(), request.category(), request.totalSeats());   
        events.put(id, newEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }
    


    @GetMapping("/{id}")
    public ResponseEntity<Event> get(@PathVariable Long id) {
        Optional<Event> event = Optional.ofNullable(events.get(id));

        if(event.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event.get());
    }
    
}