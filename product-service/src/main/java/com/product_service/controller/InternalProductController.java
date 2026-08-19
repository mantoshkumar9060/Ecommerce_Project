package com.product_service.controller;

import com.product_service.dto.ProductBrandSnapshot;
import com.product_service.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service catalog contract. It will be restricted to internal
 * callers once API Gateway and service authentication are introduced.
 */
@RestController
@RequestMapping("/internal/products")
public class InternalProductController {
    private final ProductService productService;

    public InternalProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}/brands/{brandId}")
    public ProductBrandSnapshot getProductBrandSnapshot(
            @PathVariable Integer productId,
            @PathVariable Integer brandId) {
        return productService.getProductBrandSnapshot(productId, brandId);
    }
}
