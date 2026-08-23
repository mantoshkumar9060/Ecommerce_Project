package com.ecommerce.order.service;

import com.ecommerce.order.client.*;
import com.ecommerce.order.dto.OrderDtos;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CartClient carts = mock(CartClient.class);
    private final PaymentClient payments = mock(PaymentClient.class);
    private final OrderService service = new OrderService(orders, carts, payments);

    @Test
    void createCopiesCartCreatesPaymentAndClearsCart() {
        when(carts.get(1L)).thenReturn(new CartClient.Cart(1L, List.of(new CartClient.Item(3L, 10, 2, "Shoes", new BigDecimal("99.99"), 2, new BigDecimal("199.98"))), new BigDecimal("199.98")));
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            setId(o, 8L);
            return o;
        });
        when(payments.create(any())).thenReturn(new PaymentClient.Response(5L, "PENDING", "pay_x"));
        var result = service.create(1L, new OrderDtos.Create("Delhi"));
        assertEquals("PENDING_PAYMENT", result.status());
        assertEquals("pay_x", result.checkoutReference());
        assertEquals(1, result.items().size());
        verify(carts).clear(1L);
    }

    @Test
    void createRejectsEmptyCartBeforeWritingOrder() {
        when(carts.get(1L)).thenReturn(new CartClient.Cart(1L, List.of(), BigDecimal.ZERO));
        assertThrows(IllegalStateException.class, () -> service.create(1L, new OrderDtos.Create("Delhi")));
        verifyNoInteractions(payments);
        verify(orders, never()).saveAndFlush(any());
    }

    @Test
    void paymentStatusIsIdempotentButCannotFinalizeTwice() {
        Order order = order(1L, OrderStatus.PENDING_PAYMENT);
        when(orders.findWithItemsById(8L)).thenReturn(Optional.of(order));
        service.updatePaymentStatus(8L, true);
        assertEquals(OrderStatus.PAID, order.getStatus());
        service.updatePaymentStatus(8L, true);
        assertThrows(IllegalStateException.class, () -> service.updatePaymentStatus(8L, false));
    }

    @Test
    void cancelRejectsOtherUsersAndCompletedOrders() {
        Order order = order(2L, OrderStatus.PENDING_PAYMENT);
        when(orders.findWithItemsById(8L)).thenReturn(Optional.of(order));
        assertThrows(IllegalArgumentException.class, () -> service.cancel(1L, 8L));
        order.setStatus(OrderStatus.PAID);
        assertThrows(IllegalStateException.class, () -> service.cancel(2L, 8L));
    }

    @Test
    void transitionAllowsOnlyDeclaredLifecycle() {
        Order order = order(1L, OrderStatus.PAID);
        when(orders.findWithItemsById(8L)).thenReturn(Optional.of(order));
        assertEquals("PROCESSING", service.transition(8L, OrderStatus.PROCESSING).status());
        assertThrows(IllegalStateException.class, () -> service.transition(8L, OrderStatus.DELIVERED));
    }

    private static Order order(long user, OrderStatus status) {
        Order o = new Order();
        setId(o, 8L);
        o.setUserId(user);
        o.setShippingAddress("Delhi");
        o.setStatus(status);
        o.setTotal(BigDecimal.TEN);
        return o;
    }

    private static void setId(Order o, long id) {
        try {
            var f = Order.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(o, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
