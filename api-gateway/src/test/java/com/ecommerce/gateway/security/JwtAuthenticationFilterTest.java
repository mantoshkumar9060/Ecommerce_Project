package com.ecommerce.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(Base64.getEncoder().encodeToString("this-is-a-long-enough-secret-for-hmac-sha-key".getBytes()));

    @Test
    void publicHealthIsSkipped() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/api/v1/ai/health");
        when(request.getMethod()).thenReturn("GET");
        org.junit.jupiter.api.Assertions.assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void protectedRequestWithoutBearerTokenIsRejected() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        filter.doFilterInternal(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Bearer token");
        verifyNoInteractions(chain);
    }
}
