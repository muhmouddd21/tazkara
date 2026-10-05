package com.mahmoud.tazkara.ticket;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface TicketRepo extends Repository<Ticket,Long> {
    Ticket save(Ticket ticket);

    long countByEventId(Long eventId);

    List<Ticket> findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(String buyerEmail);
}
