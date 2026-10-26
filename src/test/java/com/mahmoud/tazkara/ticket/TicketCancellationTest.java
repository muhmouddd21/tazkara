package com.mahmoud.tazkara.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

@Testcontainers 
@SpringBootTest(properties = {
    "tazkara.messaging.rabbit-enabled=true",
    "spring.rabbitmq.listener.simple.auto-startup=true"
})
@ActiveProfiles("test")
public class TicketCancellationTest {

    @Autowired TicketsService ticketsService;
    @Autowired EventRepo eventRepo;
    @Autowired TicketRepo ticketRepo;

    @Autowired
    EntityManager entityManager;
    
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


    @Test 
    void purchaseThenCancelSendsBothMessages() {

        Event event = eventRepo.save(
            new Event(
                null,
                "org@test.dev",
                "The Finale",
                "Cairo",
                "music",
                5
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
                "The Finale",
                "Cairo",
                "music",
                5
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


}
