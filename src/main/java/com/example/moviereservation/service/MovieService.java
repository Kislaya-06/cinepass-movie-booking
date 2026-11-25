package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.exception.ResourceNotFoundException;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with ID: " + id));
    }

    /** Deletes a movie and its showtimes, but only if nobody ever booked it (booking history is kept). */
    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = getMovieById(id);
        long bookings = reservationRepository.countByShowtime_Movie_Id(id);
        if (bookings > 0) {
            throw new ApplicationException("'" + movie.getTitle() + "' has " + bookings
                    + " booking(s) and can't be deleted. Stop scheduling new shows for it instead.");
        }
        movieRepository.delete(movie); // cascades to its (booking-free) showtimes
    }

    public List<Movie> searchMoviesByTitle(String query) {
        return movieRepository.findByTitleContainingIgnoreCase(query);
    }

    public List<Movie> getMoviesByGenre(String genre) {
        return movieRepository.findByGenreContainingIgnoreCase(genre);
    }

    public List<Showtime> getUpcomingShowtimesForMovie(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie not found with ID: " + movieId);
        }
        return showtimeRepository.findByMovieIdAndStartTimeAfter(movieId, LocalDateTime.now());
    }

    public Map<LocalDate, Map<String, List<Showtime>>> getGroupedShowtimesForMovie(Long movieId) {
        return getGroupedShowtimesForMovie(movieId, null);
    }

    public Map<LocalDate, Map<String, List<Showtime>>> getGroupedShowtimesForMovie(Long movieId, String city) {
        List<Showtime> upcoming = getUpcomingShowtimesForMovie(movieId);

        if (city != null && !city.isBlank() && !"all".equalsIgnoreCase(city.trim())) {
            String targetCity = city.trim();
            upcoming = upcoming.stream()
                    .filter(s -> s.getScreen() != null
                            && s.getScreen().getTheater() != null
                            && s.getScreen().getTheater().getCity() != null
                            && s.getScreen().getTheater().getCity().equalsIgnoreCase(targetCity))
                    .toList();
        }

        return upcoming.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStartTime().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                s -> s.getScreen() != null && s.getScreen().getTheater() != null
                                        ? s.getScreen().getTheater().getName()
                                        : "Unknown Theater",
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));
    }
}
