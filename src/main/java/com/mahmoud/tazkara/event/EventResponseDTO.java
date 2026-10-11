package com.mahmoud.tazkara.event;

import java.time.Instant;

public record EventResponseDTO(Long id, String name, String city, String type, int capacity, String organizerEmail,Instant createdAt,Instant updatedAt) {


    static EventResponseDTO from(Event event) {
        return new EventResponseDTO(event.id(), event.name(), event.city(), event.category(), event.totalSeats(), event.organizerEmail(),event.createdAt(),event.updatedAt());
    }
}
