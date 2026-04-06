package com.example.inventory_order_system.controller;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.notification.NotificationResponse;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for user notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(summary = "Get User Notifications", description = "Fetches paginated notifications for the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PagedResponse<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return ResponseEntity.ok(notificationService.getUserNotifications(user, page, size));
    }
}
