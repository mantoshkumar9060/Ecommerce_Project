package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.AppUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(@Value("${security.jwt.secret}") String secret, @Value("${security.jwt.expiration-minutes}") long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expiryMinutes = expiryMinutes;
    }

    public String create(AppUser u) {
        Instant now = Instant.now();
        return Jwts.builder().subject(u.getId().toString()).claim("role", u.getRole().name()).issuedAt(Date.from(now)).expiration(Date.from(now.plus(Duration.ofMinutes(expiryMinutes)))).signWith(key).compact();
    }
}
