package com.ecommerce.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtAuthenticationFilter(
            @Value("${security.jwt.secret}") String secret
    ) {
        this.key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(secret)
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();
        String method = request.getMethod();

        return "OPTIONS".equalsIgnoreCase(method)

                // Auth endpoints
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")

                // Public AI health check only
                || ("GET".equalsIgnoreCase(method)
                && path.equals("/api/v1/ai/health"))

                // Public product read APIs only
                || ("GET".equalsIgnoreCase(method)
                && (
                path.equals("/api/v1/products")
                        || path.matches("/api/v1/products/\\d+")
        ))

                // Health endpoint only
                || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing Bearer token"
            );
            return;
        }

        try {

            String token = header.substring(7);

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || role == null) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid token claims"
                );
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            HttpServletRequest wrappedRequest =
                    new UserHeaderRequest(
                            request,
                            userId,
                            role
                    );

            filterChain.doFilter(
                    wrappedRequest,
                    response
            );

        } catch (JwtException | IllegalArgumentException e) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token"
            );

        } finally {

            SecurityContextHolder.clearContext();
        }
    }

    static class UserHeaderRequest extends HttpServletRequestWrapper {

        private final String userId;
        private final String userRole;

        UserHeaderRequest(
                HttpServletRequest request,
                String userId,
                String userRole
        ) {
            super(request);
            this.userId = userId;
            this.userRole = userRole;
        }

        @Override
        public String getHeader(String name) {

            if ("X-User-Id".equalsIgnoreCase(name)) {
                return userId;
            }

            if ("X-User-Role".equalsIgnoreCase(name)) {
                return userRole;
            }

            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {

            if ("X-User-Id".equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(userId));
            }

            if ("X-User-Role".equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(userRole));
            }

            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {

            Set<String> headerNames = new LinkedHashSet<>(
                    Collections.list(super.getHeaderNames())
            );

            headerNames.add("X-User-Id");
            headerNames.add("X-User-Role");

            return Collections.enumeration(
                    new ArrayList<>(headerNames)
            );
        }
    }
}