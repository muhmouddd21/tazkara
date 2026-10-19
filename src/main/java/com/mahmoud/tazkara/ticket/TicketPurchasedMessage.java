package com.mahmoud.tazkara.ticket;

public record TicketPurchasedMessage (Long ticketId, Long eventId, String eventName, String buyerEmail){};

