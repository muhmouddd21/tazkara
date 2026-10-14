package com.mahmoud.tazkara.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateEventRequest(
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
    @NotNull Long version

) {}
    