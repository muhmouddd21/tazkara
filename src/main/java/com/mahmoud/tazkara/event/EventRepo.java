package com.mahmoud.tazkara.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.repository.Repository;


 
public interface EventRepo extends Repository<Event,Long> {
    // private Map<Long,Event> events = new HashMap<>();
    // private final AtomicLong counter = new AtomicLong();



    List<Event> findAll();
    // public List<Event> findAll(){
    //     return new ArrayList<>(events.values());
    // }


    Optional<Event> findById(Long id);
    // public Optional<Event> findById(Long id){
    //     return Optional.ofNullable(events.get(id));
    // }

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
    // public boolean existsByNameorCity(String city, String name){
    //     for(Event event: events.values()){
    //         if(event.city().equalsIgnoreCase(city) && event.name().equalsIgnoreCase(name) ){
    //             return true;
    //         } 
    //     }
    //     return false;
    // }

    long countByOrganizerEmailIgnoreCase(String organizerEmail);

    // public long countByOrganizer(String email){
    //     return events.values().stream().filter(event -> event.organizerEmail().equalsIgnoreCase(email)).count();
    // }


    Event save(Event event);
    // public Event save(Event event){
    //     Long id = (event.id() != null) ? event.id() : counter.incrementAndGet();
    
    //     Event stored = new Event(id, event.organizerEmail(),event.name(), event.city(),
    //             event.category(), event.totalSeats());
    //     events.put(id, stored);
    //     return stored;

    // }
    void deleteById(Long id);
    // public void deleteById(long id ){
    //     events.remove(id);
    // }
}
