package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/")
    public String index(
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "search", required = false) String search,
            Model model
    ) {
        List<Movie> movies;
        if (search != null && !search.isBlank()) {
            movies = movieService.searchMoviesByTitle(search.trim());
        } else if (genre != null && !genre.isBlank() && !"all".equalsIgnoreCase(genre)) {
            movies = movieService.getMoviesByGenre(genre.trim());
        } else {
            movies = movieService.getAllMovies();
        }

        LocalDate now = LocalDate.now();
        List<Movie> nowShowing = movies.stream()
                .filter(m -> m.getReleaseDate() == null || !m.getReleaseDate().isAfter(now))
                .toList();

        List<Movie> comingSoon = movies.stream()
                .filter(m -> m.getReleaseDate() != null && m.getReleaseDate().isAfter(now))
                .toList();

        // No fallbacks: filling an empty list with all movies showed the same films in both rows
        model.addAttribute("featuredMovie", movies.isEmpty() ? null : movies.get(0));
        model.addAttribute("nowShowing", nowShowing);
        model.addAttribute("comingSoon", comingSoon);
        model.addAttribute("activeGenre", genre != null ? genre : "all");
        model.addAttribute("pageTitle", "CinePass - Book Movie Tickets Online");

        return "index";
    }

    @GetMapping("/movies/{id}")
    public String movieDetails(
            @PathVariable Long id,
            @RequestParam(value = "city", required = false) String cityParam,
            @org.springframework.web.bind.annotation.CookieValue(value = "selected_city", defaultValue = "Mumbai") String cookieCity,
            Model model
    ) {
        String effectiveCity = (cityParam != null && !cityParam.isBlank()) ? cityParam.trim() : cookieCity;
        Movie movie = movieService.getMovieById(id);
        Map<LocalDate, Map<String, List<Showtime>>> groupedShowtimes =
                movieService.getGroupedShowtimesForMovie(id, effectiveCity);

        model.addAttribute("movie", movie);
        model.addAttribute("groupedShowtimes", groupedShowtimes);
        model.addAttribute("dates", groupedShowtimes.keySet());
        model.addAttribute("selectedCity", effectiveCity);
        model.addAttribute("pageTitle", movie.getTitle() + " - Theaters in " + effectiveCity);

        return "movie-details";
    }

    @GetMapping("/error-page")
    public String errorDemo(Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", "The page or showtime you requested could not be located.");
        model.addAttribute("pageTitle", "404 - Page Not Found");
        return "error";
    }
}
