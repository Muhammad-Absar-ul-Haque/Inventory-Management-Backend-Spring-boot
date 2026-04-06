package com.example.inventory_order_system.service;

import com.example.inventory_order_system.config.JwtProvider;
import com.example.inventory_order_system.dto.auth.*;
import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.entity.enums.Role;
import com.example.inventory_order_system.entity.enums.UserStatus;
import com.example.inventory_order_system.exception.ValidationException;
import com.example.inventory_order_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final OtpEmailService otpEmailService;

    public RegisterResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered: " + request.getEmail());
        }
        
        String generatedOtp = generateRandomOtp();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.PENDING_VERIFICATION)
                .otp(generatedOtp)
                .otpExpiryTime(LocalDateTime.now().plusMinutes(5))
                .otpAttempts(0)
                .build();

        userRepository.save(user);
        log.info("User registered in PENDING_VERIFICATION state: {}", user.getEmail());

        otpEmailService.sendOtpEmail(user.getEmail(), generatedOtp);

        return RegisterResponse.builder()
                .email(user.getEmail())
                .message("Registration successful. Please verify your OTP sent to your email.")
                .build();
    }
    
    public LoginResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ValidationException("User not found: " + request.getEmail()));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ValidationException("User is already verified.");
        }
        
        if (user.getOtpAttempts() >= 3) {
            throw new ValidationException("Maximum verification attempts reached. Please request a new OTP.");
        }

        if (user.getOtpExpiryTime() == null || user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException("OTP has expired. Please request a new one.");
        }

        if (!request.getOtp().equals(user.getOtp())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            throw new ValidationException("Invalid OTP code.");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        user.setOtpAttempts(0);
        userRepository.save(user);
        
        log.info("OTP verified successfully for user: {}", user.getEmail());

        String token = jwtProvider.generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
    
    public RegisterResponse resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ValidationException("User not found: " + request.getEmail()));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ValidationException("User is already verified.");
        }
        
        String newOtp = generateRandomOtp();
        user.setOtp(newOtp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        user.setOtpAttempts(0);
        userRepository.save(user);
        
        otpEmailService.sendOtpEmail(user.getEmail(), newOtp);
        
        return RegisterResponse.builder()
                .email(user.getEmail())
                .message("A new OTP has been sent successfully.")
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            User user = (User) authentication.getPrincipal();
            
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                throw new ValidationException("Account pending verification. Please verify your OTP to login.");
            }

            String token = jwtProvider.generateToken(user);

            log.info("Login successful for: {}", request.getEmail());

            return LoginResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .build();
        } catch (BadCredentialsException e) {
            log.warn("Login failed for: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }
    
    private String generateRandomOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); 
        return String.valueOf(otp);
    }
}
