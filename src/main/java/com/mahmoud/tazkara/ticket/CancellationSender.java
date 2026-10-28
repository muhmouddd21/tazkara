package com.mahmoud.tazkara.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mahmoud.tazkara.config.RabbitConfig;

public interface CancellationSender {
    void send(TicketCancelledMessage message);
        
    } 


@Component 
class LoggingCancellationSender implements CancellationSender {   
    private static final Logger log = LoggerFactory.getLogger(LoggingCancellationSender.class);
    public void send(TicketCancelledMessage m) {
        
            log.info("Cancellation → {} : ticket #{} for {}", m.buyerEmail(), m.ticketId(), m.eventName());
        // throw new EmailDeliveryException("mahmoud test");
    }
}
    
@Component
class CancellationListener {
    private final CancellationService cancelService;

    CancellationListener(CancellationSender sender, CancellationService cancelService) 
    { 
    this.cancelService = cancelService;

    }

    @RabbitListener (queues = RabbitConfig.CANCELLATIONS_QUEUE)
    void onTicketPurchased(TicketCancelledMessage message) {
        this.cancelService.confirm(message);
    }
}
