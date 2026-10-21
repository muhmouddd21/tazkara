package com.mahmoud.tazkara.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TicketRepo extends Repository<Ticket,Long> {
    Ticket save(Ticket ticket);

    long countByEventId(Long eventId);

    List<Ticket> findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(String buyerEmail);


    @Modifying 
    @Query (value = """
        UPDATE tickets SET confirmation_sent_at = now()
        WHERE id = :ticketId AND confirmation_sent_at IS NULL
        """, nativeQuery = true)
    int markConfirmationSent(@Param("ticketId") Long ticketId);
}
