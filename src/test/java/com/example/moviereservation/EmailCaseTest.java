package com.example.moviereservation;

import com.example.moviereservation.dto.UserRegisterRequestDto;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.security.CustomUserDetailsService;
import com.example.moviereservation.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
class EmailCaseTest {

    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private UserService userService;

    @Test
    void loginIgnoresEmailCaseAndSpaces() {
        assertThat(userDetailsService.loadUserByUsername("  Rahul.Sharma@Example.COM ").getUsername())
                .isEqualTo("rahul.sharma@example.com");
    }

    @Test
    void registeringSameEmailInDifferentCaseGivesFriendlyError() {
        UserRegisterRequestDto dup = new UserRegisterRequestDto("Copy", "RAHUL.SHARMA@example.com", "secret12", null);
        assertThatThrownBy(() -> userService.registerUser(dup))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("already registered");
    }
}
