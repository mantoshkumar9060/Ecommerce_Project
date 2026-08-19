package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/carts")
public class InternalCartController {
    private final CartService carts;
    public InternalCartController(CartService carts) { this.carts = carts; }
    @GetMapping("/{userId}") public CartResponse get(@PathVariable Long userId) { return carts.get(userId); }
    @DeleteMapping("/{userId}") public void clear(@PathVariable Long userId) { carts.clear(userId); }
}
