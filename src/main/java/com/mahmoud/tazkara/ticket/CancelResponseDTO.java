package com.mahmoud.tazkara.ticket;

import java.time.Instant;

public record CancelResponseDTO(
    Long ticketId, Long eventId, TicketStatus status,Instant cancelledAt,String cancelledBy
) {
    
}
