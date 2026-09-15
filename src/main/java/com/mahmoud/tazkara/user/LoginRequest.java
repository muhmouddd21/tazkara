package com.mahmoud.tazkara.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank 
    String email,
    @NotBlank 
    String password
) {
    
}
