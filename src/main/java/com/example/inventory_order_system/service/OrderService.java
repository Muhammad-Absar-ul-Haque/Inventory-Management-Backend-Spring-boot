package com.example.inventory_order_system.service;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.order.OrderItemRequest;
import com.example.inventory_order_system.dto.order.OrderRequest;
import com.example.inventory_order_system.dto.order.OrderResponse;
import com.example.inventory_order_system.dto.order.OrderStatusUpdateRequest;
import com.example.inventory_order_system.entity.Order;
import com.example.inventory_order_system.entity.OrderItem;
import com.example.inventory_order_system.entity.Product;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.entity.enums.OrderStatus;
import com.example.inventory_order_system.dto.event.OrderCompletedEvent;
import com.example.inventory_order_system.dto.event.OrderItemEvent;
import com.example.inventory_order_system.config.kafka.KafkaConfig;
import com.example.inventory_order_system.exception.InsufficientStockException;
import com.example.inventory_order_system.exception.ResourceNotFoundException;
import com.example.inventory_order_system.exception.ValidationException;
import com.example.inventory_order_system.mapper.OrderMapper;
import com.example.inventory_order_system.repository.OrderRepository;
import com.example.inventory_order_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @CacheEvict(value = {"user_orders", "products"}, allEntries = true)
    public OrderResponse placeOrder(OrderRequest request, User user) {
        log.info("Placing order for user: {}", user.getEmail());

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndDeletedFalse(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + itemRequest.getProductId()));

            // Validate stock availability
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getName() +
                        "'. Available: " + product.getStockQuantity() +
                        ", Requested: " + itemRequest.getQuantity());
            }

            // Decrement stock using optimistic locking
            int updated = productRepository.decrementStock(
                    product.getId(), itemRequest.getQuantity(), product.getVersion());

            if (updated == 0) {
                throw new InsufficientStockException(
                        "Failed to reserve stock for product '" + product.getName() +
                        "'. The product may have been modified concurrently. Please try again.");
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build();

            order.addOrderItem(orderItem);
        }

        order.setTotalPrice(totalPrice);
        order = orderRepository.save(order);

        log.info("Order placed successfully with ID: {} for user: {}", order.getId(), user.getEmail());
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user_orders", key = "#userId + ':' + #page + ':' + #size")
    public PagedResponse<OrderResponse> getUserOrders(Long userId, int page, int size) {
        log.debug("Fetching orders for user ID: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        return buildPagedResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(String status, int page, int size) {
        log.debug("Fetching all orders - status: {}, page: {}, size: {}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage;

        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderRepository.findByStatus(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid order status: " + status);
            }
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        return buildPagedResponse(orderPage);
    }

    @Transactional
    @CacheEvict(value = "user_orders", allEntries = true)
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        log.info("Updating order status for order ID: {} to {}", orderId, request.getStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
            order.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid order status: " + request.getStatus());
        }

        order = orderRepository.save(order);

        // Publish event if order is DELIVERED
        if (order.getStatus() == OrderStatus.DELIVERED) {
            publishOrderCompletedEvent(order);
        }

        log.info("Order status updated to {} for order ID: {}", order.getStatus(), orderId);
        return orderMapper.toResponse(order);
    }

    private void publishOrderCompletedEvent(Order order) {
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUser().getId())
                .totalPrice(order.getTotalPrice())
                .items(order.getOrderItems().stream().map(item -> OrderItemEvent.builder()
                        .productId(item.getProduct().getId())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .build()).toList())
                .build();
        
        kafkaTemplate.send(KafkaConfig.TOPIC_ORDERS_COMPLETED, String.valueOf(order.getId()), event);
        log.info("Published OrderCompletedEvent to Kafka topic {} for order ID: {}", KafkaConfig.TOPIC_ORDERS_COMPLETED, order.getId());
    }

    private PagedResponse<OrderResponse> buildPagedResponse(Page<Order> orderPage) {
        return PagedResponse.<OrderResponse>builder()
                .content(orderPage.getContent().stream()
                        .map(orderMapper::toResponse)
                        .toList())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }
}
