package com.mahmoud.tazkara.ticket;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class FakeTicketRepo implements TicketRepo {

    private final Map<Long, Ticket> tickets = new HashMap<>();
    private final AtomicLong counter = new AtomicLong();
    private final Set<Long> confirmdTickers = new HashSet<>();
    private final Set<Long> cancellationSentTickets = new HashSet<>();

    @Override
    public Ticket save(Ticket ticket) {
        Long id = ticket.id() != null
                ? ticket.id()
                : counter.incrementAndGet();

        tickets.put(id, ticket);

        return ticket;
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
        @Override
        public long countByEventIdAndStatus(Long eventId, TicketStatus status) {
        return tickets.values().stream()
                .filter(ticket -> ticket.event().id().equals(eventId))
                .filter(ticket -> ticket.status() == status)
                .count();
        }

        @Override
        public Optional<Ticket> findById(Long id) {
                return Optional.ofNullable(tickets.get(id));
        }

        @Override
        public int markCancellationSent(Long ticketId) {
                Ticket ticket = tickets.get(ticketId);

                if (ticket == null
                        || ticket.status() != TicketStatus.CANCELLED
                        || cancellationSentTickets.contains(ticketId)) {
                return 0;
                }

                cancellationSentTickets.add(ticketId);
                return 1;
        }

        @Override
        public int cancelTicket(
                Long ticketId,
                String cancelledBy,
                Instant cancelledAt
                ) {
                        Ticket ticket = tickets.get(ticketId);

                        if (ticket == null || ticket.status() != TicketStatus.PURCHASED) {
                        return 0;
                        }

                        ticket.cancel(cancelledBy, cancelledAt);
                        return 1;
        }


        @Override
        public long countByEventId(Long eventId) {
            return tickets.values().stream()
                    .filter(ticket -> ticket.event().id().equals(eventId))
                    .count();
        }
}