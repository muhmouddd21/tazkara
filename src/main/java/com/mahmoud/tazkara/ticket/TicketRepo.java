package com.mahmoud.tazkara.ticket;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TicketRepo extends Repository<Ticket,Long> {
    Ticket save(Ticket ticket);

    long countByEventId(Long eventId);
    long countByEventIdAndStatus(Long eventId, TicketStatus status);
    List<Ticket> findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(String buyerEmail);

    Optional<Ticket> findById(Long id);
    @Modifying 
    @Query (value = """
        UPDATE tickets SET confirmation_sent_at = now()
        WHERE id = :ticketId AND confirmation_sent_at IS NULL
        """, nativeQuery = true)
    int markConfirmationSent(@Param("ticketId") Long ticketId);

    @Modifying 
    @Query (value ="""
           UPDATE tickets SET cancellation_email_sent_at = now()
        WHERE id = :ticketId AND cancellation_email_sent_at IS NULL
            
            """,nativeQuery =true)
    int markCancellationSent(@Param("ticketId") Long ticketId);


    
    @Modifying
    @Query(value = """
        UPDATE tickets
        SET status = 'CANCELLED',
            cancelled_at = :cancelledAt,
            cancelled_by = :cancelledBy
        WHERE id = :ticketId
          AND status = 'PURCHASED'
        """, nativeQuery = true)
    int cancelTicket(
        @Param("ticketId") Long ticketId,
        @Param("cancelledBy") String cancelledBy,
        @Param("cancelledAt") Instant cancelledAt
    );

}
