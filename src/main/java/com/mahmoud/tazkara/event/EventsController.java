package com.mahmoud.tazkara.event;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public  EventPageDTO list(@RequestParam (required = false) String city,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam (defaultValue = "10")int size
    ) {
        return eventService.list(city,page,size);
        

    }   

    @PostMapping
    public ResponseEntity<Event> create( @RequestBody @Valid CreateEventRequest request) {

        Event newEvent = eventService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }
    @PutMapping ("/{id}")
    public Event update(@PathVariable Long id,
            @RequestBody @Valid UpdateEventRequest request
    ) {

        return eventService.update(id,request);
    }
    
    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        eventService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @GetMapping("/{id}")
    public ResponseEntity<Event> get(@PathVariable Long id) {

        return eventService.get(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
}