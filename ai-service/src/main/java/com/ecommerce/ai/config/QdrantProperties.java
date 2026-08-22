package com.ecommerce.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qdrant")
public record QdrantProperties(String host, int port, String collection) {
    public String baseUrl() { return "http://" + host + ":" + port; }
}
