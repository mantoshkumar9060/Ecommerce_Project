package com.ecommerce.ai.controller;

import com.ecommerce.ai.dto.AiDtos.ChatRequest;
import com.ecommerce.ai.dto.AiDtos.ChatResponse;
import com.ecommerce.ai.dto.AiDtos.IngestionResponse;
import com.ecommerce.ai.dto.AiDtos.IngestionStatus;
import com.ecommerce.ai.dto.AiDtos.SearchRequest;
import com.ecommerce.ai.dto.AiDtos.SearchResponse;
import com.ecommerce.ai.service.ProductRagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final ProductRagService rag;

    public AiController(ProductRagService rag) { this.rag = rag; }

    @PostMapping("/ingest/products")
    public IngestionResponse ingestProducts() { return rag.ingest(); }

    @GetMapping("/ingest/status")
    public IngestionStatus ingestionStatus() { return rag.status(); }

    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) { return rag.search(request.query(), request.safeLimit()); }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) { return rag.chat(request.question(), request.safeLimit()); }
}
