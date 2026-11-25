package com.example.moviereservation.service;

import com.example.moviereservation.dto.UserRegisterRequestDto;
import com.example.moviereservation.entity.Role;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserRegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApplicationException("Email '" + request.getEmail() + "' is already registered. Please sign in or use another email.");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with ID {} and email {}", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }
}
