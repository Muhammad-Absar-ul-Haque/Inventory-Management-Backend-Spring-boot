package com.example.inventory_order_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpEmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toAddress, String otpCode) {
        log.info("Async: Preparing to send OTP email to {}", toAddress);
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(toAddress);
            email.setSubject("Your Verification OTP Code");
            email.setText("Your OTP is " + otpCode + ", it expires in 5 minutes.");
            
            mailSender.send(email);
            log.info("Async: OTP email successfully sent to {}", toAddress);
        } catch (Exception e) {
            log.error("Async Error: Failed to send OTP email to {}", toAddress, e);
        }
    }
}
