package com.mahmoud.tazkara.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mahmoud.tazkara.config.RabbitConfig;


public interface ConfirmationSender {
void send(TicketPurchasedMessage message);
    
} 
    
@Component 
class LoggingConfirmationSender implements ConfirmationSender {   
    private static final Logger log = LoggerFactory.getLogger(LoggingConfirmationSender.class);
    public void send(TicketPurchasedMessage m) {

            log.info("CONFIRMATION → {} : ticket #{} for {}", m.buyerEmail(), m.ticketId(), m.eventName());

        // throw new EmailDeliveryException("mahmoud test");
    }
}

// consumer
@Component
class ConfirmationListener {
    private final ConfirmationService confirmService;
    ConfirmationListener(ConfirmationService confirmService) 
    { 
    this.confirmService = confirmService;

    }

    @RabbitListener (queues = RabbitConfig.CONFIRMATIONS_QUEUE)
    void onTicketPurchased(TicketPurchasedMessage message) {
        confirmService.confirm(message);
    }
}


