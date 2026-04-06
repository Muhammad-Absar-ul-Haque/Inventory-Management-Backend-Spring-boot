package com.example.inventory_order_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Format must be a valid email address")
    private String email;

    @NotBlank(message = "OTP is required")
    private String otp;
}
