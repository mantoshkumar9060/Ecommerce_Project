package com.ecommerce.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(String apiKey, String baseUrl, String chatModel, String embeddingModel, int embeddingDimensions) {
    public boolean configured() { return apiKey != null && !apiKey.isBlank(); }
}
