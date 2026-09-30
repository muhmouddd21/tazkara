package com.mahmoud.tazkara.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;



 
public class FakeEventRepo implements EventRepo{
    private Map<Long,Event> events = new HashMap<>();
    private final AtomicLong counter = new AtomicLong();


    @Override
    public boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city) {
        for(Event event: events.values()){
            if(event.city().equalsIgnoreCase(city) && event.name().equalsIgnoreCase(name) ){
                return true;
            } 
        }
        return false;
    }


    @Override
    public long countByOrganizerEmailIgnoreCase(String email) {
        return events.values().stream().filter(event -> event.organizerEmail().equalsIgnoreCase(email)).count();
}


    @Override
    public void deleteById(Long id) {
        events.remove(id);
    }


    @Override
    public List<Event> findAll() {
        return new ArrayList<>(events.values());
    }


    @Override
    public Optional<Event> findById(Long id) {
        return Optional.ofNullable(events.get(id));

    }


    @Override
    public Event save(Event event) {
        Long id = (event.id() != null) ? event.id() : counter.incrementAndGet();
    
        Event stored = new Event(id, event.organizerEmail(),event.name(), event.city(),
                event.category(), event.totalSeats());
        events.put(id, stored);
        return stored;
    }

}
