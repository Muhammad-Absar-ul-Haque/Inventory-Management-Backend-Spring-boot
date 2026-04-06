package com.example.inventory_order_system.service;

import com.example.inventory_order_system.config.kafka.KafkaConfig;
import com.example.inventory_order_system.dto.event.OrderCompletedEvent;
import com.example.inventory_order_system.entity.Notification;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.entity.enums.NotificationStatus;
import com.example.inventory_order_system.entity.enums.NotificationType;
import com.example.inventory_order_system.repository.NotificationRepository;
import com.example.inventory_order_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.notification.NotificationResponse;
import com.example.inventory_order_system.mapper.NotificationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_ORDERS_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        log.info("Received OrderCompletedEvent for order ID: {}", event.getOrderId());

        User user = userRepository.findById(event.getUserId()).orElse(null);
        if (user == null) {
            log.error("User not found for ID: {}. Cannot process notification.", event.getUserId());
            return;
        }

        String messageStr = String.format("Dear %s, your order #%d has been successfully delivered! Total amount: $%.2f",
                user.getFullName(), event.getOrderId(), event.getTotalPrice());

        Notification notification = Notification.builder()
                .user(user)
                .message(messageStr)
                .type(NotificationType.ORDER_COMPLETED)
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        boolean emailSent = sendEmail(user.getEmail(), "Order Delivered - #" + event.getOrderId(), messageStr);
        boolean wsSent = sendWebSocketMessage(user.getEmail(), messageStr);

        if (emailSent || wsSent) {
            notification.setStatus(NotificationStatus.SENT);
        } else {
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
        log.info("Processed OrderCompletedEvent for order ID: {}. Notification status: {}", event.getOrderId(), notification.getStatus());
    }

    private boolean sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            return false;
        }
    }

    private boolean sendWebSocketMessage(String username, String message) {
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send WebSocket message to user {}", username, e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(User user, int page, int size) {
        log.debug("Fetching notifications for user ID: {}", user.getId());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationPage = notificationRepository.findByUserId(user.getId(), pageable);

        return PagedResponse.<NotificationResponse>builder()
                .content(notificationPage.getContent().stream()
                        .map(notificationMapper::toResponse)
                        .toList())
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }
}
