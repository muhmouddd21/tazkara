package com.mahmoud.tazkara.ticket;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.mahmoud.tazkara.config.RabbitConfig;
import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
public class CancellationReliabilityTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management");

    @MockitoBean CancellationSender sender;
    @Autowired TicketsService ticketsService;
    @Autowired EventRepo eventRepo;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired JdbcTemplate jdbc;

    private Timestamp cancellationEmailSentAt(Long ticketId) {
        return jdbc.queryForObject("SELECT cancellation_email_sent_at FROM tickets WHERE id = ?", Timestamp.class, ticketId);
    }

    @Test
    void permanentFailureEndsInTheCancellationDeadLetterQueue() {      // B2's guard
        doThrow(new EmailDeliveryException("provider down")).when(sender).send(any());

        Event event = eventRepo.save(new Event(null, "org@test.dev", "Doomed Cancel", "Cairo", "music", 10,
                Instant.now().plus(Duration.ofDays(30)), Instant.now().plus(Duration.ofDays(33))));
        TicketResponseDTO ticket = ticketsService.purchase("doomed@test.dev", event.id());
        ticketsService.cancel("org@test.dev", ticket.id(), event.id());

        Message dead = rabbitTemplate.receive(RabbitConfig.CANCELLATIONS_DLQ, 10_000);   // wait up to 10s
        assertThat(dead).isNotNull();
        assertThat(new String(dead.getBody())).contains("\"ticketId\":" + ticket.id());
        verify(sender, times(3)).send(any());                         // 1 attempt + max-retries(2)
        assertThat(cancellationEmailSentAt(ticket.id())).isNull();    // never claims an email it didn't send
    }
}
