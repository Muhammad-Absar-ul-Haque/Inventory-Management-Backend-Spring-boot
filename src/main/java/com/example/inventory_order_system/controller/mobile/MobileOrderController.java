package com.example.inventory_order_system.controller.mobile;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.order.OrderRequest;
import com.example.inventory_order_system.dto.order.OrderResponse;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/orders")
@RequiredArgsConstructor
@Tag(name = "Mobile - Orders", description = "Customer order APIs")
public class MobileOrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order",
               description = "Validate stock, decrement quantities, save order atomically. Returns 201 Created.")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request,
                                                     @AuthenticationPrincipal User user) {
        log.info("Mobile request: Place order for user: {}", user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request, user));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my order history", description = "List current user's order history with pagination")
    public ResponseEntity<PagedResponse<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Mobile request: Get orders for user: {}", user.getEmail());
        return ResponseEntity.ok(orderService.getUserOrders(user.getId(), page, size));
    }
}
