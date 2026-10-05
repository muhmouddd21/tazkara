package com.mahmoud.tazkara.ticket;

import java.time.Instant;

public record TicketResponseDTO(Long id, Long eventId, String eventName, String buyerEmail, Instant purchasedAt) {
    
    static TicketResponseDTO from(Ticket ticket) {
        return new TicketResponseDTO(ticket.id(), ticket.event().id(),
                ticket.event().name(), ticket.buyerEmail(), ticket.purchasedAt());
    }
}
