package com.ecommerce.payment.service;
import com.ecommerce.payment.*;
import com.ecommerce.payment.dto.PaymentDtos;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service public class PaymentService {
 private final PaymentRepository payments; private final KafkaTemplate<String,PaymentCompletedEvent> events;
 public PaymentService(PaymentRepository payments,KafkaTemplate<String,PaymentCompletedEvent> events){this.payments=payments;this.events=events;}
 @Transactional public PaymentDtos.Created create(PaymentDtos.Create r){Payment existing=payments.findByOrderId(r.orderId()).orElse(null);if(existing!=null){if(!existing.getUserId().equals(r.userId())||existing.getAmount().compareTo(r.amount())!=0)throw new IllegalStateException("Payment details do not match the existing order payment");return new PaymentDtos.Created(existing.getId(),existing.getStatus().name(),existing.getCheckoutReference());}Payment p=new Payment();p.setOrderId(r.orderId());p.setUserId(r.userId());p.setAmount(r.amount());p.setStatus(PaymentStatus.PENDING);p.setCheckoutReference("pay_"+UUID.randomUUID());p=payments.save(p);return new PaymentDtos.Created(p.getId(),p.getStatus().name(),p.getCheckoutReference());}
 @Transactional public PaymentDtos.Details getByOrder(Long orderId,Long userId){Payment p=payments.findByOrderIdAndUserId(orderId,userId).orElseThrow(()->new IllegalArgumentException("Payment not found"));return new PaymentDtos.Details(p.getId(),p.getOrderId(),p.getAmount(),p.getStatus().name(),p.getCheckoutReference());}
 @Transactional public void callback(Long userId,PaymentDtos.Callback r){Payment p=payments.findByCheckoutReference(r.checkoutReference()).orElseThrow(()->new IllegalArgumentException("Payment not found"));if(!p.getUserId().equals(userId))throw new IllegalArgumentException("Payment does not belong to user");if(p.getStatus()!=PaymentStatus.PENDING)return;p.setStatus(r.successful()?PaymentStatus.SUCCESS:PaymentStatus.FAILED);events.send("payment.completed",p.getOrderId().toString(),new PaymentCompletedEvent(p.getId(),p.getOrderId(),p.getUserId(),p.getAmount(),r.successful()));}
}
