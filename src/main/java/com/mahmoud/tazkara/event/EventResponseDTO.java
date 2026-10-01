package com.mahmoud.tazkara.event;

public record EventResponseDTO(Long id, String name, String city, String type, int capacity, String organizerEmail) {


    static EventResponseDTO from(Event event) {
        return new EventResponseDTO(event.id(), event.name(), event.city(), event.category(), event.totalSeats(), event.organizerEmail());
    }
}
