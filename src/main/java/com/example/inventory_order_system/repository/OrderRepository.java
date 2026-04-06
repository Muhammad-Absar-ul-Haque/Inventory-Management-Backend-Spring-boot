package com.example.inventory_order_system.repository;

import com.example.inventory_order_system.entity.Order;
import com.example.inventory_order_system.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status != 'CANCELLED'")
    BigDecimal calculateTotalSales();
}
