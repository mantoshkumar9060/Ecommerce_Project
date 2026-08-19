package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {
    private final CartService carts;
    public CartController(CartService carts) { this.carts = carts; }
    @GetMapping public CartResponse get(@RequestHeader("X-User-Id") Long userId) { return carts.get(userId); }
    @PostMapping("/items") @ResponseStatus(HttpStatus.CREATED) public CartResponse add(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody AddCartItemRequest request) { return carts.addItem(userId, request); }
    @PatchMapping("/items/{itemId}") public CartResponse update(@RequestHeader("X-User-Id") Long userId, @PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) { return carts.updateItem(userId, itemId, request); }
    @DeleteMapping("/items/{itemId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@RequestHeader("X-User-Id") Long userId, @PathVariable Long itemId) { carts.removeItem(userId, itemId); }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) public void clear(@RequestHeader("X-User-Id") Long userId) { carts.clear(userId); }
}
