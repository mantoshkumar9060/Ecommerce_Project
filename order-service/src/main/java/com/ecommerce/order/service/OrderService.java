package com.ecommerce.order.service;

import com.ecommerce.order.client.*;
import com.ecommerce.order.dto.OrderDtos;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final CartClient carts;
    private final PaymentClient payments;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orders, CartClient carts, PaymentClient payments, TransactionTemplate transactionTemplate) {
        this.orders = orders;
        this.carts = carts;
        this.payments = payments;
        this.transactionTemplate = transactionTemplate;
    }

    public OrderDtos.Response create(Long userId, OrderDtos.Create request) {
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Order> existing = orders.findByIdempotencyKey(request.idempotencyKey().trim());
            if (existing.isPresent()) {
                Order order = existing.get();
                if (!order.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("Idempotency key already used by another account");
                }
                return map(order);
            }
        }

        CartClient.Cart cart = carts.get(userId);
        if (cart.items() == null || cart.items().isEmpty())
            throw new IllegalStateException("Cannot create an order from an empty cart");

        Order order = savePendingOrder(userId, request, cart);

        PaymentClient.Response payment;
        try {
            payment = payments.create(new PaymentClient.Request(order.getId(), userId, order.getTotal()));
            saveCheckoutReference(order.getId(), payment.checkoutReference());
        } catch (Exception ex) {
            handlePaymentException(order.getId(), ex);
            throw ex;
        }

        try {
            carts.clear(userId);
        } catch (Exception ex) {
            log.warn("Failed to clear cart for user {} after payment creation for order {}: {}", userId, order.getId(), ex.getMessage());
        }

        return map(order, payment.checkoutReference());
    }

    public Order savePendingOrder(Long userId, OrderDtos.Create request, CartClient.Cart cart) {
        return transactionTemplate.execute(status -> {
            Order order = new Order();
            order.setUserId(userId);
            order.setShippingAddress(request.shippingAddress());
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setTotal(cart.total());
            if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
                order.setIdempotencyKey(request.idempotencyKey().trim());
            }
            for (CartClient.Item source : cart.items()) {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProductId(source.productId());
                item.setBrandId(source.brandId());
                item.setProductName(source.productName());
                item.setUnitPrice(source.unitPrice());
                item.setQuantity(source.quantity());
                order.getItems().add(item);
            }
            return orders.saveAndFlush(order);
        });
    }

    private void saveCheckoutReference(Long orderId, String checkoutReference) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                orders.findById(orderId).ifPresent(order -> {
                    order.setCheckoutReference(checkoutReference);
                    orders.save(order);
                });
            });
        } catch (Exception e) {
            log.error("Failed to save checkout reference for order {}: {}", orderId, e.getMessage());
        }
    }

    private void handlePaymentException(Long orderId, Exception ex) {
        boolean isExplicitDecline = isExplicitClientDecline(ex);
        OrderStatus targetStatus = isExplicitDecline ? OrderStatus.PAYMENT_FAILED : OrderStatus.PAYMENT_PENDING_VERIFICATION;
        try {
            transactionTemplate.executeWithoutResult(status -> {
                orders.findById(orderId).ifPresent(order -> {
                    order.setStatus(targetStatus);
                    orders.save(order);
                });
            });
            log.warn("Payment exception on order {}. Transitioned to {}: {}", orderId, targetStatus, ex.getMessage());
        } catch (Exception e) {
            log.error("Failed to update order {} status to {}: {}", orderId, targetStatus, e.getMessage());
        }
    }

    private boolean isExplicitClientDecline(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        return message.contains("declined") || message.contains("bad request") || message.contains("400") || message.contains("422");
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, boolean successful) {
        Order order = orders.findWithItemsById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        OrderStatus target = successful ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED;
        if (order.getStatus() == target) return;
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAYMENT_PENDING_VERIFICATION)
            throw new IllegalStateException("Order is already finalized");
        order.setStatus(target);
    }

    @Transactional
    public OrderDtos.Response cancel(Long userId, Long orderId) {
        Order order = owned(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAYMENT_PENDING_VERIFICATION)
            throw new IllegalStateException("Only orders awaiting payment can be cancelled online");
        order.setStatus(OrderStatus.CANCELLED);
        return map(order);
    }

    @Transactional
    public OrderDtos.Response transition(Long orderId, OrderStatus target) {
        Order order = orders.findWithItemsById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!allowed(order.getStatus(), target))
            throw new IllegalStateException("Invalid order status transition: " + order.getStatus() + " to " + target);
        order.setStatus(target);
        return map(order);
    }

    @Transactional
    public OrderDtos.Response get(Long userId, Long orderId) {
        return map(owned(userId, orderId));
    }

    @Transactional
    public List<OrderDtos.Response> list(Long userId) {
        return orders.findByUserIdOrderByIdDesc(userId).stream().map(this::map).toList();
    }

    private OrderDtos.Response map(Order order) {
        return map(order, order.getCheckoutReference());
    }

    private OrderDtos.Response map(Order order, String checkoutReference) {
        return new OrderDtos.Response(order.getId(), order.getUserId(), order.getShippingAddress(), order.getStatus().name(), order.getTotal(), order.getItems().stream().map(i -> new OrderDtos.Item(i.getId(), i.getProductId(), i.getBrandId(), i.getProductName(), i.getUnitPrice(), i.getQuantity())).toList(), checkoutReference);
    }

    private Order owned(Long userId, Long orderId) {
        Order order = orders.findWithItemsById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUserId().equals(userId)) throw new IllegalArgumentException("Order does not belong to user");
        return order;
    }

    private boolean allowed(OrderStatus current, OrderStatus target) {
        return switch (current) {
            case PENDING_PAYMENT, PAYMENT_PENDING_VERIFICATION ->
                    target == OrderStatus.PAID || target == OrderStatus.PAYMENT_FAILED || target == OrderStatus.CANCELLED;
            case PAID -> target == OrderStatus.PROCESSING;
            case PROCESSING -> target == OrderStatus.SHIPPED;
            case SHIPPED -> target == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> target == OrderStatus.DELIVERED;
            default -> false;
        };
    }
}
