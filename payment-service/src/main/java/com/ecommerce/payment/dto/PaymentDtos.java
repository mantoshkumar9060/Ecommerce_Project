package com.ecommerce.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record Create(@NotNull @Positive Long orderId, @NotNull @Positive Long userId,
                         @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
    }

    public record Created(Long paymentId, String status, String checkoutReference) {
    }

    public record Callback(@NotBlank @Size(max = 100) String checkoutReference, boolean successful) {
    }

    public record Details(Long paymentId, Long orderId, BigDecimal amount, String status, String checkoutReference) {
    }
}
