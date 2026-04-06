package com.example.inventory_order_system.service;

import com.example.inventory_order_system.dto.order.OrderItemRequest;
import com.example.inventory_order_system.dto.order.OrderRequest;
import com.example.inventory_order_system.dto.order.OrderResponse;
import com.example.inventory_order_system.entity.Order;
import com.example.inventory_order_system.entity.Product;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.entity.enums.OrderStatus;
import com.example.inventory_order_system.entity.enums.Role;
import com.example.inventory_order_system.exception.InsufficientStockException;
import com.example.inventory_order_system.exception.ResourceNotFoundException;
import com.example.inventory_order_system.mapper.OrderMapper;
import com.example.inventory_order_system.repository.OrderRepository;
import com.example.inventory_order_system.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.ROLE_CUSTOMER)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .version(0L)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should place order successfully with sufficient stock")
    void placeOrder_Success() {
        // Given
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 2);
        OrderRequest request = new OrderRequest(List.of(itemRequest));

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.decrementStock(1L, 2, 0L)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(
                OrderResponse.builder()
                        .id(1L)
                        .userId(1L)
                        .totalPrice(new BigDecimal("59.98"))
                        .status("PENDING")
                        .build());

        // When
        OrderResponse response = orderService.placeOrder(request, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("59.98"));
        verify(productRepository).decrementStock(1L, 2, 0L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock is too low")
    void placeOrder_InsufficientStock() {
        // Given
        testProduct.setStockQuantity(1);
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 5);
        OrderRequest request = new OrderRequest(List.of(itemRequest));

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(request, testUser))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product does not exist")
    void placeOrder_ProductNotFound() {
        // Given
        OrderItemRequest itemRequest = new OrderItemRequest(999L, 1);
        OrderRequest request = new OrderRequest(List.of(itemRequest));

        when(productRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(request, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException on concurrent stock modification")
    void placeOrder_ConcurrentModification() {
        // Given
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 2);
        OrderRequest request = new OrderRequest(List.of(itemRequest));

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.decrementStock(1L, 2, 0L)).thenReturn(0); // Simulates concurrent modification

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(request, testUser))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("concurrently");
    }
}
