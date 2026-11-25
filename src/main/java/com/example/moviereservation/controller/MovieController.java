package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.repository.ReservationSeatRepository;
import com.example.moviereservation.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final ReservationSeatRepository reservationSeatRepository;

    @GetMapping("/")
    public String index(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.moviereservation.security.CustomUserDetails user,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "search", required = false) String search,
            Model model
    ) {
        if (isAdmin(user)) {
            return "redirect:/admin/dashboard";
        }
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
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.moviereservation.security.CustomUserDetails user,
            @PathVariable Long id,
            @RequestParam(value = "city", required = false) String cityParam,
            @org.springframework.web.bind.annotation.CookieValue(value = "selected_city", defaultValue = "Mumbai") String cookieCity,
            Model model
    ) {
        if (isAdmin(user)) {
            return "redirect:/admin/dashboard";
        }
        String effectiveCity = (cityParam != null && !cityParam.isBlank()) ? cityParam.trim() : cookieCity;
        Movie movie = movieService.getMovieById(id);
        Map<LocalDate, Map<String, List<Showtime>>> groupedShowtimes =
                movieService.getGroupedShowtimesForMovie(id, effectiveCity);

        // "You might also like": released films sharing the first genre
        String firstGenre = movie.getGenre() == null ? "" : movie.getGenre().split(",")[0].trim();
        LocalDate today = LocalDate.now();
        List<Movie> similar = firstGenre.isEmpty() ? List.of() : movieService.getMoviesByGenre(firstGenre).stream()
                .filter(m -> !m.getId().equals(id))
                .filter(m -> m.getReleaseDate() == null || !m.getReleaseDate().isAfter(today))
                .limit(10)
                .toList();

        model.addAttribute("movie", movie);
        model.addAttribute("hasShows", !groupedShowtimes.isEmpty());
        model.addAttribute("comingSoon", movie.getReleaseDate() != null && movie.getReleaseDate().isAfter(today));
        model.addAttribute("similarMovies", similar);
        model.addAttribute("selectedCity", effectiveCity);
        model.addAttribute("pageTitle", movie.getTitle() + " - CinePass");

        return "movie-details";
    }

    /** The "Book tickets" step: one day's showtimes, theater by theater. */
    @GetMapping("/movies/{id}/buytickets")
    public String buyTickets(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.moviereservation.security.CustomUserDetails user,
            @PathVariable Long id,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "city", required = false) String cityParam,
            @org.springframework.web.bind.annotation.CookieValue(value = "selected_city", defaultValue = "Mumbai") String cookieCity,
            @org.springframework.web.bind.annotation.CookieValue(value = "selected_area", required = false) String area,
            Model model
    ) {
        if (isAdmin(user)) {
            return "redirect:/admin/dashboard";
        }
        String effectiveCity = (cityParam != null && !cityParam.isBlank()) ? cityParam.trim() : cookieCity;
        Movie movie = movieService.getMovieById(id);
        Map<LocalDate, Map<String, List<Showtime>>> grouped = movieService.getGroupedShowtimesForMovie(id, effectiveCity);

        LocalDate today = LocalDate.now();
        List<LocalDate> days = IntStream.range(0, 7).mapToObj(today::plusDays).toList();
        LocalDate selected = date != null && days.contains(date) ? date
                : grouped.keySet().stream().findFirst().orElse(today);
        Map<String, List<Showtime>> theaters = grouped.getOrDefault(selected, Map.of());
        if (area != null && !area.isBlank()) {
            // Cinemas in the picked area (e.g. Noida within Delhi-NCR) come first
            String needle = area.trim().toLowerCase();
            theaters = theaters.entrySet().stream()
                    .sorted(java.util.Comparator.comparing(e -> !(e.getKey() + " " + e.getValue().get(0).getScreen().getTheater().getLocation())
                            .toLowerCase().contains(needle)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, java.util.LinkedHashMap::new));
        }

        List<Long> ids = theaters.values().stream().flatMap(List::stream).map(Showtime::getId).toList();
        Map<Long, Long> booked = ids.isEmpty() ? Map.of() : reservationSeatRepository
                .countBookedSeatsByShowtime(ids, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        // Half the hall gone counts as fast filling
        Set<Long> fastFilling = theaters.values().stream().flatMap(List::stream)
                .filter(st -> st.getScreen().getSeatCapacity() != null
                        && booked.getOrDefault(st.getId(), 0L) * 2 >= st.getScreen().getSeatCapacity())
                .map(Showtime::getId)
                .collect(Collectors.toSet());
        Set<Long> soldOut = theaters.values().stream().flatMap(List::stream)
                .filter(st -> st.getScreen().getSeatCapacity() != null
                        && booked.getOrDefault(st.getId(), 0L) >= st.getScreen().getSeatCapacity())
                .map(Showtime::getId)
                .collect(Collectors.toSet());
        // Hover hint on each time: what every seat tier costs for that show
        Map<Long, String> priceHints = theaters.values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(Showtime::getId, st -> java.util.Arrays.stream(com.example.moviereservation.entity.SeatType.values())
                        .map(t -> t.name() + " ₹" + t.calculatePrice(st.getPrice()).stripTrailingZeros().toPlainString())
                        .collect(Collectors.joining("  ·  "))));

        model.addAttribute("movie", movie);
        model.addAttribute("soldOut", soldOut);
        model.addAttribute("priceHints", priceHints);
        model.addAttribute("days", days);
        model.addAttribute("daysWithShows", grouped.keySet());
        model.addAttribute("selectedDate", selected);
        model.addAttribute("theaters", theaters);
        model.addAttribute("fastFilling", fastFilling);
        model.addAttribute("selectedArea", area);
        model.addAttribute("selectedCity", effectiveCity);
        model.addAttribute("pageTitle", movie.getTitle() + " showtimes in " + effectiveCity + " - CinePass");

        return "buy-tickets";
    }

    // Admins run the cinema from the dashboard; the customer storefront is not theirs
    private static boolean isAdmin(com.example.moviereservation.security.CustomUserDetails user) {
        return user != null && user.getRole() == com.example.moviereservation.entity.Role.ADMIN;
    }

    @GetMapping("/error-page")
    public String errorDemo(Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", "The page or showtime you requested could not be located.");
        model.addAttribute("pageTitle", "404 - Page Not Found");
        return "error";
    }
}
