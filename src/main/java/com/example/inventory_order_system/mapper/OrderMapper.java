package com.example.inventory_order_system.mapper;

import com.example.inventory_order_system.dto.order.OrderItemResponse;
import com.example.inventory_order_system.dto.order.OrderResponse;
import com.example.inventory_order_system.entity.Order;
import com.example.inventory_order_system.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "orderItems", target = "items")
    OrderResponse toResponse(Order order);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.sku", target = "productSku")
    OrderItemResponse toItemResponse(OrderItem orderItem);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> orderItems);
}
