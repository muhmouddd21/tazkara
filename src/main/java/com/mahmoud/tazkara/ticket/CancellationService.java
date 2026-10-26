package com.mahmoud.tazkara.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class CancellationService {
    private static final Logger log = LoggerFactory.getLogger(CancellationService.class);
    private final TicketRepo ticketRepo;
    private final CancellationSender sender;


    public CancellationService(TicketRepo ticketRepo, CancellationSender sender) {
        this.ticketRepo = ticketRepo;
        this.sender = sender;
    }

    @Transactional 
    public void confirm(TicketCancelledMessage message){
        if(this.ticketRepo.markCancellationSent(message.ticketId()) ==0){
            log.info("ticket {} already confirmed, duplicate delivery skipped", message.ticketId());
            return;  
        }
        sender.send(message);
    }
}
