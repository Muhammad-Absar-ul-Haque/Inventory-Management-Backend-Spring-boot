package com.example.inventory_order_system.controller.mobile;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.product.ProductResponse;
import com.example.inventory_order_system.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/products")
@RequiredArgsConstructor
@Tag(name = "Mobile - Products", description = "Customer product browsing APIs")
public class MobileProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search products", description = "Searchable and category-filtered list of available products")
    public ResponseEntity<PagedResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Mobile request: Search products search={} category={} page={} size={}", search, category, page, size);
        return ResponseEntity.ok(productService.searchProducts(search, category, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details", description = "Return detailed information about a single product including stock")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("Mobile request: Get product ID: {}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }
}
