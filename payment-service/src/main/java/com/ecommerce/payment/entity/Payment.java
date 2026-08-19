package com.ecommerce.payment.entity;

import com.ecommerce.payment.PaymentStatus;
import jakarta.persistence.*;

import java.math.*;
import java.time.*;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(name = "uk_payment_order", columnNames = "order_id"))
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    @Column(nullable = false, unique = true)
    private String checkoutReference;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setOrderId(Long v) {
        orderId = v;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public void setAmount(BigDecimal v) {
        amount = v;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus v) {
        status = v;
    }

    public String getCheckoutReference() {
        return checkoutReference;
    }

    public void setCheckoutReference(String v) {
        checkoutReference = v;
    }
}
