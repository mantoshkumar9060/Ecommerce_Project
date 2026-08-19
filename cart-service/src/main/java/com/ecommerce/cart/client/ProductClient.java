package com.ecommerce.cart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;

@FeignClient(name = "product-service", path = "/internal/products")
public interface ProductClient {
    @GetMapping("/{productId}/brands/{brandId}")
    ProductSnapshot getProduct(@PathVariable Integer productId, @PathVariable Integer brandId);
    record ProductSnapshot(Integer productId, Integer brandId, String productName, BigDecimal price, boolean available, int availableQuantity) { }
}
