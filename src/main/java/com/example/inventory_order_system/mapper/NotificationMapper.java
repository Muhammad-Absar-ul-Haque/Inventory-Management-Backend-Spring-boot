package com.example.inventory_order_system.mapper;

import com.example.inventory_order_system.dto.notification.NotificationResponse;
import com.example.inventory_order_system.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(source = "user.id", target = "userId")
    NotificationResponse toResponse(Notification notification);
}
