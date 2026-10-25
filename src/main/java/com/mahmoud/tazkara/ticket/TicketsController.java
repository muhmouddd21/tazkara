package com.mahmoud.tazkara.ticket;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping ("/api/v1/tickets")
public class TicketsController {
    private final TicketsService ticketsService;

    TicketsController(TicketsService ticketsService) {
        this.ticketsService = ticketsService;
    }

    @GetMapping
    public List<TicketResponseDTO> getTickets(Authentication authentication) {
        String email = authentication.getName();

        return this.ticketsService.myTickets(email);
    }
    @PostMapping("/id")
    public String cancelATicket(@RequestBody @Valid cancelRequestBody request,Authentication authentication) {

        
        request.id
        return entity;
    }
    

}
