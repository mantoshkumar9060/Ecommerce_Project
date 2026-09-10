package com.ecommerce.order.service;

import com.ecommerce.order.client.*;
import com.ecommerce.order.dto.OrderDtos;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CartClient carts = mock(CartClient.class);
    private final PaymentClient payments = mock(PaymentClient.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final OrderService service = new OrderService(orders, carts, payments, transactionTemplate);

    @BeforeEach
    void setupTransactionTemplate() {
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(mock(TransactionStatus.class));
        });
        doAnswer(inv -> {
            Consumer<TransactionStatus> consumer = inv.getArgument(0);
            consumer.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

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

    @Test
    void paymentGatewayTimeoutMarksOrderPendingVerification() {
        Order pendingOrder = order(1L, OrderStatus.PENDING_PAYMENT);
        when(carts.get(1L)).thenReturn(new CartClient.Cart(1L, List.of(new CartClient.Item(3L, 10, 2, "Shoes", new BigDecimal("99.99"), 2, new BigDecimal("199.98"))), new BigDecimal("199.98")));
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            setId(o, 8L);
            return o;
        });
        when(orders.findById(8L)).thenReturn(Optional.of(pendingOrder));
        when(payments.create(any())).thenThrow(new RuntimeException("Gateway read timeout"));

        assertThrows(RuntimeException.class, () -> service.create(1L, new OrderDtos.Create("Delhi")));

        verify(payments).create(any());
        assertEquals(OrderStatus.PAYMENT_PENDING_VERIFICATION, pendingOrder.getStatus());
        verify(orders).save(pendingOrder);
        verify(carts, never()).clear(1L);
    }

    @Test
    void paymentExplicitDeclineMarksOrderFailed() {
        Order pendingOrder = order(1L, OrderStatus.PENDING_PAYMENT);
        when(carts.get(1L)).thenReturn(new CartClient.Cart(1L, List.of(new CartClient.Item(3L, 10, 2, "Shoes", new BigDecimal("99.99"), 2, new BigDecimal("199.98"))), new BigDecimal("199.98")));
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            setId(o, 8L);
            return o;
        });
        when(orders.findById(8L)).thenReturn(Optional.of(pendingOrder));
        when(payments.create(any())).thenThrow(new RuntimeException("Card declined: insufficient funds"));

        assertThrows(RuntimeException.class, () -> service.create(1L, new OrderDtos.Create("Delhi")));

        verify(payments).create(any());
        assertEquals(OrderStatus.PAYMENT_FAILED, pendingOrder.getStatus());
        verify(orders).save(pendingOrder);
        verify(carts, never()).clear(1L);
    }

    @Test
    void cartClearFailureDoesNotFailOrderCreation() {
        when(carts.get(1L)).thenReturn(new CartClient.Cart(1L, List.of(new CartClient.Item(3L, 10, 2, "Shoes", new BigDecimal("99.99"), 2, new BigDecimal("199.98"))), new BigDecimal("199.98")));
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            setId(o, 8L);
            return o;
        });
        when(orders.findById(8L)).thenReturn(Optional.of(order(1L, OrderStatus.PENDING_PAYMENT)));
        when(payments.create(any())).thenReturn(new PaymentClient.Response(5L, "PENDING", "pay_x"));
        doThrow(new RuntimeException("Cart service timeout")).when(carts).clear(1L);

        var result = service.create(1L, new OrderDtos.Create("Delhi"));

        assertNotNull(result);
        assertEquals("PENDING_PAYMENT", result.status());
        assertEquals("pay_x", result.checkoutReference());
        verify(carts).clear(1L);
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingOrderWithPersistedCheckoutReference() {
        Order existingOrder = order(1L, OrderStatus.PENDING_PAYMENT);
        existingOrder.setIdempotencyKey("idemp_123");
        existingOrder.setCheckoutReference("pay_persisted_ref");
        when(orders.findByIdempotencyKey("idemp_123")).thenReturn(Optional.of(existingOrder));

        var result = service.create(1L, new OrderDtos.Create("Delhi", "idemp_123"));

        assertNotNull(result);
        assertEquals(existingOrder.getId(), result.id());
        assertEquals("pay_persisted_ref", result.checkoutReference());
        verifyNoInteractions(payments);
        verify(orders, never()).saveAndFlush(any());
        verify(carts, never()).clear(any());
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
