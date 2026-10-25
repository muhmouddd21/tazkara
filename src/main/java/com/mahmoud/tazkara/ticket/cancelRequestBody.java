package com.mahmoud.tazkara.ticket;

import jakarta.validation.constraints.NotBlank;

public record cancelRequestBody(
    @NotBlank 
    Long id,
    @NotBlank 
    Long eventId
) {
    
}
