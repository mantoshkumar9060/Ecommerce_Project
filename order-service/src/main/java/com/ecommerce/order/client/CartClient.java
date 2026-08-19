package com.ecommerce.order.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal; import java.util.List;
@FeignClient(name="cart-service",path="/internal/carts") public interface CartClient { @GetMapping("/{userId}") Cart get(@PathVariable Long userId); @DeleteMapping("/{userId}") void clear(@PathVariable Long userId); record Cart(Long userId,List<Item> items,BigDecimal total){} record Item(Long id,Integer productId,Integer brandId,String productName,BigDecimal unitPrice,Integer quantity,BigDecimal subtotal){} }
