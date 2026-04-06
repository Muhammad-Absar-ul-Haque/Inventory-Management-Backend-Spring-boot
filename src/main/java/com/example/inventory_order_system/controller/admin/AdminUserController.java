package com.example.inventory_order_system.controller.admin;

import com.example.inventory_order_system.dto.PagedResponse;
import com.example.inventory_order_system.dto.user.UserResponse;
import com.example.inventory_order_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Admin user management APIs")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users", description = "Paginated list of users with optional role filtering")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin request: List users role={} page={} size={}", role, page, size);
        return ResponseEntity.ok(userService.getAllUsers(role, page, size));
    }
}
