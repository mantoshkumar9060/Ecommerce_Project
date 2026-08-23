package com.ecommerce.notification.service;

import com.ecommerce.notification.event.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test
    void enabledEmailSendsPaymentOutcomeToUserAddress() {
        JavaMailSender mail = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(mail, true);
        service.payment(new PaymentCompletedEvent(1L, 7L, 3L, new BigDecimal("199.99"), true));
        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertArrayEquals(new String[]{"user-3@example.invalid"}, message.getTo());
        assertTrue(message.getText().contains("completed"));
        assertTrue(message.getText().contains("199.99"));
    }

    @Test
    void disabledEmailDoesNotCallMailProvider() {
        JavaMailSender mail = mock(JavaMailSender.class);
        new NotificationService(mail, false).payment(new PaymentCompletedEvent(1L, 7L, 3L, BigDecimal.TEN, false));
        verify(mail, never()).send(any(SimpleMailMessage.class));
    }
}
