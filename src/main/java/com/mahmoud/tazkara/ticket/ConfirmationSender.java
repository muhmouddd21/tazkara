package com.mahmoud.tazkara.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mahmoud.tazkara.config.RabbitConfig;


public interface ConfirmationSender {
void send(TicketPurchasedMessage message);
    
} 
    
@Component 
class LoggingConfirmationSender implements ConfirmationSender {   
    private static final Logger log = LoggerFactory.getLogger(LoggingConfirmationSender.class);
    public void send(TicketPurchasedMessage m) {
        log.info("CONFIRMATION → {} : ticket #{} for {}", m.buyerEmail(), m.ticketId(), m.eventName());
    }
}

// consumer
@Component
class ConfirmationListener {
    private final ConfirmationSender sender;
    ConfirmationListener(ConfirmationSender sender) 
    { 
    this.sender = sender; 
    }

    @RabbitListener (queues = RabbitConfig.CONFIRMATIONS_QUEUE)
    void onTicketPurchased(TicketPurchasedMessage message) {
        sender.send(message);               // returns normally → Spring acks → message deleted
    }
}


@Component
class TicketPurchasedRelay {
    private static final Logger log = LoggerFactory.getLogger(TicketPurchasedRelay.class);
    private final RabbitTemplate rabbit;
    
    TicketPurchasedRelay(RabbitTemplate rabbit) 
    { 
        this.rabbit = rabbit; 
    }

    @TransactionalEventListener 
    void relay(TicketPurchasedMessage message) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PURCHASED_KEY, message);
        } catch (AmqpException e) {
            log.warn("ticket {} committed but its confirmation was not queued", message.ticketId(), e);
        }
    }
}
