package com.example.inventory_order_system.dto.notification;

import com.example.inventory_order_system.entity.enums.NotificationStatus;
import com.example.inventory_order_system.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private LocalDateTime createdAt;
}
