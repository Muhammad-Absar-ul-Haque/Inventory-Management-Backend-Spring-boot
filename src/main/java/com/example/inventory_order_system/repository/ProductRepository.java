package com.example.inventory_order_system.repository;

import com.example.inventory_order_system.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedFalse(Long id);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.deleted = false " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))")
    Page<Product> searchProducts(@Param("search") String search,
                                 @Param("category") String category,
                                 Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity, p.version = p.version + 1 " +
           "WHERE p.id = :productId AND p.stockQuantity >= :quantity AND p.version = :version")
    int decrementStock(@Param("productId") Long productId,
                       @Param("quantity") int quantity,
                       @Param("version") Long version);

    Page<Product> findByDeletedFalse(Pageable pageable);
}
