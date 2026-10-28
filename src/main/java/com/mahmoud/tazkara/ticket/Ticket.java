package com.mahmoud.tazkara.ticket;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

import com.mahmoud.tazkara.event.Event;

import jakarta.persistence.*;

@Entity 
@Table(name = "tickets")
public class Ticket{

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id",nullable = false)
    private Event event;

    @Column(nullable = false)
    private String buyerEmail;

    @Column(nullable = false)
    private Instant purchasedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.PURCHASED;

    private Instant confirmationSentAt;

    private Instant cancelledAt;

    private Instant cancellationEmailSentAt;

    @Column(length = 255)
    private String cancelledBy;

    protected Ticket() {}

    public Ticket(Event event, String buyerEmail, Instant purchasedAt) {
        this.event = event;
        this.buyerEmail = buyerEmail;
        this.purchasedAt = purchasedAt;
    }

    public Long id() { return id; }
    public Event event() { return event; }
    public String buyerEmail() { return buyerEmail; }
    public Instant purchasedAt() { return purchasedAt; }
    public Instant confirmationSentAt() {
        return confirmationSentAt;
    }

    public TicketStatus status() {
        return status;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }

    public Instant cancellationEmailSentAt() {
        return cancellationEmailSentAt;
    }

    public String cancelledBy() {
        return cancelledBy;
    }

    public void cancel(String cancelledBy, Instant cancelledAt) {
        if (this.status != TicketStatus.PURCHASED) {
            throw new IllegalStateException(
                    "Only purchased tickets can be cancelled"
            );
        }
    
        this.status = TicketStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
    }

}