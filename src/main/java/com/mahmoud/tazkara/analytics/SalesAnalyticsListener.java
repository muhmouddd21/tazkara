package com.mahmoud.tazkara.analytics;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.KafkaHeaders;
import com.mahmoud.tazkara.config.KafkaConfig;
import com.mahmoud.tazkara.ticket.TicketPurchasedMessage;

@Component 
public class SalesAnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(SalesAnalyticsListener.class);
    private final SalesProjection projection;
    SalesAnalyticsListener(SalesProjection projection) { 
        this.projection = projection; 
    }


    @KafkaListener (topics = KafkaConfig.TICKET_EVENTS_TOPIC, groupId = "analytics")
    void onTicketEvent(TicketPurchasedMessage message,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset,
                       @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp) {
        log.info("analytics ← partition {} offset {} : ticket {} (event {})",
                 partition, offset, message.ticketId(), message.eventId());
        projection.apply(message, Instant.ofEpochMilli(timestamp));
    }
}
