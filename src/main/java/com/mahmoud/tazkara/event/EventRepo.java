package com.mahmoud.tazkara.event;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;


 
public interface EventRepo extends Repository<Event,Long> {


    Page<Event> findAll(Pageable pageable);
    Page<Event> findByCityIgnoreCase(String city, Pageable pageable);

    Optional<Event> findById(Long id);

    @Query ("select coalesce(sum(e.totalSeats), 0) from Event e")
    long sumTotalSeats();   


    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);


    long countByOrganizerEmailIgnoreCase(String organizerEmail);

    long count();
    
    Event save(Event event);
    void deleteById(Long id);   
}
