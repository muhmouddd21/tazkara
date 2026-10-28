package com.mahmoud.tazkara.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mahmoud.tazkara.config.RabbitConfig;

@Component 
@ConditionalOnProperty (name = "tazkara.messaging.rabbit-enabled", havingValue = "true", matchIfMissing = true)
public class TicketCancelledRelay {
    private static final Logger log = LoggerFactory.getLogger(TicketCancelledRelay.class);
    private final RabbitTemplate rabbit;

    TicketCancelledRelay(RabbitTemplate rabbit) 
    { 
        this.rabbit = rabbit; 
    }

    @TransactionalEventListener 
    void relay(TicketCancelledMessage message) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.CANCELLED_KEY, message);
        } catch (AmqpException e) {
            log.warn("ticket {} committed but its Cancellation was not queued", message.ticketId(), e);
        }
    }

}
