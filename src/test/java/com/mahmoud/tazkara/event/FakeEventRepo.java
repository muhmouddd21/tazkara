package com.mahmoud.tazkara.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;



 
public class FakeEventRepo implements EventRepo{
    private Map<Long,Event> events = new HashMap<>();
    private final AtomicLong counter = new AtomicLong();



    private Page<Event> pageOf(List<Event> all, Pageable pageable) {
        int from = Math.min((int) pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }

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


    @Override
    public Page<Event> findAll(Pageable pageable) {
        return pageOf(new ArrayList<>(events.values()), pageable);
    }


    @Override
    public Page<Event> findByCityIgnoreCase(String city, Pageable pageable) {
        List<Event> matching = events.values().stream()
                .filter(event -> event.city().equalsIgnoreCase(city)).toList();
        return pageOf(matching, pageable);
    }

}
