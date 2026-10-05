package com.mahmoud.tazkara.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.FakeEventRepo;

public class TicketsServiceTest {
    private TicketsService service;
    private FakeEventRepo eventRepo;
    private FakeTicketRepo ticketRepo;

    @BeforeEach
    void setUp() {
        eventRepo = new FakeEventRepo();
        ticketRepo = new FakeTicketRepo();

        service = new TicketsService(ticketRepo, eventRepo);
    }
    
    @Test 
    void purchaseSucceedsWhenSeatsRemain(){ 
        this.eventRepo.save(new Event(1L, "mahmoud@gmail.com", "play","cairo", "music", 100));
        this.service.purchese("ahmed@gmail.com", 1L);

        assertThat(service.myTickets("ahmed@gmail.com")).hasSize(1);
    }


    @Test 
    void purchaseRejectedWhenSoldOut(){ 
        this.eventRepo.save(new Event(1L, "mahmoud@gmail.com", "play","cairo", "music", 1));
        this.service.purchese("ahmed@gmail.com", 1L);
        
        assertThatThrownBy(()-> this.service.purchese("ahmed@gmail.com", 1L)).hasMessageContaining("sold out");
    }

    @Test
    void purchaseFailsForMissingEvent() {

        assertThatThrownBy(() ->
                this.service.purchese("ahmed@gmail.com", 999L)
        )
        .hasMessageContaining("not found");
    }
}
