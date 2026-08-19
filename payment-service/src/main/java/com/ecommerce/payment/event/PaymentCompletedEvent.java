package com.ecommerce.payment.event;
import java.math.BigDecimal;
public record PaymentCompletedEvent(Long paymentId, Long orderId, Long userId, BigDecimal amount, boolean successful) { }
