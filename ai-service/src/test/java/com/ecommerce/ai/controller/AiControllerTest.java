package com.ecommerce.ai.controller;

import com.ecommerce.ai.dto.AiDtos.ChatResponse;
import com.ecommerce.ai.dto.AiDtos.ProductResult;
import com.ecommerce.ai.dto.AiDtos.SearchResponse;
import com.ecommerce.ai.service.ProductRagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRagService productRagService;

    @Test
    void shouldSearchProducts() throws Exception {

        ProductResult product = new ProductResult(
                1,
                1,
                "Classic Sneakers",
                "Puma",
                "Footwear",
                BigDecimal.valueOf(2799),
                0.90
        );

        SearchResponse response = new SearchResponse(
                "Puma shoes under 3000",
                List.of(product)
        );

        when(productRagService.search(anyString(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/ai/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "query": "Puma shoes under 3000",
                                          "limit": 5
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query")
                        .value("Puma shoes under 3000"))
                .andExpect(jsonPath("$.results.length()")
                        .value(1))
                .andExpect(jsonPath("$.results[0].productName")
                        .value("Classic Sneakers"))
                .andExpect(jsonPath("$.results[0].brandName")
                        .value("Puma"));
    }

    @Test
    void shouldChatWithProducts() throws Exception {

        ProductResult product = new ProductResult(
                1,
                1,
                "Classic Sneakers",
                "Puma",
                "Footwear",
                BigDecimal.valueOf(2799),
                0.90
        );

        ChatResponse response = new ChatResponse(
                "Here is a product matching your request.",
                List.of(product)
        );

        when(productRagService.chat(anyString(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "question": "Puma shoes under 3000",
                                          "limit": 5
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer")
                        .value("Here is a product matching your request."))
                .andExpect(jsonPath("$.products.length()")
                        .value(1))
                .andExpect(jsonPath("$.products[0].brandName")
                        .value("Puma"));
    }

    @Test
    void shouldRejectInvalidSearchRequest() throws Exception {

        mockMvc.perform(
                        post("/api/v1/ai/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "query": "",
                                          "limit": 5
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidChatRequest() throws Exception {

        mockMvc.perform(
                        post("/api/v1/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "question": "",
                                          "limit": 5
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}