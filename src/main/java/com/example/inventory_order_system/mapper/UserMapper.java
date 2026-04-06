package com.example.inventory_order_system.mapper;

import com.example.inventory_order_system.dto.profile.ProfileResponse;
import com.example.inventory_order_system.dto.user.UserResponse;
import com.example.inventory_order_system.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role", target = "role")
    UserResponse toResponse(User user);

    @Mapping(source = "role", target = "role")
    ProfileResponse toProfileResponse(User user);
}
