package com.example.inventory_order_system.mapper;

import com.example.inventory_order_system.dto.product.ProductRequest;
import com.example.inventory_order_system.dto.product.ProductResponse;
import com.example.inventory_order_system.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    Product toEntity(ProductRequest request);

    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);
}
