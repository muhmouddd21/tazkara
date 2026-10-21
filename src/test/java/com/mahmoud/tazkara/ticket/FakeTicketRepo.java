package com.mahmoud.tazkara.ticket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class FakeTicketRepo implements TicketRepo {

    private final Map<Long, Ticket> tickets = new HashMap<>();
    private final AtomicLong counter = new AtomicLong();
    private final Set<Long> confirmdTickers = new HashSet<>();

    @Override
    public Ticket save(Ticket ticket) {
        Long id = ticket.id() != null
                ? ticket.id()
                : counter.incrementAndGet();

        tickets.put(id, ticket);

        return ticket;
    }

    @Override
    public long countByEventId(Long eventId) {
        return tickets.values().stream()
                .filter(ticket -> ticket.event().id().equals(eventId))
                .count();
    }

    @Override
    public List<Ticket> findByBuyerEmailIgnoreCaseOrderByPurchasedAtDesc(
            String buyerEmail) {

        return tickets.values().stream()
                .filter(ticket ->
                        ticket.buyerEmail().equalsIgnoreCase(buyerEmail))
                .sorted((a, b) ->
                        b.purchasedAt().compareTo(a.purchasedAt()))
                .toList();
    }

    @Override
    public int markConfirmationSent(Long ticketId) {
        if(this.confirmdTickers.contains(ticketId)){
                return 0;
        }else{
                this.confirmdTickers.add(ticketId);
                return 1;
        }
        
    }
}