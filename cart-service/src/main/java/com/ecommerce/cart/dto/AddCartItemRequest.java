package com.ecommerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(@NotNull Integer productId, @NotNull Integer brandId, @NotNull @Min(1) Integer quantity) { }
