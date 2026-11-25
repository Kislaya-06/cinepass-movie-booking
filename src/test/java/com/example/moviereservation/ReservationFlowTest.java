package com.example.moviereservation;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.SeatType;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.exception.SeatAlreadyBookedException;
import com.example.moviereservation.exception.UnauthorizedActionException;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
class ReservationFlowTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private com.example.moviereservation.service.MovieService movieService;

    @Autowired
    private com.example.moviereservation.repository.MovieRepository movieRepository;

    @Test
    @DisplayName("SeatType calculates dynamic pricing with multipliers correctly")
    void testSeatPricingMultipliers() {
        BigDecimal basePrice = new BigDecimal("200.00");

        assertThat(SeatType.REGULAR.calculatePrice(basePrice))
                .isEqualByComparingTo(new BigDecimal("200.00"));

        assertThat(SeatType.PREMIUM.calculatePrice(basePrice))
                .isEqualByComparingTo(new BigDecimal("300.00"));

        assertThat(SeatType.RECLINER.calculatePrice(basePrice))
                .isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    @Transactional
    @DisplayName("Full reservation lifecycle: hold -> conflict -> cancel releases hold -> rebook -> confirm")
    void testReservationHoldConflictCancellationAndRebooking() {
        User rahul = userRepository.findByEmail("rahul.sharma@example.com").orElseThrow();
        User priya = userRepository.findByEmail("priya.patel@example.com").orElseThrow();

        // Use Dune showtime 1
        Showtime showtime = showtimeRepository.findAll().stream()
                .filter(s -> s.getMovie().getTitle().contains("Dune"))
                .findFirst()
                .orElseThrow();

        // Pick seat A1 (Recliner) on this screen
        Long seatA1Id = seatRepository.findByScreenIdAndSeatRowAndSeatNumber(showtime.getScreen().getId(), "A", 1)
                .orElseThrow()
                .getId();

        // 1. Rahul creates a pending hold on seat A1
        Reservation rahulRes = reservationService.createReservation(rahul.getId(), showtime.getId(), List.of(seatA1Id));
        assertThat(rahulRes.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(rahulRes.getSeatSummary()).isEqualTo("A1");
        assertThat(rahulRes.getTotalAmount()).isEqualByComparingTo(SeatType.RECLINER.calculatePrice(showtime.getPrice()));

        // 2. Priya tries to double-book seat A1 while Rahul holds it -> Conflict
        assertThatThrownBy(() -> reservationService.createReservation(priya.getId(), showtime.getId(), List.of(seatA1Id)))
                .isInstanceOf(SeatAlreadyBookedException.class);

        // 3. Rahul cancels his reservation -> Releases the seat hold
        reservationService.cancelReservation(rahulRes.getId(), rahul.getId());
        Reservation cancelledRahulRes = reservationService.getReservationById(rahulRes.getId());
        assertThat(cancelledRahulRes.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        // Seat summary preserved for audit/history
        assertThat(cancelledRahulRes.getSeatSummary()).isEqualTo("A1");

        // 4. Verification of uk_showtime_seat bugfix: Priya can now successfully re-book seat A1!
        Reservation priyaRes = reservationService.createReservation(priya.getId(), showtime.getId(), List.of(seatA1Id));
        assertThat(priyaRes).isNotNull();
        assertThat(priyaRes.getUser().getId()).isEqualTo(priya.getId());
        assertThat(priyaRes.getStatus()).isEqualTo(ReservationStatus.PENDING);

        // Rahul must not be able to confirm Priya's reservation (IDOR guard)
        assertThatThrownBy(() -> reservationService.confirmReservation(priyaRes.getId(), rahul.getId()))
                .isInstanceOf(UnauthorizedActionException.class);

        Reservation confirmedPriyaRes = reservationService.confirmReservation(priyaRes.getId(), priya.getId());
        assertThat(confirmedPriyaRes.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmedPriyaRes.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("getGroupedShowtimesForMovie filters showtimes strictly by city")
    void testCityShowtimeFiltering() {
        com.example.moviereservation.entity.Movie movie = movieRepository.findAll().get(0);

        // Mumbai showtimes should exist
        var mumbaiShows = movieService.getGroupedShowtimesForMovie(movie.getId(), "Mumbai");
        assertThat(mumbaiShows).isNotEmpty();

        // Delhi-NCR showtimes should exist
        var delhiShows = movieService.getGroupedShowtimesForMovie(movie.getId(), "Delhi-NCR");
        assertThat(delhiShows).isNotEmpty();

        // Non-existent city should return empty map
        var fakeCityShows = movieService.getGroupedShowtimesForMovie(movie.getId(), "NonExistentCityXYZ");
        assertThat(fakeCityShows).isEmpty();
    }
}
