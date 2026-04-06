package com.example.inventory_order_system.controller.mobile;

import com.example.inventory_order_system.dto.profile.ProfileResponse;
import com.example.inventory_order_system.dto.profile.ProfileUpdateRequest;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/profile")
@RequiredArgsConstructor
@Tag(name = "Mobile - Profile", description = "Customer profile management APIs")
public class MobileProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "View my profile", description = "Get current user's profile information")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        log.info("Mobile request: Get profile for user: {}", user.getEmail());
        return ResponseEntity.ok(profileService.getProfile(user.getId()));
    }

    @PutMapping
    @Operation(summary = "Update my profile", description = "Update user profile (name, email, password)")
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody ProfileUpdateRequest request) {
        log.info("Mobile request: Update profile for user: {}", user.getEmail());
        return ResponseEntity.ok(profileService.updateProfile(user.getId(), request));
    }
}
