package com.mahmoud.tazkara.ticket;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;


@Testcontainers 
@SpringBootTest 
@ActiveProfiles ("test")
public class OversellingTest {
    
    @Autowired TicketsService ticketsService;
    @Autowired EventRepo eventRepo;
    @Autowired TicketRepo ticketRepo;

    @Container 
    @ServiceConnection 
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");



    @Test 
    void twentyRacingBuyersCannotOversellFiveSeats() throws Exception {
        Event event = eventRepo.save(
            new Event(null, "org@test.dev", "The Finale", "Cairo", "music", 5,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))));
            
        int buyers =20;
        ExecutorService pool= Executors.newFixedThreadPool(buyers);
        CountDownLatch startGun = new CountDownLatch(1);

        for (int i = 0; i < buyers; i++) {
            String email = "buyer" + i + "@test.dev";
            pool.submit(() -> {
                try {
                    startGun.await();                      // line up at the gate
                    ticketsService.purchase(email, event.id());
                } catch (Exception expected) {
                    // "sold out" 409s are the happy path for 15 of these threads
                }
            });
        }
        startGun.countDown(); 
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(ticketRepo.countByEventIdAndStatus(event.id(),TicketStatus.PURCHASED)).isEqualTo(5);
    }


}
