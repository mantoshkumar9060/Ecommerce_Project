package com.ecommerce.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public final class AiDtos {
    private AiDtos() { }
    public record SearchRequest(@NotBlank String query, @Min(1) @Max(10) Integer limit) { public int safeLimit() { return limit == null ? 5 : limit; } }
    public record ChatRequest(@NotBlank String question, @Min(1) @Max(10) Integer limit) { public int safeLimit() { return limit == null ? 5 : limit; } }
    public record ProductResult(Integer productId, Integer brandId, String productName, String brandName, String category, BigDecimal price, double score) { }
    public record SearchResponse(String query, List<ProductResult> results) { }
    public record ChatResponse(String answer, List<ProductResult> products) { }
    public record IngestionResponse(String status, int productsFetched, int productsIndexed, int productsSkipped, String collection) { }
    public record IngestionStatus(String collection, boolean collectionExists, long vectorCount) { }
}
