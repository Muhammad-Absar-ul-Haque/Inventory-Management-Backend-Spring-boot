package com.example.inventory_order_system.service;

import com.example.inventory_order_system.dto.dashboard.DashboardStatsResponse;
import com.example.inventory_order_system.dto.product.ProductResponse;
import com.example.inventory_order_system.entity.enums.OrderStatus;
import com.example.inventory_order_system.mapper.ProductMapper;
import com.example.inventory_order_system.repository.OrderRepository;
import com.example.inventory_order_system.repository.ProductRepository;
import com.example.inventory_order_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        log.info("Fetching dashboard statistics");

        List<ProductResponse> lowStockProducts = productRepository.findLowStockProducts(10)
                .stream()
                .map(productMapper::toResponse)
                .toList();

        return DashboardStatsResponse.builder()
                .totalSales(orderRepository.calculateTotalSales())
                .pendingOrdersCount(orderRepository.countByStatus(OrderStatus.PENDING))
                .totalProducts(productRepository.count())
                .totalUsers(userRepository.count())
                .lowStockProducts(lowStockProducts)
                .build();
    }
}
