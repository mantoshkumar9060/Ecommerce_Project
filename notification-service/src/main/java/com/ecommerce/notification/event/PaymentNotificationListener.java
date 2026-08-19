package com.ecommerce.notification.event;

import com.ecommerce.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationListener {
    private final NotificationService notifications;

    public PaymentNotificationListener(NotificationService n) {
        notifications = n;
    }

    @KafkaListener(topics = "payment.completed")
    public void receive(PaymentCompletedEvent event) {
        notifications.payment(event);
    }
}
