package com.example.inventory_order_system.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEvent {
    private Long productId;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
}
