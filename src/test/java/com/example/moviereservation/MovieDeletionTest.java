package com.example.moviereservation;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
class MovieDeletionTest {

    @Autowired private MovieService movieService;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ReservationRepository reservationRepository;

    @Test
    void movieWithBookingsCannotBeDeletedAndBookingsSurvive() {
        // Seeded sample booking CP-928410 is for Dune
        Movie dune = reservationRepository.findByBookingReference("CP-928410").orElseThrow().getShowtime().getMovie();
        long bookingsBefore = reservationRepository.count();

        assertThatThrownBy(() -> movieService.deleteMovie(dune.getId()))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("booking");

        movieRepository.flush();
        assertThat(movieRepository.existsById(dune.getId())).isTrue();
        assertThat(reservationRepository.count()).isEqualTo(bookingsBefore);
    }

    @Test
    void movieWithoutBookingsIsDeleted() {
        Movie fresh = movieRepository.save(Movie.builder()
                .title("Temp").genre("Drama").language("Hindi").durationInMinutes(100)
                .releaseDate(LocalDate.now()).build());

        movieService.deleteMovie(fresh.getId());

        assertThat(movieRepository.existsById(fresh.getId())).isFalse();
    }
}
