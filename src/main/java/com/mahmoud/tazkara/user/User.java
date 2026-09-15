package com.mahmoud.tazkara.user;

public record User(
    Long id,
    String email,
    String passwordHash,
    String plan
) {
    
}
