package com.example.inventory_order_system.controller.admin;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.product.ProductRequest;
import com.example.inventory_order_system.dto.product.ProductResponse;
import com.example.inventory_order_system.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin - Products", description = "Admin product management APIs")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product", description = "Create product with name, SKU, description, price, and stock quantity")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("Admin request: Create product with SKU: {}", request.getSku());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product", description = "Update product details and adjust stock manually")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                          @Valid @RequestBody ProductRequest request) {
        log.info("Admin request: Update product ID: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product", description = "Mark product as deleted (soft-delete)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Admin request: Soft-delete product ID: {}", id);
        productService.softDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List all products", description = "Paginated list of all products")
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin request: List products page={} size={}", page, size);
        return ResponseEntity.ok(productService.getAllProducts(page, size));
    }
}
