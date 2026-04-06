package com.example.inventory_order_system.dto.dashboard;

import com.example.inventory_order_system.dto.product.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private BigDecimal totalSales;
    private long pendingOrdersCount;
    private long totalProducts;
    private long totalUsers;
    private List<ProductResponse> lowStockProducts;
}
