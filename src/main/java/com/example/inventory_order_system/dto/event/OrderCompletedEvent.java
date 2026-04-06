package com.example.inventory_order_system.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    private Long orderId;
    private Long userId;
    private List<OrderItemEvent> items;
    private BigDecimal totalPrice;
}
