package com.example.inventory_order_system.service;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.user.UserResponse;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.entity.enums.Role;
import com.example.inventory_order_system.mapper.UserMapper;
import com.example.inventory_order_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(String role, int page, int size) {
        log.debug("Fetching users - role: {}, page: {}, size: {}", role, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage;

        if (role != null && !role.isEmpty()) {
            try {
                Role userRole = Role.valueOf(role.toUpperCase());
                userPage = userRepository.findByRole(userRole, pageable);
            } catch (IllegalArgumentException e) {
                throw new com.example.inventory_order_system.exception.ValidationException(
                        "Invalid role: " + role);
            }
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return PagedResponse.<UserResponse>builder()
                .content(userPage.getContent().stream()
                        .map(userMapper::toResponse)
                        .toList())
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }
}
