package com.product_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomCacheErrorHandlerTest {

    private final CustomCacheErrorHandler handler = new CustomCacheErrorHandler();
    private final Cache cache = mock(Cache.class);

    @Test
    void shouldHandleGetErrorGracefullyWithoutThrowing() {
        when(cache.getName()).thenReturn("products");
        RuntimeException redisError = new RuntimeException("Redis connection refused");

        assertDoesNotThrow(() -> handler.handleCacheGetError(redisError, cache, 123));
    }

    @Test
    void shouldHandlePutErrorGracefullyWithoutThrowing() {
        when(cache.getName()).thenReturn("products");
        RuntimeException redisError = new RuntimeException("Redis timeout on PUT");

        assertDoesNotThrow(() -> handler.handleCachePutError(redisError, cache, 123, "val"));
    }

    @Test
    void shouldHandleEvictErrorGracefullyWithoutThrowing() {
        when(cache.getName()).thenReturn("products");
        RuntimeException redisError = new RuntimeException("Redis cluster down");

        assertDoesNotThrow(() -> handler.handleCacheEvictError(redisError, cache, 123));
    }

    @Test
    void shouldHandleClearErrorGracefullyWithoutThrowing() {
        when(cache.getName()).thenReturn("products");
        RuntimeException redisError = new RuntimeException("Redis OOM");

        assertDoesNotThrow(() -> handler.handleCacheClearError(redisError, cache));
    }
}
