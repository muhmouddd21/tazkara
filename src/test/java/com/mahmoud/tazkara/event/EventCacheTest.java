package com.mahmoud.tazkara.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers 
@SpringBootTest (properties = "spring.cache.type=redis")
@ActiveProfiles ("test")
public class EventCacheTest {
    
    @Container 
    @ServiceConnection 
    static PostgreSQLContainer postgres =new PostgreSQLContainer("postgres:16");

    @Container 
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);


    @Autowired EventService eventService;
    @Autowired JdbcTemplate jdbc;


    @Test 
    void secondReadNeverReachesTheDatabase() {
        EventResponseDTO created = eventService.create(new CreateEventRequest("Cache Fest", "Giza", "music", 50), "owner@test.dev", true);

        eventService.get(created.id());                       

        jdbc.update("UPDATE events SET name = 'Changed Behind Your Back' WHERE id = ?", created.id());

        assertThat(eventService.get(created.id()).name()).isEqualTo("Cache Fest");

    }
    @Test 
    void updateEvictsCacheAndReturnsFreshData(){
        EventResponseDTO created = eventService.create(new CreateEventRequest("Evict Fest", "Giza", "music", 50), "owner@test.dev", true);
        eventService.get(created.id());   //cached    
        
        UpdateEventRequest newEvent = new UpdateEventRequest("Evict Fest II", "Giza", "music", 50,created.version());
        eventService.update(created.id(), newEvent, "owner@test.dev", true);

        assertThat(eventService.get(created.id()).name()).isEqualTo("Evict Fest II");

    }


}
