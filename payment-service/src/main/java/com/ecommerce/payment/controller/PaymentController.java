package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentDtos;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/internal/payments")
    public PaymentDtos.Created create(@Valid @RequestBody PaymentDtos.Create r) {
        return payments.create(r);
    }

    @GetMapping("/api/v1/payments/orders/{orderId}")
    public PaymentDtos.Details getByOrder(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
        return payments.getByOrder(orderId, userId);
    }

    @PostMapping("/api/v1/payments/callback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void callback(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody PaymentDtos.Callback r) {
        payments.callback(userId, r);
    }
}
