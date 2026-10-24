package com.mahmoud.tazkara.analytics;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mahmoud.tazkara.ticket.TicketPurchasedMessage;

@Service 
public class SalesProjection {
    private final JdbcTemplate jdbc;
    
    public SalesProjection(JdbcTemplate jdbc) { 
        this.jdbc = jdbc; 
    }

    @Transactional 
    public void apply(TicketPurchasedMessage m, Instant soldAt) {
        int fresh = jdbc.update(
            "INSERT INTO analytics_processed_tickets (ticket_id) VALUES (?) ON CONFLICT DO NOTHING",
            m.ticketId());
        if (fresh == 0) return;                               // already counted → duplicate, skip

        jdbc.update("""
            INSERT INTO analytics_event_sales (event_id, tickets_sold, last_sale_at) VALUES (?, 1, ?)
            ON CONFLICT (event_id) DO UPDATE
               SET tickets_sold = analytics_event_sales.tickets_sold + 1,
                   last_sale_at = GREATEST(analytics_event_sales.last_sale_at, EXCLUDED.last_sale_at)
            """, m.eventId(), Timestamp.from(soldAt));
    }
}