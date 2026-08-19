package com.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long userId, List<Item> items, BigDecimal total) {
    public record Item(Long id, Integer productId, Integer brandId, String productName, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) { }
}
