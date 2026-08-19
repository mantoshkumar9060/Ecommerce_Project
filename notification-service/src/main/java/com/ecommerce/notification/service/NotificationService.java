package com.ecommerce.notification.service;
import com.ecommerce.notification.event.PaymentCompletedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
@Service public class NotificationService { private final JavaMailSender mail; private final boolean emailEnabled; public NotificationService(JavaMailSender mail,@Value("${notification.email.enabled}")boolean emailEnabled){this.mail=mail;this.emailEnabled=emailEnabled;} public void payment(PaymentCompletedEvent e){String message="Order #"+e.orderId()+" payment "+(e.successful()?"completed":"failed")+". Amount: "+e.amount();if(emailEnabled){SimpleMailMessage m=new SimpleMailMessage();m.setTo("user-"+e.userId()+"@example.invalid");m.setSubject("Ecommerce payment update");m.setText(message);mail.send(m);}else System.out.println("NOTIFICATION: "+message);}}
