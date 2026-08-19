package com.product_service.dto;

import java.math.BigDecimal;

public record ProductBrandSnapshot(
        Integer productId,
        Integer brandId,
        String productName,
        BigDecimal price,
        boolean available,
        int availableQuantity
) {
}
