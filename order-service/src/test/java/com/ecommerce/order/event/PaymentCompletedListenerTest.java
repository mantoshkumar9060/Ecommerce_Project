package com.ecommerce.order.event;

import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class PaymentCompletedListenerTest {

    private final OrderService orders = mock(OrderService.class);
    private final CartClient carts = mock(CartClient.class);
    private final PaymentCompletedListener listener = new PaymentCompletedListener(orders, carts);

    @Test
    void shouldUpdateStatusAndClearCartWhenPaymentSuccessful() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 10L, 5L, BigDecimal.TEN, true);

        listener.receive(event);

        verify(orders).updatePaymentStatus(10L, true);
        verify(carts).clear(5L);
    }

    @Test
    void shouldUpdateStatusAndSkipCartClearWhenPaymentFailed() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 10L, 5L, BigDecimal.TEN, false);

        listener.receive(event);

        verify(orders).updatePaymentStatus(10L, false);
        verifyNoInteractions(carts);
    }
}
