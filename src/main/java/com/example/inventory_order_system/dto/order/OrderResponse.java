package com.example.inventory_order_system.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private BigDecimal totalPrice;
    private String status;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
}
