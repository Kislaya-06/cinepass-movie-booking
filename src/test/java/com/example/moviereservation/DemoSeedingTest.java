package com.example.moviereservation;

import com.example.moviereservation.config.DataLoader;
import com.example.moviereservation.entity.Role;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
class DemoSeedingTest {

    @Autowired private DataLoader dataLoader;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void adminIsCreatedByBootstrap() {
        assertThat(userRepository.findByEmail("admin@cinepass.com"))
                .get().extracting(u -> u.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @Transactional // rolled back, other tests keep the original schedule
    void topUpAddsShowsWhenScheduleRanOut() {
        LocalDateTime now = LocalDateTime.now();
        showtimeRepository.findAll().forEach(s -> s.setStartTime(now.minusDays(3)));
        showtimeRepository.flush();
        assertThat(showtimeRepository.existsByStartTimeBetween(now, now.plusHours(24))).isFalse();

        dataLoader.topUpShowtimes();

        assertThat(showtimeRepository.existsByStartTimeBetween(now, now.plusHours(24))).isTrue();
    }
}
