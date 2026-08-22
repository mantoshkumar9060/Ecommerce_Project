package com.ecommerce.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String secret) {
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        String path = r.getServletPath();
        String method = r.getMethod();

        return "OPTIONS".equalsIgnoreCase(method)
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")

                || ("GET".equalsIgnoreCase(method)
                && path.equals("/api/v1/ai/health"))

                || ("POST".equalsIgnoreCase(method)
                && path.equals("/api/v1/ai/ingest/products"))

                || ("GET".equalsIgnoreCase(method)
                && path.equals("/api/v1/ai/ingest/status"))

                || ("GET".equalsIgnoreCase(method)
                && path.startsWith("/api/v1/products/"))

                || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Bearer token");
            return;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
            var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of(() -> "ROLE_" + claims.get("role", String.class)));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(new UserHeaderRequest(req, claims.getSubject(), claims.get("role", String.class)), res);
        } catch (JwtException | IllegalArgumentException e) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    static class UserHeaderRequest extends HttpServletRequestWrapper {
        private final Map<String, String> added;

        UserHeaderRequest(HttpServletRequest r, String id, String role) {
            super(r);
            added = Map.of("X-User-Id", id, "X-User-Role", role);
        }

        public String getHeader(String n) {
            return added.containsKey(n) ? added.get(n) : super.getHeader(n);
        }

        public Enumeration<String> getHeaders(String n) {
            return added.containsKey(n) ? Collections.enumeration(List.of(added.get(n))) : super.getHeaders(n);
        }

        public Enumeration<String> getHeaderNames() {
            Set<String> s = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            s.addAll(added.keySet());
            return Collections.enumeration(s);
        }
    }
}
