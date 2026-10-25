package com.mahmoud.tazkara.ticket;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;
import static org.awaitility.Awaitility.await;
import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;


@Testcontainers 
@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.auto-startup=true",
    "tazkara.messaging.rabbit-enabled=true"
}
)
@ActiveProfiles("test")
public class TicketConfirmationFlowTest {
    @Container 
    @ServiceConnection 
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Container 
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management");


    @MockitoBean 
    ConfirmationSender confirmationSender;   // org.springframework.test.context.bean.override.mockito

    @Autowired TicketsService ticketsService;
    @Autowired EventRepo eventRepo;
    @Autowired TicketRepo ticketRepo;
    @Autowired TransactionTemplate tx;


    @Test 
    void purchaseSendsConfirmationAsynchronously() {
        Event event = eventRepo.save(new Event(null, "org@test.dev", "Queue Night", "Cairo", "music", 5));

        TicketResponseDTO ticket = ticketsService.purchase("fan@test.dev", event.id());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            verify(confirmationSender).send(argThat(m ->
                m.ticketId().equals(ticket.id()) && m.buyerEmail().equals("fan@test.dev"))));
    }

    @Test
    void rolledBackPurchaseSendsNoConfirmation() {
        Event event = eventRepo.save(new Event(null, "org@test.dev", "Ghost Gig", "Cairo", "music", 5));

        tx.executeWithoutResult(status -> {                
            ticketsService.purchase("ghost@test.dev", event.id());
            status.setRollbackOnly();                  
        });

        assertThat(ticketRepo.countByEventId(event.id())).isZero();   // the ticket never existed
        verify(confirmationSender, after(2000).never())
            .send(argThat(m -> m.buyerEmail().equals("ghost@test.dev")));
    }

}
