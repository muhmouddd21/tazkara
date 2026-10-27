package com.mahmoud.tazkara.event;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

// public record Event(Long id,String organizerEmail ,String name, String city, String category, int totalSeats) {}


@Entity 
@Table(name = "events")
public class Event {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)   
    private Long id;


    private String organizerEmail;
    private String name;
    private String city;
    // private String venue;
    private String category;
    private int totalSeats;

    @Version 
    private long version;

    @Column (insertable = false, updatable = false)
    private Instant createdAt;

    @Column(insertable = false, updatable = false)
    private Instant updatedAt;  

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    protected Event() {} 

    public Event(Long id, String organizerEmail, String name,
        String city, String category, int totalSeats) {
        this.id = id;
        this.organizerEmail = organizerEmail;
        this.name = name;
        this.city = city;
        this.category = category;
        this.totalSeats = totalSeats;
        }

        public Event(Long id, String organizerEmail, String name,
            String city, String category, int totalSeats, Instant startAt,Instant endAt) {
            this.id = id;
            this.organizerEmail = organizerEmail;
            this.name = name;
            this.city = city;
            this.category = category;
            this.totalSeats = totalSeats;
            this.startAt =startAt;
            this.endAt =endAt;
            }


    public Long id() { return id; }
    public String organizerEmail() { return organizerEmail; }
    public String name() { return name; }
    public String city() { return city; }
    public String category() { return category; }
    public int totalSeats() { return totalSeats; }
    public Instant createdAt(){return createdAt;}
    public Instant updatedAt(){return updatedAt;}
    public Instant startAt(){return startAt;}
    public Instant endAt(){return endAt;}
    public long version() { return version; }

    public void applyEdit(String name, String city, String category, int totalSeats) {
        this.name = name; this.city = city;
        this.category = category; this.totalSeats = totalSeats;
    }
}

