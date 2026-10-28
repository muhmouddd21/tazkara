package com.mahmoud.tazkara.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers 
@SpringBootTest 
@ActiveProfiles ("test")
public class LostUpdateTest {
    @Container @ServiceConnection 
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");


    @Autowired EventService eventService;

    @Test 
    void staleTabCannotSilentlyEraseAnEdit() {
        EventResponseDTO created = eventService.create(
        new CreateEventRequest("Jazz Night", "Cairo", "music", 100,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))),"owner@test.dev", true); 
            
            
        eventService.update(created.id(),
        new UpdateEventRequest("Jazz Night", "Cairo", "concert", 100, created.version()),
        "owner@test.dev", true);


                // tab 2 saves its own edit — STILL holding version 0 (stale)
        assertThatThrownBy(() -> eventService.update(created.id(),
        new UpdateEventRequest("Jazz Nite!", "Cairo", "music", 100, created.version()),
        "owner@test.dev", true))
        .hasMessageContaining("modified");


        assertThat(eventService.get(created.id()).type())
        .isEqualTo("concert");
    }

}
