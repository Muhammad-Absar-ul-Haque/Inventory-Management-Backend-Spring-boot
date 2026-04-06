package com.example.inventory_order_system.integration;

import com.example.inventory_order_system.dto.order.OrderItemRequest;
import com.example.inventory_order_system.dto.order.OrderRequest;
import com.example.inventory_order_system.dto.order.OrderResponse;
import com.example.inventory_order_system.entity.Product;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.entity.enums.Role;
import com.example.inventory_order_system.repository.OrderRepository;
import com.example.inventory_order_system.repository.ProductRepository;
import com.example.inventory_order_system.repository.UserRepository;
import com.example.inventory_order_system.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderServiceIT extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Test User")
                .role(Role.ROLE_CUSTOMER)
                .build());

        testProduct = productRepository.save(Product.builder()
                .sku("TEST-SKU-1")
                .name("Test Product")
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(10)
                .category("TEST")
                .build());
    }

    @Test
    void testConcurrentOrderPlacement() throws InterruptedException {
        int numberOfThreads = 15;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(testProduct.getId());
        itemRequest.setQuantity(1);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    orderService.placeOrder(orderRequest, testUser);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        
        assertThat(successCount.get()).isEqualTo(10); // Only 10 in stock
        assertThat(failCount.get()).isEqualTo(5); // 5 should fail due to insufficient stock
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0); // Stock should be exactly 0
    }
}
