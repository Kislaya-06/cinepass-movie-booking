package com.example.moviereservation;

import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Role;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.service.ReservationService;
import com.example.moviereservation.service.SeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** Not @Transactional: each booking must commit in its own thread to exercise the row lock. */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
class ReservationConcurrencyTest {

    @Autowired private ReservationService reservationService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void parallelRequestsCannotExceedHoldLimit() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Race Tester").email("race-" + UUID.randomUUID() + "@example.com")
                .password("x").role(Role.USER).build());

        int attempts = ReservationService.MAX_ACTIVE_HOLDS_PER_USER + 2;
        List<Showtime> shows = showtimeRepository.findAll().stream()
                .filter(s -> s.getStartTime().isAfter(LocalDateTime.now().plusMinutes(30)))
                .limit(attempts)
                .toList();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Future<Boolean>> results = shows.stream().map(show -> pool.submit(() -> {
            Long seatId = seatService.getSeatsForShowtime(show.getId()).stream()
                    .filter(s -> "AVAILABLE".equals(s.status())).map(SeatResponse::id).findFirst().orElseThrow();
            start.await();
            try {
                reservationService.createReservation(user.getId(), show.getId(), List.of(seatId));
                return true;
            } catch (Exception e) {
                return false;
            }
        })).toList();

        start.countDown();
        long succeeded = 0;
        try {
            for (Future<Boolean> r : results) {
                if (r.get()) succeeded++;
            }
        } finally {
            pool.shutdown();
            // These holds were committed; release them so other tests can still book those seats
            reservationService.getReservationsForUser(user.getId())
                    .forEach(r -> reservationService.cancelReservation(r.getId(), user.getId()));
        }

        assertThat(succeeded).isEqualTo(ReservationService.MAX_ACTIVE_HOLDS_PER_USER);
    }
}
