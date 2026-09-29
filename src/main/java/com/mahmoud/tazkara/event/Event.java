package com.mahmoud.tazkara.event;

public record Event(Long id,String organizerEmail ,String name, String city, String category, int totalSeats) {}

