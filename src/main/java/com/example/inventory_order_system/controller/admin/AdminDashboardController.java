package com.example.inventory_order_system.controller.admin;

import com.example.inventory_order_system.dto.dashboard.DashboardStatsResponse;
import com.example.inventory_order_system.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard", description = "Admin dashboard statistics")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics",
               description = "Returns total sales, low-stock alerts, pending orders count, and more")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        log.info("Admin request: Get dashboard stats");
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
}
