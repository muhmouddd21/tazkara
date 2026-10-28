package com.mahmoud.tazkara.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import com.jayway.jsonpath.JsonPath;
import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers 
@SpringBootTest(properties = {
    "tazkara.messaging.rabbit-enabled=true",
    "spring.rabbitmq.listener.simple.auto-startup=true"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc 
public class TicketCancellationTest {

    @Autowired TicketsService ticketsService;
    @Autowired EventRepo eventRepo;
    @Autowired TicketRepo ticketRepo;
    @Autowired JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @Autowired 
    MockMvc mockMvc;
    
    @Container 
    @ServiceConnection 
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");


    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit =
            new RabbitMQContainer(
                    DockerImageName.parse("rabbitmq:4-management")
    );


    private String registerAndLogin(String email) throws Exception {
        String credentials = """
                {"email":"%s","password":"correct-horse-battery"}""".formatted(email);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json").content(credentials));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json").content(credentials)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private Long ticketsSold(Long eventId) {
        return jdbc.query("SELECT tickets_sold FROM analytics_event_sales WHERE event_id = ?",
         rs -> rs.next() ? rs.getLong(1) : 0L, eventId);
    }




    @Test 
    void purchaseThenCancelSendsBothMessages() {

        Event event = eventRepo.save(
            new Event(
                null,
                "org@test.dev",
                "The Finaale",
                "Cairo",
                "music",
                5,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))
            )
        );

        String buyerEmail = "buyer@test.dev";

        // 2. Purchase a ticket.
        ticketsService.purchase(buyerEmail, event.id());

        Ticket ticket = ticketRepo
                .findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(buyerEmail)
                .stream()
                .findFirst()
                .orElseThrow();

        Long ticketId = ticket.id();


        // 3. Wait until the confirmation consumer processes the message.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Ticket fresh = ticketRepo.findById(ticketId).orElseThrow();

            assertThat(fresh.confirmation_sent_at()).isNotNull();
        });

        // 4. Cancel the ticket as the event organizer.
        ticketsService.cancel(
            "org@test.dev",
            ticketId,
            event.id()
        );

        // 5. Verify that the ticket is cancelled.
        Ticket cancelledTicket =
                ticketRepo.findById(ticketId).orElseThrow();

        assertThat(cancelledTicket.status())
                .isEqualTo(TicketStatus.CANCELLED);

        assertThat(cancelledTicket.cancelledAt()).isNotNull();
        assertThat(cancelledTicket.cancelledBy())
                .isEqualTo("org@test.dev");

        // 6. Wait until the cancellation consumer processes the message.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Ticket fresh = ticketRepo.findById(ticketId).orElseThrow();

            assertThat(fresh.cancellationEmailSentAt()).isNotNull();
        });

        // 7. The cancelled ticket no longer occupies a seat.
        assertThat(
            ticketRepo.countByEventIdAndStatus(
                event.id(),
                TicketStatus.PURCHASED
            )
        ).isEqualTo(0);


        Event updatedEvent = eventRepo.findById(event.id()).orElseThrow();

        assertThat(updatedEvent.totalSeats()).isEqualTo(5);
    }

    @Test
    void organizerCancelsTicketAndBuyerReceivesCancellationEmail() {
        // 1. Create an event owned by the organizer
        String organizerEmail = "org@test.dev";
        String buyerEmail = "buyer@test.dev";

        Event event = eventRepo.save(
            new Event(
                null,
                organizerEmail,
                "The Finalee",
                "Cairo",
                "music",
                5
                ,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))
            )
        );

        // 2. Buyer purchases a ticket
        ticketsService.purchase(buyerEmail, event.id());

        Ticket ticket = ticketRepo
            .findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(buyerEmail)
            .stream()
            .findFirst()
            .orElseThrow();

        Long ticketId = ticket.id();

        // 3. Wait for the purchase confirmation to be processed
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Ticket fresh = ticketRepo.findById(ticketId).orElseThrow();
            assertThat(fresh.confirmation_sent_at()).isNotNull();
        });

        // 4. Organizer cancels the ticket
        ticketsService.cancel(organizerEmail, ticketId, event.id());

        // 5. Verify cancellation details
        Ticket cancelledTicket = ticketRepo.findById(ticketId).orElseThrow();

        assertThat(cancelledTicket.status())
            .isEqualTo(TicketStatus.CANCELLED);

        assertThat(cancelledTicket.cancelledAt()).isNotNull();

        assertThat(cancelledTicket.cancelledBy())
            .isEqualTo(organizerEmail);

        // 6. Wait until the cancellation email is processed
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Ticket fresh = ticketRepo.findById(ticketId).orElseThrow();

            assertThat(fresh.cancellationEmailSentAt()).isNotNull();
        });

        // 7. Verify the ticket no longer occupies a seat
        assertThat(
            ticketRepo.countByEventIdAndStatus(
                event.id(),
                TicketStatus.PURCHASED
            )
        ).isZero();
    }



    @Test
    void twoOrganizersCannotCancelTheSameTicketTwice() throws Exception {

        String organizerEmail = "org@test.dev";
        String buyerEmail = "buyer@test.dev";

        Event event = eventRepo.save(
            new Event(
                null,
                organizerEmail,
                "The Finaleee",
                "Cairo",
                "music",
                5,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))
            )
        );

        // 1. Buyer purchases one ticket
        ticketsService.purchase(buyerEmail, event.id());

        Ticket ticket = ticketRepo
            .findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(buyerEmail)
            .stream()
            .findFirst()
            .orElseThrow();

        Long ticketId = ticket.id();

        // 2. Prepare two competing cancellation requests
        int workers = 2;

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch startGun = new CountDownLatch(1);

        AtomicInteger successfulCancellations = new AtomicInteger();
        AtomicInteger rejectedCancellations = new AtomicInteger();

        try {
            for (int i = 0; i < workers; i++) {
                pool.submit(() -> {
                    ready.countDown();

                    try {
                        startGun.await();

                        ticketsService.cancel(
                            organizerEmail,
                            ticketId,
                            event.id()
                        );

                        successfulCancellations.incrementAndGet();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (RuntimeException e) {
                        // Assumes a repeated cancellation is rejected
                        rejectedCancellations.incrementAndGet();
                    }
                });
            }

            // 3. Wait until both threads are ready
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            // 4. Release both threads together
            startGun.countDown();

            pool.shutdown();

            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS))
                .isTrue();

        } finally {
            pool.shutdownNow();
        }

        // 5. Exactly one cancellation should succeed
        assertThat(successfulCancellations.get()).isEqualTo(1);
        assertThat(rejectedCancellations.get()).isEqualTo(1);

        // 6. Verify the final database state
        Ticket cancelled = ticketRepo.findById(ticketId).orElseThrow();

        assertThat(cancelled.status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(cancelled.cancelledBy()).isEqualTo(organizerEmail);

        assertThat(
            ticketRepo.countByEventIdAndStatus(
                event.id(),
                TicketStatus.PURCHASED
            )
        ).isZero();
    }



    @Test
    void cancellingAlreadyCancelledTicketHasNoAdditionalEffects() {

        String organizerEmail = "org@test.dev";
        String buyerEmail = "buyer@test.dev";

        Event event = eventRepo.save(
            new Event(
                null,
                organizerEmail,
                "AC3 Event",
                "Cairo",
                "music",
                5,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))
            )
        );

        ticketsService.purchase(buyerEmail, event.id());

        Ticket ticket = ticketRepo
            .findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(buyerEmail)
            .stream()
            .findFirst()
            .orElseThrow();

        Long ticketId = ticket.id();

        // Wait for the purchase confirmation
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(
                ticketRepo.findById(ticketId)
                    .orElseThrow()
                    .confirmation_sent_at()
            ).isNotNull()
        );

        // First cancellation
        ticketsService.cancel(organizerEmail, ticketId, event.id());

        // Wait for the cancellation email to be processed
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(
                ticketRepo.findById(ticketId)
                    .orElseThrow()
                    .cancellationEmailSentAt()
            ).isNotNull()
        );

        Ticket beforeRetry = ticketRepo.findById(ticketId).orElseThrow();

        var cancelledAtBefore = beforeRetry.cancelledAt();
        var cancellationEmailSentAtBefore =
            beforeRetry.cancellationEmailSentAt();

        long purchasedCountBefore =
            ticketRepo.countByEventIdAndStatus(
                event.id(), TicketStatus.PURCHASED
            );

        long cancelledCountBefore =
            ticketRepo.countByEventIdAndStatus(
                event.id(), TicketStatus.CANCELLED
            );

        // Second cancellation must be rejected
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            ticketsService.cancel(organizerEmail, ticketId, event.id())
        ).isInstanceOf(RuntimeException.class);

        // Verify that the retry changed nothing
        Ticket afterRetry = ticketRepo.findById(ticketId).orElseThrow();

        assertThat(afterRetry.status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(afterRetry.cancelledAt()).isEqualTo(cancelledAtBefore);
        assertThat(afterRetry.cancellationEmailSentAt())
            .isEqualTo(cancellationEmailSentAtBefore);

        assertThat(
            ticketRepo.countByEventIdAndStatus(
                event.id(), TicketStatus.PURCHASED
            )
        ).isEqualTo(purchasedCountBefore);

        assertThat(
            ticketRepo.countByEventIdAndStatus(
                event.id(), TicketStatus.CANCELLED
            )
        ).isEqualTo(cancelledCountBefore);
    }


    @Test
    void cancelTicketLessThan48HoursBeforeStart() {
    
        String organizerEmail = "org@test.dev";
        String buyerEmail = "buyer-48h@test.dev";
    
        Event event = new Event(
            null,
            organizerEmail,
            "48 Hour Cancellation Test",
            "Cairo",
            "music",
            5,
            Instant.now().plus(Duration.ofHours(24)),
            Instant.now().plus(Duration.ofHours(26))
        );
    
        Event eventAdded = eventRepo.save(event);
    
        ticketsService.purchase(buyerEmail, eventAdded.id());
    
        Ticket ticket = ticketRepo
            .findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(buyerEmail)
            .stream()
            .findFirst()
            .orElseThrow();
    
        Long ticketId = ticket.id();
    
        assertThatThrownBy(() ->
            ticketsService.cancel(
                organizerEmail,
                ticketId,
                eventAdded.id()
            )
        )
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("less than 48 hours");
    
        Ticket unchangedTicket = ticketRepo.findById(ticketId).orElseThrow();
    
        assertThat(unchangedTicket.status())
            .isEqualTo(TicketStatus.PURCHASED);
    
        assertThat(unchangedTicket.cancelledAt()).isNull();
    
        assertThat(
            ticketRepo.countByEventIdAndStatus(
                eventAdded.id(),
                TicketStatus.PURCHASED
            )
        ).isEqualTo(1);
    }


    @Test 
    void fullCycleOfCancellation() throws Exception{
        String token = registerAndLogin("canceller@tazkara.dev");
        // 2. Create an event
        String eventJson = """
            {
                "name": "Cancellation Test",
                "city": "Cairo",
                "category": "music",
                "totalSeats": 10,  
                "startAt": "2027-06-01T18:00:00Z",
                 "endAt": "2027-06-01T21:00:00Z"
            }
            """;

        MvcResult eventResult = mockMvc.perform(
                post("/api/v1/events")
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .content(eventJson)
            )
            .andExpect(status().isCreated())
            .andReturn();

        Integer eventIdValue = JsonPath.read(
            eventResult.getResponse().getContentAsString(),
            "$.id"
        );
        
        Long eventId = eventIdValue.longValue();

        // 3. Purchase a ticket
        MvcResult ticketResult = mockMvc.perform(
                post("/api/v1/events/{id}/tickets", eventId)
                    .header("Authorization", "Bearer " + token)
            )
            .andExpect(status().isCreated())
            .andReturn();


        Integer ticketIdValue = JsonPath.read(
            ticketResult.getResponse().getContentAsString(),
            "$.id"
        );
        Long ticketId = ticketIdValue.longValue();

        // 4. Cancel the ticket for the first time
        mockMvc.perform(
                patch("/api/v1/events/{eventId}/tickets/{ticketId}/cancellation",
                    eventId, ticketId)
                    .header("Authorization", "Bearer " + token)
            )
            .andExpect(status().isOk());

        // 5. Cancel the same ticket again
        mockMvc.perform(
                patch("/api/v1/events/{eventId}/tickets/{ticketId}/cancellation",
                    eventId, ticketId)
                    .header("Authorization", "Bearer " + token)
            )
            // Change this to the status defined by your API contract.
            .andExpect(status().is4xxClientError());

        // 6. Wait for asynchronous analytics processing
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(ticketsSold(eventId)).isZero()
        );
    }
}
