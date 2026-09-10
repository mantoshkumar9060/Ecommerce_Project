package com.product_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Fail-Open cache error handler.
 * If Redis is down, unreachable, or times out, errors are logged as warnings
 * and execution falls back to the database instead of failing client requests with HTTP 500.
 */
public class CustomCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis GET failure on cache '{}' for key '{}': {}. Falling back to database.",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis PUT failure on cache '{}' for key '{}': {}. Proceeding without caching.",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis EVICT failure on cache '{}' for key '{}': {}. Proceeding without cache eviction.",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis CLEAR failure on cache '{}': {}. Proceeding without cache clear.",
                cache.getName(), exception.getMessage());
    }
}
