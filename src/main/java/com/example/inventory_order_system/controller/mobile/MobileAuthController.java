package com.example.inventory_order_system.controller.mobile;

import com.example.inventory_order_system.dto.auth.*;
import com.example.inventory_order_system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/auth")
@RequiredArgsConstructor
@Tag(name = "Mobile - Authentication", description = "Customer authentication APIs")
public class MobileAuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer", description = "Customer signup. Returns instructions to verify OTP.")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Mobile request: Register user with email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.register(request));
    }
    
    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP code", description = "Submit the 6-digit code sent to email to unlock the account and receive JWT token.")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Mobile request: Verify OTP for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.verifyOtp(request));
    }
    
    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP code", description = "Request a fresh 6-digit code sent to email.")
    public ResponseEntity<RegisterResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Mobile request: Resend OTP for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Customer login", description = "Authenticate securely and return JWT token. Blocked if unverified.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Mobile request: Login for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }
}
