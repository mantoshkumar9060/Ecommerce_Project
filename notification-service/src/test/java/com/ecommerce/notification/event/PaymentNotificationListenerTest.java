package com.ecommerce.notification.event;

import com.ecommerce.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class PaymentNotificationListenerTest {
    @Test
    void forwardsKafkaEventToNotificationService() {
        NotificationService service = mock(NotificationService.class);
        PaymentNotificationListener listener = new PaymentNotificationListener(service);
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 2L, 3L, BigDecimal.TEN, true);
        listener.receive(event);
        verify(service).payment(event);
    }
}
