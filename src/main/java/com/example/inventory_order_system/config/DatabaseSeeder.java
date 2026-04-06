package com.example.inventory_order_system.config;

import com.example.inventory_order_system.entity.User;
import com.example.inventory_order_system.entity.enums.Role;
import com.example.inventory_order_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@yourdomain.com";
        String adminPassword = "adminpassword";

        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("No Admin found. Seeding default Admin user...");
            User admin = User.builder()
                    .fullName("Head Administrator")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Default Admin seeded successfully!");
            log.info("Email: {}", adminEmail);
            log.info("Password: {}", adminPassword);
        } else {
            log.info("Admin user already exists. Skipping database seeding.");
        }
    }
}
