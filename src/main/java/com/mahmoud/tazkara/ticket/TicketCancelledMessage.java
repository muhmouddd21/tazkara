package com.mahmoud.tazkara.ticket;

import java.time.Instant;

public record TicketCancelledMessage(
    Long ticketId,
    Long eventId,
    String eventName,
    String buyerEmail,
    String cancelledBy,
    Instant cancelledAt
) {}
