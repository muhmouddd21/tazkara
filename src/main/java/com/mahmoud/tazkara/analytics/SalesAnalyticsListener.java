package com.mahmoud.tazkara.analytics;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.KafkaHeaders;
import com.mahmoud.tazkara.config.KafkaConfig;
import com.mahmoud.tazkara.ticket.TicketCancelledMessage;
import com.mahmoud.tazkara.ticket.TicketPurchasedMessage;

@Component 
@KafkaListener(topics = KafkaConfig.TICKET_EVENTS_TOPIC, groupId = "analytics")
public class SalesAnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(SalesAnalyticsListener.class);
    private final SalesProjection projection;
    SalesAnalyticsListener(SalesProjection projection) { 
        this.projection = projection; 
    }

    
    @KafkaHandler 
    void onTicketPurchasedEvent(TicketPurchasedMessage message,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.OFFSET) long offset,
                                @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp) {
        log.info("analytics ← partition {} offset {} : ticket {} purchased (event {})",
                    partition, offset, message.ticketId(), message.eventId());
        projection.applyPurchased(message, Instant.ofEpochMilli(timestamp));
    }

    @KafkaHandler
    void onTicketCancelledEvent(TicketCancelledMessage message,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.OFFSET) long offset,
                                @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp) {
        log.info("analytics ← partition {} offset {} : ticket {} cancelled (event {})",
                    partition, offset, message.ticketId(), message.eventId());
        projection.applyCancelled(message, Instant.ofEpochMilli(timestamp));
    }

    @KafkaHandler(isDefault = true)
    void unknown(Object message) {
        log.warn("Unknown message type: {}", message.getClass().getName());
    }
    

}
