package com.example.inventory_order_system.controller.admin;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.order.OrderResponse;
import com.example.inventory_order_system.dto.order.OrderStatusUpdateRequest;
import com.example.inventory_order_system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin - Orders", description = "Admin order management APIs")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders", description = "Paginated list of orders, filterable by status")
    public ResponseEntity<PagedResponse<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin request: List orders status={} page={} size={}", status, page, size);
        return ResponseEntity.ok(orderService.getAllOrders(status, page, size));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status",
               description = "Update order status (PENDING → SHIPPED → DELIVERED → CANCELLED)")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                            @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("Admin request: Update order {} status to {}", id, request.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }
}
