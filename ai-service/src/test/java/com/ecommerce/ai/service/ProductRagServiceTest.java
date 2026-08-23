package com.ecommerce.ai.service;

import com.ecommerce.ai.client.ProductClient;
import com.ecommerce.ai.config.QdrantProperties;
import com.ecommerce.ai.dto.AiDtos.ProductResult;
import com.ecommerce.ai.dto.AiDtos.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductRagServiceTest {

    private ProductClient productClient;
    private OpenAiClient openAiClient;
    private QdrantService qdrantService;
    private QdrantProperties qdrantProperties;
    private ProductRagService productRagService;

    @BeforeEach
    void setUp() {
        productClient = mock(ProductClient.class);
        openAiClient = mock(OpenAiClient.class);
        qdrantService = mock(QdrantService.class);
        qdrantProperties = mock(QdrantProperties.class);

        productRagService = new ProductRagService(
                productClient,
                openAiClient,
                qdrantService,
                qdrantProperties
        );
    }

    @Test
    void shouldFilterProductsByBudget() {

        when(openAiClient.embed(anyList()))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        List<ProductResult> products = List.of(
                product(1, "Classic Sneakers", "Puma", "Footwear", 2799, 0.90),
                product(2, "Premium Sneakers", "Nike", "Footwear", 4999, 0.85),
                product(3, "Budget Shoes", "HRX", "Footwear", 1599, 0.80)
        );

        when(qdrantService.search(anyList(), anyInt()))
                .thenReturn(products);

        SearchResponse response =
                productRagService.search("shoes under 3000", 5);

        assertEquals(2, response.results().size());

        assertTrue(
                response.results().stream()
                        .allMatch(product ->
                                product.price()
                                        .compareTo(BigDecimal.valueOf(3000)) <= 0
                        )
        );
    }

    @Test
    void shouldFilterProductsByBrand() {

        when(openAiClient.embed(anyList()))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        List<ProductResult> products = List.of(
                product(1, "Classic Sneakers", "Puma", "Footwear", 2799, 0.90),
                product(2, "Court Vision Sneakers", "Nike", "Footwear", 4299, 0.85),
                product(3, "Crew Neck Sweatshirt", "Puma", "Men", 1799, 0.80)
        );

        when(qdrantService.search(anyList(), anyInt()))
                .thenReturn(products);

        SearchResponse response =
                productRagService.search("Puma products", 5);

        assertEquals(2, response.results().size());

        assertTrue(
                response.results().stream()
                        .allMatch(product ->
                                product.brandName().equals("Puma")
                        )
        );
    }

    @Test
    void shouldFilterShoeProducts() {

        when(openAiClient.embed(anyList()))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        List<ProductResult> products = List.of(
                product(1, "Running Shoes", "Puma", "Footwear", 3199, 0.90),
                product(2, "Everyday Ballet Flats", "Metro", "Footwear", 1599, 0.85),
                product(3, "Canvas Sneakers", "HRX", "Footwear", 1599, 0.80)
        );

        when(qdrantService.search(anyList(), anyInt()))
                .thenReturn(products);

        SearchResponse response =
                productRagService.search("running shoes", 5);

        assertEquals(2, response.results().size());

        assertTrue(
                response.results().stream()
                        .noneMatch(product ->
                                product.productName()
                                        .toLowerCase()
                                        .contains("flat")
                        )
        );
    }

    @Test
    void shouldRejectLowRelevanceForNormalSearch() {

        when(openAiClient.embed(anyList()))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        List<ProductResult> products = List.of(
                product(1, "Running Shoes", "Puma", "Footwear", 3199, 0.90),
                product(2, "Unrelated Product", "Brand", "Men", 1000, 0.30)
        );

        when(qdrantService.search(anyList(), anyInt()))
                .thenReturn(products);

        SearchResponse response =
                productRagService.search("running shoes", 5);

        assertEquals(1, response.results().size());
        assertEquals(
                "Running Shoes",
                response.results().get(0).productName()
        );
    }

    @Test
    void shouldFilterByBrandAndBudgetTogether() {

        when(openAiClient.embed(anyList()))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        List<ProductResult> products = List.of(
                product(1, "Classic Sneakers", "Puma", "Footwear", 2799, 0.90),
                product(2, "Premium Sneakers", "Puma", "Footwear", 3999, 0.85),
                product(3, "Budget Shoes", "HRX", "Footwear", 1599, 0.80)
        );

        when(qdrantService.search(anyList(), anyInt()))
                .thenReturn(products);

        SearchResponse response =
                productRagService.search("Puma products under 3000", 5);

        assertEquals(1, response.results().size());

        ProductResult result = response.results().get(0);

        assertEquals("Puma", result.brandName());
        assertTrue(
                result.price()
                        .compareTo(BigDecimal.valueOf(3000)) <= 0
        );
    }

    @Test
    void shouldRespectRequestedLimit() {

        when(openAiClient.embed(anyList()))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        List<ProductResult> products = List.of(
                product(1, "Running Shoes 1", "Puma", "Footwear", 3000, 0.95),
                product(2, "Running Shoes 2", "Nike", "Footwear", 3500, 0.90),
                product(3, "Running Shoes 3", "Adidas", "Footwear", 4000, 0.85),
                product(4, "Running Shoes 4", "HRX", "Footwear", 2000, 0.80)
        );

        when(qdrantService.search(anyList(), anyInt()))
                .thenReturn(products);

        SearchResponse response =
                productRagService.search("running shoes", 2);

        assertEquals(2, response.results().size());
    }

    @Test
    void shouldReturnEmptyResultsWhenNoProductMatches() {

        when(openAiClient.embed(anyList()))
                .thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

        List<ProductResult> products = List.of(
                product(1, "Premium Sneakers", "Nike", "Footwear", 4999, 0.90),
                product(2, "Luxury Shoes", "Adidas", "Footwear", 5999, 0.85)
        );

        when(qdrantService.search(anyList(), anyInt()))
                .thenReturn(products);

        SearchResponse response =
                productRagService.search("Puma products under 1000", 5);

        assertTrue(response.results().isEmpty());
    }

    private ProductResult product(
            int productId,
            String productName,
            String brandName,
            String category,
            int price,
            double score
    ) {
        return new ProductResult(
                productId,
                productId,
                productName,
                brandName,
                category,
                BigDecimal.valueOf(price),
                score
        );
    }
}