package com.ecommerce.cart.service;

import com.ecommerce.cart.client.ProductClient;
import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class CartService {
    private final CartRepository carts;
    private final ProductClient products;
    public CartService(CartRepository carts, ProductClient products) { this.carts = carts; this.products = products; }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        ProductClient.ProductSnapshot product = products.getProduct(request.productId(), request.brandId());
        if (!product.available()) throw new IllegalStateException("Product is not available");
        Cart cart = carts.findByUserId(userId).orElseGet(() -> { Cart c = new Cart(); c.setUserId(userId); return c; });
        CartItem item = cart.getItems().stream().filter(i -> i.getProductId().equals(request.productId()) && i.getBrandId().equals(request.brandId())).findFirst().orElseGet(() -> { CartItem i = new CartItem(); i.setCart(cart); i.setProductId(product.productId()); i.setBrandId(product.brandId()); i.setProductName(product.productName()); i.setUnitPrice(product.price()); cart.getItems().add(i); return i; });
        int desiredQuantity = item.getQuantity() == null ? request.quantity() : item.getQuantity() + request.quantity();
        ensureAvailable(product, desiredQuantity);
        item.setQuantity(desiredQuantity);
        return toResponse(carts.save(cart));
    }
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) { Cart cart = getCart(userId); CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Cart item not found")); ProductClient.ProductSnapshot product = products.getProduct(item.getProductId(), item.getBrandId()); if (!product.available()) throw new IllegalStateException("Product is not available"); ensureAvailable(product, request.quantity()); item.setQuantity(request.quantity()); return toResponse(cart); }
    @Transactional
    public void removeItem(Long userId, Long itemId) { Cart cart = getCart(userId); if (!cart.getItems().removeIf(i -> i.getId().equals(itemId))) throw new IllegalArgumentException("Cart item not found"); }
    @Transactional
    public void clear(Long userId) { getCart(userId).getItems().clear(); }
    @Transactional
    public CartResponse get(Long userId) {
        return carts.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> new CartResponse(userId, List.of(), BigDecimal.ZERO));
    }
    private Cart getCart(Long userId) { return carts.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Cart not found")); }
    private void ensureAvailable(ProductClient.ProductSnapshot product, int requestedQuantity) { if (requestedQuantity > product.availableQuantity()) throw new IllegalStateException("Only " + product.availableQuantity() + " item(s) are available"); }
    private CartResponse toResponse(Cart cart) { var items = cart.getItems().stream().sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Long::compareTo))).map(i -> new CartResponse.Item(i.getId(), i.getProductId(), i.getBrandId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList(); return new CartResponse(cart.getUserId(), items, items.stream().map(CartResponse.Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)); }
}
