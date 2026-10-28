package com.mahmoud.tazkara.analytics;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
// import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.mahmoud.tazkara.config.KafkaConfig;
import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;
import com.mahmoud.tazkara.ticket.TicketPurchasedMessage;
import com.mahmoud.tazkara.ticket.TicketResponseDTO;
import com.mahmoud.tazkara.ticket.TicketsService;

@Testcontainers 
@SpringBootTest (properties = {
    "tazkara.messaging.kafka-enabled=true",
    "spring.kafka.listener.auto-startup=true",
    "spring.kafka.admin.auto-create=true"
})
@ActiveProfiles ("test")
public class TicketAnalyticsTest {

    @Container @ServiceConnection 
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");


    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("apache/kafka:4.2.1")
    );


    @Autowired TicketsService ticketsService;
    @Autowired EventRepo eventRepo;
    @Autowired JdbcTemplate jdbc;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;


    private Long ticketsSold(Long eventId) {
        return jdbc.query("SELECT tickets_sold FROM analytics_event_sales WHERE event_id = ?",
         rs -> rs.next() ? rs.getLong(1) : 0L, eventId);
    }

    private boolean processed(Long ticketId) {
        return jdbc.queryForObject("SELECT count(*) FROM analytics_processed_tickets WHERE ticket_id = ?",
            Long.class, ticketId) == 1;
    }

    @Test 
    void threeSalesBecomeACountOfThree() {
        Event event = eventRepo.save(new Event(null, "org@test.dev", "Stream Night", "Cairo", "music", 10,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))));

        ticketsService.purchase("a@test.dev", event.id());
        ticketsService.purchase("b@test.dev", event.id());
        ticketsService.purchase("c@test.dev", event.id());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(ticketsSold(event.id())).isEqualTo(3L));
    }
    
    @Test
    void redeliveredEventIsCountedOnce() {
        Event event = eventRepo.save(new Event(null, "org@test.dev", "Echo Stream", "Cairo", "music", 10,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))));
        TicketResponseDTO first = ticketsService.purchase("first@test.dev", event.id());
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(ticketsSold(event.id())).isEqualTo(1L));

        // a duplicate of the first event (what a replay, a rebalance or a crash before commit produces)…
        // kafkaTemplate.send(KafkaConfig.TICKET_EVENTS_TOPIC, String.valueOf(event.id()),
        kafkaTemplate.send(KafkaConfig.TICKET_EVENTS_TOPIC, "999",
            new TicketPurchasedMessage(first.id(), event.id(), "Echo Stream", "first@test.dev"));
        // …followed by a genuinely new sale with the SAME key → same partition → consumed after the duplicate
        TicketResponseDTO second = ticketsService.purchase("second@test.dev", event.id());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(processed(second.id())).isTrue());
        assertThat(ticketsSold(event.id())).isEqualTo(2L);         // not 3
    }

    @Test 
    void threeSalesAndCancelledBecomeACountOfTwo() {
        Event event = eventRepo.save(new Event(null, "org@test.dev", "Stream NightT", "Cairo", "music", 10,Instant.now().plus(Duration.ofDays(10)),Instant.now().plus(Duration.ofDays(12))));

        ticketsService.purchase("a@test.dev", event.id());
        ticketsService.purchase("b@test.dev", event.id());
        TicketResponseDTO ticketC = ticketsService.purchase("c@test.dev", event.id());


        ticketsService.cancel("c@test.dev", ticketC.id(), event.id());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(ticketsSold(event.id())).isEqualTo(2L));
    }

}
