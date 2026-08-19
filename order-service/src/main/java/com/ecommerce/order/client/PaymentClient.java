package com.ecommerce.order.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@FeignClient(name="payment-service",path="/internal/payments") public interface PaymentClient { @PostMapping Response create(@RequestBody Request request); record Request(Long orderId,Long userId,BigDecimal amount){} record Response(Long paymentId,String status,String checkoutReference){} }
