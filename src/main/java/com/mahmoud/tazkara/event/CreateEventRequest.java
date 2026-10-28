package com.mahmoud.tazkara.event;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateEventRequest(
    @NotBlank (message = "Name is required")
    String name,
    @NotBlank(message = "City is required")
    String city,
    @NotBlank(message = "Category is required")
    @Pattern (regexp = "^[a-zA-Z]+$", message = "Category must be a valid category name")
    @ValidCategory 
    String category,
    @Min (value = 1, message = "Total seats must be at least 1")
    @Max (value = 1000, message = "Total seats must be less than 1000")
    int totalSeats,
    @NotNull (message = "Start date and time is required")
    @Future(message = "Start date and time must be in the future")
    Instant startAt,

    @NotNull(message = "End date and time is required")
    @Future(message = "End date and time must be in the future")
    Instant endAt
) {
    @AssertTrue(message = "End date and time must be after start date and time")
    public boolean isEndAfterStart() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }
}