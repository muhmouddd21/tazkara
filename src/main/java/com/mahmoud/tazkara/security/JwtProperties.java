package com.mahmoud.tazkara.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@ConfigurationProperties ("tazkara.jwt")
@Validated 
public record JwtProperties(
        @NotBlank(message = "is required — set TAZKARA_JWT_SECRET (see .env.example)")
        @Size(min = 32, message = "must be at least 32 characters (HS256 needs a 256-bit key)")
        String secret,

        @Min (1)@Max(1440)
        int expiryMinutes) {
}
