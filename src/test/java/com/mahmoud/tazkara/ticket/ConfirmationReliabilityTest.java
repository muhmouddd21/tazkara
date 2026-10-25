package com.mahmoud.tazkara.ticket;


import java.sql.Timestamp;
import java.time.Duration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.mahmoud.tazkara.config.RabbitConfig;
import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.after;

@Testcontainers
@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.auto-startup=true",
    "spring.rabbitmq.listener.simple.retry.enabled=true",
    "spring.rabbitmq.listener.simple.retry.max-retries=2",          // 3 attempts in total
    "spring.rabbitmq.listener.simple.retry.initial-interval=100ms", // fast for tests
    "spring.rabbitmq.listener.simple.retry.multiplier=2",
    "tazkara.messaging.rabbit-enabled=true"
})
@ActiveProfiles("test")
public class ConfirmationReliabilityTest {
    @Container 
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management");


    @MockitoBean ConfirmationSender sender;
    @Autowired TicketsService ticketsService;
    @Autowired EventRepo eventRepo;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired JdbcTemplate jdbc;


    private TicketResponseDTO buyTicket(String eventName, String buyer) {
        Event event = eventRepo.save(new Event(null, "org@test.dev", eventName, "Cairo", "music", 10));
        return ticketsService.purchase(buyer, event.id());
    }

    private Timestamp confirmationSentAt(Long ticketId) {
        return jdbc.queryForObject("SELECT confirmation_sent_at FROM tickets WHERE id = ?", Timestamp.class, ticketId);
    }

    @Test 
    void transientFailureIsRetriedThenSucceeds(){
        doThrow(new EmailDeliveryException("smtp timeout")).doNothing().when(sender).send(any());

        TicketResponseDTO ticket = buyTicket("Retry Night", "retry@test.dev");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(sender, times(2)).send(any());                
            assertThat(confirmationSentAt(ticket.id())).isNotNull();
        });
    }


    @Test
    void permanentFailureEndsInTheDeadLetterQueue() {
        doThrow(new EmailDeliveryException("provider down")).when(sender).send(any());

        TicketResponseDTO ticket = buyTicket("Doomed Gig", "doomed@test.dev");

        Message dead = rabbitTemplate.receive(RabbitConfig.CONFIRMATIONS_DLQ, 10_000);   // wait up to 10s
        assertThat(dead).isNotNull();
        assertThat(new String(dead.getBody())).contains("\"ticketId\":" + ticket.id());
        verify(sender, times(3)).send(any());                     // 1 attempt + max-retries(2)
        assertThat(confirmationSentAt(ticket.id())).isNull();     // never claims an email it didn't send
    }

    @Test
    void duplicateDeliverySendsExactlyOneEmail() {
        TicketResponseDTO ticket = buyTicket("Echo Fest", "echo@test.dev");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(sender).send(any()));

        // simulate a redelivery: the same message again (what a crash before ack, or an outbox republish, produces)
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PURCHASED_KEY,
            new TicketPurchasedMessage(ticket.id(), ticket.eventId(), "Echo Fest", "echo@test.dev"));

        verify(sender, after(2000).times(1)).send(any());         // still exactly one
    }
}
