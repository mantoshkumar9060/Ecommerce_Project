package com.ecommerce.payment.service;

import com.ecommerce.payment.*;
import com.ecommerce.payment.PaymentStatus;
import com.ecommerce.payment.dto.PaymentDtos;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, PaymentCompletedEvent> events = mock(KafkaTemplate.class);
    private final PaymentService service = new PaymentService(payments, events);

    @Test
    void createPersistsPendingPaymentWithCheckoutReference() {
        when(payments.findByOrderId(7L)).thenReturn(Optional.empty());
        when(payments.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            setId(p, 3L);
            return p;
        });
        var result = service.create(new PaymentDtos.Create(7L, 1L, new BigDecimal("99.99")));
        assertEquals("PENDING", result.status());
        assertTrue(result.checkoutReference().startsWith("pay_"));
    }

    @Test
    void createReturnsExistingPaymentForSameOrderDetails() {
        Payment payment = payment(7L, 1L, PaymentStatus.PENDING);
        setId(payment, 3L);
        when(payments.findByOrderId(7L)).thenReturn(Optional.of(payment));
        var result = service.create(new PaymentDtos.Create(7L, 1L, BigDecimal.TEN));
        assertEquals(3L, result.paymentId());
        verify(payments, never()).save(any());
    }

    @Test
    void createRejectsExistingPaymentWithDifferentUserOrAmount() {
        Payment payment = payment(7L, 1L, PaymentStatus.PENDING);
        when(payments.findByOrderId(7L)).thenReturn(Optional.of(payment));
        assertThrows(IllegalStateException.class, () -> service.create(new PaymentDtos.Create(7L, 2L, BigDecimal.TEN)));
        assertThrows(IllegalStateException.class, () -> service.create(new PaymentDtos.Create(7L, 1L, new BigDecimal("11.00"))));
    }

    @Test
    void callbackPublishesExactlyOnceAndEnforcesOwnership() {
        Payment payment = payment(7L, 1L, PaymentStatus.PENDING);
        when(payments.findByCheckoutReference("pay_x")).thenReturn(Optional.of(payment));
        service.callback(1L, new PaymentDtos.Callback("pay_x", true));
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(events).send(eq("payment.completed"), eq("7"), any(PaymentCompletedEvent.class));
        service.callback(1L, new PaymentDtos.Callback("pay_x", true));
        verify(events, times(1)).send(anyString(), anyString(), any());
        assertThrows(IllegalArgumentException.class, () -> service.callback(2L, new PaymentDtos.Callback("pay_x", false)));
    }

    @Test
    void getByOrderRejectsUnknownPayment() {
        when(payments.findByOrderIdAndUserId(7L, 1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getByOrder(7L, 1L));
    }

    private static Payment payment(long order, long user, PaymentStatus status) {
        Payment p = new Payment();
        p.setOrderId(order);
        p.setUserId(user);
        p.setAmount(BigDecimal.TEN);
        p.setStatus(status);
        p.setCheckoutReference("pay_x");
        return p;
    }

    private static void setId(Payment p, long id) {
        try {
            var f = Payment.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
