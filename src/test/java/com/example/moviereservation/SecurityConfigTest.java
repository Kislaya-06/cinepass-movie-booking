package com.example.moviereservation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.example.moviereservation.repository.ReservationRepository reservationRepository;

    @Test
    @WithUserDetails("admin@cinepass.com")
    void adminIsKeptOutOfTheBookingFlow() throws Exception {
        mockMvc.perform(get("/")).andExpect(header().string("Location", "/admin/dashboard"));
        mockMvc.perform(get("/my-bookings")).andExpect(status().isForbidden());
        mockMvc.perform(get("/showtimes/1/seats")).andExpect(status().isForbidden());
    }

    @Test
    void healthIsPublicAndFramesAreSameOriginOnly() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void loginPageNeverShowsAdminPassword() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("admin123"))))
                .andExpect(content().string(containsString("rahul.sharma@example.com"))); // dev shows demo user
    }

    @Test
    void metricsRequireLogin() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "USER")
    void metricsForbiddenForNormalUser() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin@cinepass.com") // real CustomUserDetails, the navbar reads principal.name
    void unexpectedErrorsDoNotLeakRawMessage() throws Exception {
        // Non-numeric id -> MethodArgumentTypeMismatchException, whose message names Java types
        mockMvc.perform(post("/admin/showtimes").with(csrf())
                        .param("movieId", "abc")
                        .param("screenId", "1")
                        .param("startTime", "2030-01-01T18:00:00")
                        .param("price", "200"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("java.lang"))));
    }

    @Test
    @WithUserDetails("rahul.sharma@example.com")
    @org.springframework.transaction.annotation.Transactional
    void paymentPageRedirectsForCancelledBooking() throws Exception {
        // Seeded sample booking CP-928410 belongs to Rahul and is CONFIRMED, i.e. not payable
        Long id = reservationRepository.findByBookingReference("CP-928410").orElseThrow().getId();
        mockMvc.perform(get("/reservations/" + id + "/payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/my-bookings"));
    }

    @Test
    @WithUserDetails("rahul.sharma@example.com")
    void unknownUrlIsNotFound() throws Exception {
        mockMvc.perform(get("/no-such-page"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin@cinepass.com")
    void unknownMovieIdIsNotFound() throws Exception {
        mockMvc.perform(post("/admin/showtimes").with(csrf())
                        .param("movieId", "999999")
                        .param("screenId", "1")
                        .param("startTime", "2030-01-01T18:00:00")
                        .param("price", "200"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void metricsAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }
}
