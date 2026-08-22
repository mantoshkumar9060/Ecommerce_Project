package com.ecommerce.ai.service;

import com.ecommerce.ai.config.AiProperties;
import com.ecommerce.ai.config.QdrantProperties;
import com.ecommerce.ai.exception.AiServiceException;
import com.ecommerce.ai.dto.AiDtos.ProductResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class QdrantService {
    private final QdrantProperties qdrant; private final AiProperties ai; private final RestClient client = RestClient.create();
    public QdrantService(QdrantProperties qdrant, AiProperties ai) { this.qdrant = qdrant; this.ai = ai; }
    public void ensureCollection() {
        if (exists()) return;
        try { client.put().uri(url()).body(Map.of("vectors", Map.of("size", ai.embeddingDimensions(), "distance", "Cosine"))).retrieve().toBodilessEntity(); }
        catch (Exception error) { throw unavailable(); }
    }
    public void upsert(List<Point> points) {
        ensureCollection();
        try { client.put().uri(url() + "/points?wait=true").body(Map.of("points", points.stream().map(point -> Map.of("id", point.id(), "vector", point.vector(), "payload", point.payload())).toList())).retrieve().toBodilessEntity(); }
        catch (Exception error) { throw unavailable(); }
    }
    public List<ProductResult> search(List<Double> vector, int limit) {
        ensureCollection();
        try {
            Map response = client.post().uri(url() + "/points/query").body(Map.of("query", vector, "limit", limit, "with_payload", true)).retrieve().body(Map.class);
            List<Map> points = (List<Map>) ((Map) response.get("result")).get("points");
            return points.stream().map(point -> { Map payload = (Map) point.get("payload"); return new ProductResult(((Number) payload.get("productId")).intValue(), ((Number) payload.get("brandId")).intValue(), String.valueOf(payload.get("productName")), String.valueOf(payload.get("brandName")), String.valueOf(payload.get("category")), new BigDecimal(String.valueOf(payload.get("price"))), ((Number) point.get("score")).doubleValue()); }).toList();
        } catch (AiServiceException error) { throw error; } catch (Exception error) { throw unavailable(); }
    }
    public long vectorCount() {
        if (!exists()) return 0;
        try { Map response = client.get().uri(url()).retrieve().body(Map.class); return ((Number) ((Map) response.get("result")).getOrDefault("points_count", 0)).longValue(); }
        catch (Exception error) { throw unavailable(); }
    }
    public boolean exists() {
        try {
            client.get().uri(url()).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound error) {
            return false;
        } catch (Exception error) {
            throw unavailable();
        }
    }
    public String deterministicId(int productId, int brandId) { return UUID.nameUUIDFromBytes(("product:" + productId + ":brand:" + brandId).getBytes(StandardCharsets.UTF_8)).toString(); }
    private String url() { return qdrant.baseUrl() + "/collections/" + qdrant.collection(); }
    private AiServiceException unavailable() { return new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Vector search service is temporarily unavailable"); }
    public record Point(String id, List<Double> vector, Map<String, Object> payload) { }
}
