package com.example.inventory_order_system.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
