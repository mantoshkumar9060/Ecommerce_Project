package com.ecommerce.order.repository;
import com.ecommerce.order.entity.Order;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface OrderRepository extends JpaRepository<Order,Long> { @EntityGraph(attributePaths="items") Optional<Order> findWithItemsById(Long id); @EntityGraph(attributePaths="items") List<Order> findByUserIdOrderByIdDesc(Long userId); @EntityGraph(attributePaths="items") Optional<Order> findByIdempotencyKey(String idempotencyKey); }
