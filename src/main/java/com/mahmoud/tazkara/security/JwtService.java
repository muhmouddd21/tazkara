package com.mahmoud.tazkara.security;

import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mahmoud.tazkara.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtService {
    private final SecretKey secretKey;
    private final long expiryMinutes;

    public JwtService(@Value ("${tazkara.jwt.secret}") String secret,
        @Value("${tazkara.jwt.expiry-minutes}") long expiryMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiryMinutes = expiryMinutes;
    }


    public String generate(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.email())
                .claim("plan", user.plan())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMinutes * 60_000))
                .signWith(secretKey)
                .compact();
    }


    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload());
        } catch (Exception e) {
            return Optional.empty();   
        }
    }
}
