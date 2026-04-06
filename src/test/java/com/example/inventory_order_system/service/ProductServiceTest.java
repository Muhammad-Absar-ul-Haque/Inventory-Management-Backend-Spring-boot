package com.example.inventory_order_system.service;

import com.example.inventory_order_system.dto.product.ProductRequest;
import com.example.inventory_order_system.dto.product.ProductResponse;
import com.example.inventory_order_system.entity.Product;
import com.example.inventory_order_system.exception.ResourceNotFoundException;
import com.example.inventory_order_system.exception.ValidationException;
import com.example.inventory_order_system.mapper.ProductMapper;
import com.example.inventory_order_system.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest productRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productRequest = new ProductRequest();
        productRequest.setName("Test Product");
        productRequest.setSku("SKU-001");
        productRequest.setDescription("A test product");
        productRequest.setPrice(new BigDecimal("29.99"));
        productRequest.setStockQuantity(100);
        productRequest.setCategory("Electronics");

        testProduct = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .deleted(false)
                .version(0L)
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct_Success() {
        // Given
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productMapper.toEntity(productRequest)).thenReturn(testProduct);
        when(productRepository.save(testProduct)).thenReturn(testProduct);
        when(productMapper.toResponse(testProduct)).thenReturn(productResponse);

        // When
        ProductResponse result = productService.createProduct(productRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should throw ValidationException for duplicate SKU")
    void createProduct_DuplicateSku() {
        // Given
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(productRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SKU");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);
        when(productMapper.toResponse(testProduct)).thenReturn(productResponse);

        // When
        ProductResponse result = productService.updateProduct(1L, productRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productMapper).updateEntityFromRequest(productRequest, testProduct);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent product")
    void updateProduct_NotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(999L, productRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("Should soft-delete product successfully")
    void softDeleteProduct_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        // When
        productService.softDeleteProduct(1L);

        // Then
        assertThat(testProduct.getDeleted()).isTrue();
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when soft-deleting non-existent product")
    void softDeleteProduct_NotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.softDeleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }
}
