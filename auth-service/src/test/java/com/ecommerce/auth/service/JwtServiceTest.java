package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.AppUser;
import com.ecommerce.auth.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void createsTokenWithCustomerIdentityAndExpiry() throws Exception {
        AppUser user = user(7L);
        String token = new JwtService(SECRET, 30).create(user);

        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET))).build().parseSignedClaims(token).getPayload();
        assertEquals("7", claims.getSubject());
        assertEquals("CUSTOMER", claims.get("role", String.class));
        assertEquals(30, (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 60_000L);
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        String token = new JwtService(SECRET, -1).create(user(7L));
        assertThrows(Exception.class, () -> Jwts.parser().verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET))).build().parseSignedClaims(token));
    }

    private AppUser user(Long id) throws Exception {
        AppUser user = new AppUser();
        Field field = AppUser.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
