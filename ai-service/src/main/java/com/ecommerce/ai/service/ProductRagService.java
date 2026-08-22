package com.ecommerce.ai.service;

import com.ecommerce.ai.client.ProductClient;
import com.ecommerce.ai.config.QdrantProperties;
import com.ecommerce.ai.dto.AiDtos.*;
import com.ecommerce.ai.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductRagService {

    private static final Logger log =
            LoggerFactory.getLogger(ProductRagService.class);

    private static final float MIN_RELEVANCE_SCORE = 0.55f;

    private static final Pattern BUDGET =
            Pattern.compile(
                    "(?i)(?:under|below)\\s*(?:₹|rs\\.?|inr)?\\s*([0-9][0-9,]*)"
            );

    private static final Set<String> KNOWN_CATEGORIES = Set.of(
            "footwear",
            "men",
            "women",
            "kids",
            "home",
            "living",
            "beauty"
    );

    private static final Set<String> SHOE_KEYWORDS = Set.of(
            "shoe",
            "shoes",
            "running",
            "runner",
            "sneaker",
            "sneakers",
            "trainer",
            "trainers"
    );

    private static final Set<String> NON_SHOE_KEYWORDS = Set.of(
            "flat",
            "flats",
            "heel",
            "heels",
            "sandal",
            "sandals",
            "slipper",
            "slippers",
            "loafer",
            "loafers"
    );

    private final ProductClient products;
    private final OpenAiClient openAi;
    private final QdrantService qdrant;
    private final QdrantProperties qdrantProperties;

    public ProductRagService(
            ProductClient products,
            OpenAiClient openAi,
            QdrantService qdrant,
            QdrantProperties qdrantProperties
    ) {
        this.products = products;
        this.openAi = openAi;
        this.qdrant = qdrant;
        this.qdrantProperties = qdrantProperties;
    }

    public IngestionResponse ingest() {

        log.info("Product ingestion started");

        List<ProductClient.Product> all = fetchProducts();

        List<Candidate> candidates = all.stream()
                .filter(ProductClient.Product::active)
                .flatMap(product ->
                        Optional.ofNullable(product.brands())
                                .orElse(List.of())
                                .stream()
                                .filter(Objects::nonNull)
                                .map(brand -> candidate(product, brand))
                )
                .toList();

        if (candidates.isEmpty()) {
            return new IngestionResponse(
                    "SUCCESS",
                    all.size(),
                    0,
                    all.size(),
                    collection()
            );
        }

        List<List<Double>> vectors =
                openAi.embed(
                        candidates.stream()
                                .map(Candidate::document)
                                .toList()
                );

        List<QdrantService.Point> points = new ArrayList<>();

        for (int index = 0; index < candidates.size(); index++) {

            Candidate candidate = candidates.get(index);

            points.add(
                    new QdrantService.Point(
                            qdrant.deterministicId(
                                    candidate.productId(),
                                    candidate.brandId()
                            ),
                            vectors.get(index),
                            candidate.payload()
                    )
            );
        }

        qdrant.upsert(points);

        log.info(
                "Product ingestion completed: fetched={}, indexed={}",
                all.size(),
                points.size()
        );

        return new IngestionResponse(
                "SUCCESS",
                all.size(),
                points.size(),
                Math.max(0, all.size() - points.size()),
                collection()
        );
    }

    public IngestionStatus status() {
        return new IngestionStatus(
                collection(),
                qdrant.exists(),
                qdrant.vectorCount()
        );
    }

    public SearchResponse search(String query, int limit) {

        int retrievalLimit = Math.max(limit * 10, 50);

        log.info(
                "Semantic search executed, retrievalLimit={}, finalLimit={}",
                retrievalLimit,
                limit
        );

        List<ProductResult> results = qdrant.search(
                openAi.embed(List.of(query)).get(0),
                retrievalLimit
        );

        boolean hasBudget = BUDGET.matcher(query).find();

        /*
         * For normal semantic queries without a budget,
         * reject weak matches.
         *
         * Example:
         * "wireless headphones"
         *
         * This prevents unrelated catalogue products
         * from being returned.
         */
        if (!hasBudget) {
            results = results.stream()
                    .filter(result ->
                            result.score() >= MIN_RELEVANCE_SCORE
                    )
                    .toList();
        }

        /*
         * Apply structured filters.
         */
        results = applyBrand(query, results);
        results = applyCategory(query, results);
        results = applyProductType(query, results);
        results = applyBudget(query, results);

        /*
         * Return only requested number of products.
         */
        results = results.stream()
                .limit(limit)
                .toList();

        log.info(
                "Final filtered results: {}",
                results.size()
        );

        return new SearchResponse(query, results);
    }

    public ChatResponse chat(String question, int limit) {

        List<ProductResult> results = search(question, limit).results();

        if (results.isEmpty()) {
            return new ChatResponse(
                    "I could not find relevant products for that request.",
                    List.of()
            );
        }

        String answer = results.size() == 1
                ? "Here is a product matching your request."
                : "Here are " + results.size() + " products matching your request.";

        log.info(
                "RAG retrieval completed: {} products",
                results.size()
        );

        return new ChatResponse(
                answer,
                results
        );
    }

    private List<ProductClient.Product> fetchProducts() {

        try {

            List<ProductClient.Product> all =
                    new ArrayList<>();

            int page = 0;
            int totalPages;

            do {

                var response =
                        products.catalogue(
                                page++,
                                100,
                                "relevance"
                        );

                all.addAll(
                        Optional.ofNullable(response.data())
                                .orElse(List.of())
                );

                totalPages =
                        response.totalPages() == null
                                ? page
                                : response.totalPages();

            } while (page < totalPages);

            log.info(
                    "Products fetched: {}",
                    all.size()
            );

            return all;

        } catch (Exception error) {

            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Product catalog service is temporarily unavailable"
            );
        }
    }

    private Candidate candidate(
            ProductClient.Product product,
            ProductClient.Brand brand
    ) {

        String category =
                product.subCategory() == null
                        || product.subCategory().category() == null
                        ? "Uncategorized"
                        : product.subCategory().category().name();

        String document =
                """
                Product Name: %s
                Brand: %s
                Category: %s
                Description: %s
                Price: ₹%s
                """.formatted(
                        product.name(),
                        brand.name(),
                        category,
                        Optional.ofNullable(product.description())
                                .orElse("Not provided"),
                        brand.price()
                );

        return new Candidate(
                product.id(),
                brand.id(),
                product.name(),
                brand.name(),
                category,
                brand.price(),
                document
        );
    }

    private List<ProductResult> applyBudget(
            String query,
            List<ProductResult> results
    ) {

        Matcher matcher = BUDGET.matcher(query);

        if (!matcher.find()) {
            return results;
        }

        BigDecimal max =
                new BigDecimal(
                        matcher.group(1)
                                .replace(",", "")
                );

        return results.stream()
                .filter(result ->
                        result.price().compareTo(max) <= 0
                )
                .toList();
    }

    private List<ProductResult> applyCategory(
            String query,
            List<ProductResult> results
    ) {

        String normalizedQuery =
                query.toLowerCase(Locale.ROOT);

        Set<String> requestedCategories =
                KNOWN_CATEGORIES.stream()
                        .filter(normalizedQuery::contains)
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        if (requestedCategories.isEmpty()) {
            return results;
        }

        return results.stream()
                .filter(result ->
                        result.category() != null
                )
                .filter(result ->
                        requestedCategories.contains(
                                result.category()
                                        .toLowerCase(Locale.ROOT)
                        )
                )
                .toList();
    }

    private List<ProductResult> applyProductType(
            String query,
            List<ProductResult> results
    ) {

        String normalizedQuery =
                query.toLowerCase(Locale.ROOT);

        boolean shoeQuery =
                SHOE_KEYWORDS.stream()
                        .anyMatch(normalizedQuery::contains);

        if (!shoeQuery) {
            return results;
        }

        return results.stream()
                .filter(result -> {

                    String productName =
                            Optional.ofNullable(
                                            result.productName()
                                    )
                                    .orElse("")
                                    .toLowerCase(Locale.ROOT);

                    /*
                     * Reject clearly non-shoe footwear.
                     */
                    boolean isNonShoe =
                            NON_SHOE_KEYWORDS.stream()
                                    .anyMatch(productName::contains);

                    if (isNonShoe) {
                        return false;
                    }

                    /*
                     * Keep products whose name
                     * indicates a shoe/sneaker/trainer.
                     */
                    return SHOE_KEYWORDS.stream()
                            .anyMatch(productName::contains);
                })
                .toList();
    }

    private List<ProductResult> applyBrand(
            String query,
            List<ProductResult> results
    ) {

        String normalizedQuery =
                query.toLowerCase(Locale.ROOT);

        Set<String> requestedBrands =
                results.stream()
                        .map(ProductResult::brandName)
                        .filter(Objects::nonNull)
                        .filter(brand ->
                                normalizedQuery.contains(
                                        brand.toLowerCase(Locale.ROOT)
                                )
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        if (requestedBrands.isEmpty()) {
            return results;
        }

        return results.stream()
                .filter(result ->
                        requestedBrands.contains(
                                result.brandName()
                        )
                )
                .toList();
    }

    private String collection() {
        return qdrantProperties.collection();
    }

    private record Candidate(
            int productId,
            int brandId,
            String productName,
            String brandName,
            String category,
            BigDecimal price,
            String document
    ) {

        Map<String, Object> payload() {

            return Map.of(
                    "productId", productId,
                    "brandId", brandId,
                    "productName", productName,
                    "brandName", brandName,
                    "category", category,
                    "price", price
            );
        }
    }
}