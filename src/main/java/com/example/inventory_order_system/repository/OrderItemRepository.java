package com.example.inventory_order_system.repository;

import com.example.inventory_order_system.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
