package com.ecommerce.cart.service;

import com.ecommerce.cart.client.ProductClient;
import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final ProductClient products = mock(ProductClient.class);
    private final CartService service = new CartService(carts, products);

    @Test
    void addItemCreatesCartSnapshotsPriceAndCalculatesTotal() {
        var snapshot = snapshot(true, 5);
        when(products.getProduct(10, 2)).thenReturn(snapshot);
        when(carts.findByUserId(1L)).thenReturn(Optional.empty());
        when(carts.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        CartResponse response = service.addItem(1L, new AddCartItemRequest(10, 2, 3));
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("299.97"), response.total());
        assertEquals("Shoes", response.items().get(0).productName());
    }

    @Test
    void addItemMergesExistingVariantAndEnforcesCombinedStock() {
        Cart cart = cart(1L);
        CartItem item = item(10, 2, 4);
        cart.getItems().add(item);
        when(carts.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(products.getProduct(10, 2)).thenReturn(snapshot(true, 5));
        assertThrows(IllegalStateException.class, () -> service.addItem(1L, new AddCartItemRequest(10, 2, 2)));
        assertEquals(4, item.getQuantity());
        verify(carts, never()).save(any());
    }

    @Test
    void updateRejectsUnavailableProductAndMissingItem() {
        Cart cart = cart(1L);
        CartItem item = item(10, 2, 1);
        setId(item, 7L);
        cart.getItems().add(item);
        when(carts.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(products.getProduct(10, 2)).thenReturn(snapshot(false, 10));
        assertThrows(IllegalStateException.class, () -> service.updateItem(1L, 7L, new UpdateCartItemRequest(2)));
        assertThrows(IllegalArgumentException.class, () -> service.updateItem(1L, 99L, new UpdateCartItemRequest(2)));
    }

    @Test
    void getForNewCartReturnsEmptyZeroTotal() {
        when(carts.findByUserId(9L)).thenReturn(Optional.empty());
        CartResponse response = service.get(9L);
        assertEquals(9L, response.userId());
        assertTrue(response.items().isEmpty());
        assertEquals(BigDecimal.ZERO, response.total());
    }

    private static ProductClient.ProductSnapshot snapshot(boolean available, int stock) {
        return new ProductClient.ProductSnapshot(10, 2, "Shoes", new BigDecimal("99.99"), available, stock);
    }

    private static Cart cart(long user) {
        Cart c = new Cart();
        c.setUserId(user);
        return c;
    }

    private static CartItem item(int product, int brand, int quantity) {
        CartItem i = new CartItem();
        i.setProductId(product);
        i.setBrandId(brand);
        i.setProductName("Shoes");
        i.setUnitPrice(new BigDecimal("99.99"));
        i.setQuantity(quantity);
        return i;
    }

    private static void setId(CartItem item, long id) {
        try {
            var f = CartItem.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(item, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
