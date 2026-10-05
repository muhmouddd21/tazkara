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
}