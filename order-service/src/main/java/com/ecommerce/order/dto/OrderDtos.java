package com.ecommerce.order.dto;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
public final class OrderDtos { private OrderDtos(){} public record Create(@NotBlank String shippingAddress, String idempotencyKey){ public Create(String shippingAddress){ this(shippingAddress, null); } } public record Transition(@NotBlank String status){} public record Response(Long id,Long userId,String shippingAddress,String status,BigDecimal total,List<Item> items,String checkoutReference){} public record Item(Long id,Integer productId,Integer brandId,String productName,BigDecimal unitPrice,Integer quantity){} }
