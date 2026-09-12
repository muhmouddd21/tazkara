package com.mahmoud.tazkara.event;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final EventService eventService;

    public EventsController(EventService eventService){
        this.eventService = eventService;
    }


    @GetMapping
    public  List<Event> list(@RequestParam (required = false) String city) {
        return eventService.list(city);
        

    }   

    @PostMapping
    public ResponseEntity<Event> create( @RequestBody @Valid CreateEventRequest request) {

        Event newEvent = eventService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }
    


    @GetMapping("/{id}")
    public ResponseEntity<Event> get(@PathVariable Long id) {

        return eventService.get(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
}