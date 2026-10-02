package com.mahmoud.tazkara.event;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;


 
public interface EventRepo extends Repository<Event,Long> {


    List<Event> findAll();



    Optional<Event> findById(Long id);


    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);


    long countByOrganizerEmailIgnoreCase(String organizerEmail);


    Event save(Event event);
    void deleteById(Long id);   
}
