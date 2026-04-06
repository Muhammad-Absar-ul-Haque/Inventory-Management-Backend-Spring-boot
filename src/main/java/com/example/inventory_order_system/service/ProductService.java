package com.example.inventory_order_system.service;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.product.ProductRequest;
import com.example.inventory_order_system.dto.product.ProductResponse;
import com.example.inventory_order_system.entity.Product;
import com.example.inventory_order_system.exception.ResourceNotFoundException;
import com.example.inventory_order_system.exception.ValidationException;
import com.example.inventory_order_system.mapper.ProductMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        if (productRepository.existsBySku(request.getSku())) {
            throw new ValidationException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = productMapper.toEntity(request);
        product.setDeleted(false);
        product = productRepository.save(product);

        log.info("Product created successfully with ID: {}", product.getId());
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Check SKU uniqueness if it's being changed
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new ValidationException("Product with SKU '" + request.getSku() + "' already exists");
        }

        productMapper.updateEntityFromRequest(request, product);
        product = productRepository.save(product);

        log.info("Product updated successfully with ID: {}", product.getId());
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void softDeleteProduct(Long id) {
        log.info("Soft-deleting product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setDeleted(true);
        productRepository.save(product);

        log.info("Product soft-deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product with ID: {}", id);

        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'search:' + #search + ':' + #category + ':' + #page + ':' + #size")
    public PagedResponse<ProductResponse> searchProducts(String search, String category, int page, int size) {
        log.debug("Searching products - search: {}, category: {}, page: {}, size: {}", search, category, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.searchProducts(search, category, pageable);

        return buildPagedResponse(productPage);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all:' + #page + ':' + #size")
    public PagedResponse<ProductResponse> getAllProducts(int page, int size) {
        log.debug("Fetching all products - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.findByDeletedFalse(pageable);

        return buildPagedResponse(productPage);
    }

    private PagedResponse<ProductResponse> buildPagedResponse(Page<Product> productPage) {
        return PagedResponse.<ProductResponse>builder()
                .content(productPage.getContent().stream()
                        .map(productMapper::toResponse)
                        .toList())
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }
}
