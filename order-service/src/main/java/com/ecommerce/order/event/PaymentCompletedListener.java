package com.ecommerce.order.event;

import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedListener.class);
    private final OrderService orders;
    private final CartClient carts;

    public PaymentCompletedListener(OrderService orders, CartClient carts) {
        this.orders = orders;
        this.carts = carts;
    }

    @KafkaListener(topics = "payment.completed", groupId = "order-service")
    public void receive(PaymentCompletedEvent event) {
        log.info("Received payment event: paymentId={}, orderId={}, successful={}", event.paymentId(), event.orderId(), event.successful());
        orders.updatePaymentStatus(event.orderId(), event.successful());
        log.info("Order payment status processed: orderId={}, resultingStatus={}", event.orderId(), event.successful() ? "PAID" : "PAYMENT_FAILED");

        if (event.successful()) {
            try {
                carts.clear(event.userId());
                log.info("Reconciled and cleared cart for user {} after payment completed event", event.userId());
            } catch (Exception ex) {
                log.warn("Asynchronous cart reconciliation clear failed for user {}: {}", event.userId(), ex.getMessage());
            }
        }
    }
}
