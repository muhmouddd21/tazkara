package com.mahmoud.tazkara.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class ConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationService.class);
    private final TicketRepo ticketRepo;
    private final ConfirmationSender sender;


    public ConfirmationService(TicketRepo ticketRepo, ConfirmationSender sender) {
        this.ticketRepo = ticketRepo;
        this.sender = sender;
    }


    @Transactional 
    public void confirm(TicketPurchasedMessage message){
        if(ticketRepo.markConfirmationSent(message.ticketId()) == 0){
            log.info("ticket {} already confirmed, duplicate delivery skipped", message.ticketId());
            return;                                  // returns normally → ack → duplicate is gone
        }
        sender.send(message);
    }
}
