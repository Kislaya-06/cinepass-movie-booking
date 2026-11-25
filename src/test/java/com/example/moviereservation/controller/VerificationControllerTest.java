package com.example.moviereservation.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationControllerTest {

    @Test
    void maskNameHidesSurname() {
        assertThat(VerificationController.maskName("Rahul Sharma")).isEqualTo("Rahul S.");
        assertThat(VerificationController.maskName("  Priya  Kumari Patel ")).isEqualTo("Priya P.");
        assertThat(VerificationController.maskName("Madonna")).isEqualTo("Madonna");
    }
}
