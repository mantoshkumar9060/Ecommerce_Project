package com.ecommerce.ai.service;

import com.ecommerce.ai.config.AiProperties;
import com.ecommerce.ai.exception.AiServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiClient {
    private final AiProperties properties;
    private final RestClient client = RestClient.create();
    public OpenAiClient(AiProperties properties) { this.properties = properties; }
    public List<List<Double>> embed(List<String> inputs) {
        requireConfigured();
        try {
            Map response = client.post().uri(properties.baseUrl() + "/embeddings").header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey()).body(Map.of("model", properties.embeddingModel(), "input", inputs)).retrieve().body(Map.class);
            return ((List<Map>) response.get("data")).stream().map(item -> (List<Double>) item.get("embedding")).toList();
        } catch (Exception error) { throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI embedding provider is temporarily unavailable"); }
    }
    public String chat(String prompt) {
        requireConfigured();
        try {
            Map response = client.post().uri(properties.baseUrl() + "/chat/completions").header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey()).body(Map.of("model", properties.chatModel(), "temperature", 0.2, "messages", List.of(Map.of("role", "system", "content", "You are a grounded shopping assistant. Use only supplied product context. Never invent products, prices, brands, features, or availability. If context is insufficient, say so clearly."), Map.of("role", "user", "content", prompt)))).retrieve().body(Map.class);
            Map choice = ((List<Map>) response.get("choices")).get(0);
            return String.valueOf(((Map) choice.get("message")).get("content"));
        } catch (Exception error) { throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI chat provider is temporarily unavailable"); }
    }
    private void requireConfigured() { if (!properties.configured()) throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI embedding provider is not configured"); }
}
