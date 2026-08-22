package com.ecommerce.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiHealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "ai-service",
                "status", "UP",
                "message", "AI service infrastructure is running"
        );
    }
}
