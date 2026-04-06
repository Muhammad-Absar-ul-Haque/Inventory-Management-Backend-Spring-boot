package com.example.inventory_order_system.service;

import com.example.inventory_order_system.dto.profile.ProfileResponse;
import com.example.inventory_order_system.dto.profile.ProfileUpdateRequest;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.exception.ResourceNotFoundException;
import com.example.inventory_order_system.exception.ValidationException;
import com.example.inventory_order_system.mapper.UserMapper;
import com.example.inventory_order_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        log.debug("Fetching profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        log.info("Updating profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);

        log.info("Profile updated successfully for user ID: {}", userId);
        return userMapper.toProfileResponse(user);
    }
}
