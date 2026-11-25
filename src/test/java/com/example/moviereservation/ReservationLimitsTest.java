package com.example.moviereservation;

import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.service.ReservationService;
import com.example.moviereservation.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
class ReservationLimitsTest {

    @Autowired private ReservationService reservationService;
    @Autowired private SeatService seatService;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private UserRepository userRepository;

    private User priya;
    private List<Showtime> upcoming;

    @BeforeEach
    void setUp() {
        priya = userRepository.findByEmail("priya.patel@example.com").orElseThrow();
        upcoming = showtimeRepository.findAll().stream()
                .filter(s -> s.getStartTime().isAfter(LocalDateTime.now().plusMinutes(30)))
                .toList();
    }

    private List<Long> freeSeats(Showtime showtime, int count) {
        return seatService.getSeatsForShowtime(showtime.getId()).stream()
                .filter(s -> "AVAILABLE".equals(s.status()))
                .map(SeatResponse::id)
                .limit(count)
                .toList();
    }

    @Test
    void moreThanTenSeatsIsRejected() {
        Showtime show = upcoming.get(0);
        assertThatThrownBy(() -> reservationService.createReservation(priya.getId(), show.getId(), freeSeats(show, 11)))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("10");
    }

    @Test
    void newHoldOnSameShowReplacesOldHoldAndCanReuseItsSeats() {
        Showtime show = upcoming.get(0);
        List<Long> seats = freeSeats(show, 3);

        Reservation first = reservationService.createReservation(priya.getId(), show.getId(), seats.subList(0, 2));
        // Re-select an overlapping seat: the old hold must be released first, not clash on uk_showtime_seat
        Reservation second = reservationService.createReservation(priya.getId(), show.getId(), seats.subList(1, 3));

        assertThat(reservationService.getReservationById(first.getId()).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void userCannotHoldSeatsOnTooManyShowsAtOnce() {
        for (int i = 0; i < ReservationService.MAX_ACTIVE_HOLDS_PER_USER; i++) {
            Showtime show = upcoming.get(i);
            reservationService.createReservation(priya.getId(), show.getId(), freeSeats(show, 1));
        }
        Showtime extra = upcoming.get(ReservationService.MAX_ACTIVE_HOLDS_PER_USER);

        assertThatThrownBy(() -> reservationService.createReservation(priya.getId(), extra.getId(), freeSeats(extra, 1)))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("pending");
    }
}
