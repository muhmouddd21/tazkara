package com.mahmoud.tazkara.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mahmoud.tazkara.config.KafkaConfig;

@Component 
@ConditionalOnProperty (name = "tazkara.messaging.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class TicketEventsKafkaRelay {
    private static final Logger log = LoggerFactory.getLogger(TicketEventsKafkaRelay.class);
    private final KafkaTemplate<String, Object> kafka;


    TicketEventsKafkaRelay(KafkaTemplate<String, Object> kafka) { 
        this.kafka = kafka; 
    }


    @TransactionalEventListener 
    void publish(TicketPurchasedMessage message) {
        String key = String.valueOf(message.eventId());

        try {
            kafka.send(KafkaConfig.TICKET_EVENTS_TOPIC,key,message)
            .whenComplete((result, ex) -> {
                if (ex != null) log.warn("ticket {} event not published to Kafka", message.ticketId(), ex);
            });
        } catch (Exception e) {
            log.warn("ticket {} event not published to Kafka", message.ticketId(), e);
        }
    }
}
