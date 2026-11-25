package com.example.moviereservation;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
@Transactional
@WithUserDetails("admin@cinepass.com")
class AdminDashboardTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShowtimeRepository showtimeRepository;

    @Test
    void reservationsTableShowsOnePageAtATime() throws Exception {
        User rahul = userRepository.findByEmail("rahul.sharma@example.com").orElseThrow();
        Showtime show = showtimeRepository.findAll().get(0);
        for (int i = 0; i < 25; i++) {
            reservationRepository.save(Reservation.builder()
                    .bookingReference("PAGE-TEST-" + i).status(ReservationStatus.CANCELLED)
                    .totalAmount(BigDecimal.TEN).user(rahul).showtime(show).build());
        }

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("reservations", hasSize(20)));

        long cancelled = reservationRepository.countByStatus(ReservationStatus.CANCELLED);
        mockMvc.perform(get("/admin/dashboard").param("status", "CANCELLED").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("reservations", hasSize((int) Math.min(20, cancelled - 20))));
    }

    @Test
    void negativePageFallsBackToFirstPage() throws Exception {
        mockMvc.perform(get("/admin/dashboard").param("page", "-3"))
                .andExpect(status().isOk());
    }
}
