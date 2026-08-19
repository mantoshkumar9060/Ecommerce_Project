package com.ecommerce.order.service;

import com.ecommerce.order.client.*;
import com.ecommerce.order.dto.OrderDtos;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final CartClient carts;
    private final PaymentClient payments;

    public OrderService(OrderRepository orders, CartClient carts, PaymentClient payments) {
        this.orders = orders;
        this.carts = carts;
        this.payments = payments;
    }

    @Transactional
    public OrderDtos.Response create(Long userId, OrderDtos.Create request) {
        CartClient.Cart cart = carts.get(userId);
        if (cart.items() == null || cart.items().isEmpty())
            throw new IllegalStateException("Cannot create an order from an empty cart");
        Order order = new Order();
        order.setUserId(userId);
        order.setShippingAddress(request.shippingAddress());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotal(cart.total());
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
        order = orders.saveAndFlush(order);
        PaymentClient.Response payment = payments.create(new PaymentClient.Request(order.getId(), userId, order.getTotal()));
        carts.clear(userId);
        return map(order, payment.checkoutReference());
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, boolean successful) {
        Order order = orders.findWithItemsById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        OrderStatus target = successful ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED;
        if (order.getStatus() == target) return;
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT)
            throw new IllegalStateException("Order is already finalized");
        order.setStatus(target);
    }

    @Transactional
    public OrderDtos.Response cancel(Long userId, Long orderId) {
        Order order = owned(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT)
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
        return map(order, null);
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
            case PENDING_PAYMENT ->
                    target == OrderStatus.PAID || target == OrderStatus.PAYMENT_FAILED || target == OrderStatus.CANCELLED;
            case PAID -> target == OrderStatus.PROCESSING;
            case PROCESSING -> target == OrderStatus.SHIPPED;
            case SHIPPED -> target == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> target == OrderStatus.DELIVERED;
            default -> false;
        };
    }
}
