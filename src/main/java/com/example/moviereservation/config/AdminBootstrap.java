package com.example.moviereservation.config;

import com.example.moviereservation.entity.Role;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the admin account from app.admin.* (ADMIN_EMAIL / ADMIN_PASSWORD in prod) if it doesn't exist yet.
 * Runs in every profile, before DataLoader.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set: no admin account will be created.");
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        userRepository.save(User.builder()
                .name("Admin User")
                .email(normalized)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build());
        log.info("Created admin account {}", normalized);
    }
}
