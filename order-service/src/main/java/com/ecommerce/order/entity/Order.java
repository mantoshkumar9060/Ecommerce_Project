package com.ecommerce.order.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="orders")
public class Order {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId; @Column(nullable=false,length=1000) private String shippingAddress;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private OrderStatus status;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal total; @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @Column(name="idempotency_key",unique=true,length=100) private String idempotencyKey;
 @Column(name="checkout_reference",length=100) private String checkoutReference;
 @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) private Set<OrderItem> items=new LinkedHashSet<>();
 public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getShippingAddress(){return shippingAddress;} public void setShippingAddress(String v){shippingAddress=v;} public OrderStatus getStatus(){return status;} public void setStatus(OrderStatus v){status=v;} public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;} public Set<OrderItem> getItems(){return items;} public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;} public String getCheckoutReference(){return checkoutReference;} public void setCheckoutReference(String v){checkoutReference=v;}
}
