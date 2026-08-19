package com.ecommerce.order.controller;
import com.ecommerce.order.service.OrderService; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/internal/orders") public class InternalOrderController {private final OrderService orders;public InternalOrderController(OrderService orders){this.orders=orders;}@PatchMapping("/{orderId}/payment-status")@ResponseStatus(HttpStatus.NO_CONTENT)public void paymentStatus(@PathVariable Long orderId,@RequestParam boolean successful){orders.updatePaymentStatus(orderId,successful);}}
