package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Theater;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.TheaterRepository;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.service.ScreenService;
import com.example.moviereservation.service.TmdbMovieSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final ObjectMapper objectMapper;
    private final TmdbMovieSyncService tmdbMovieSyncService;
    private final ScreenService screenService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(
            @RequestParam(value = "status", required = false) ReservationStatus statusFilter,
            Model model
    ) {
        long totalMovies = movieRepository.count();
        long totalTheaters = theaterRepository.count();
        long totalScreens = screenRepository.count();
        long totalShowtimes = showtimeRepository.count();
        long totalReservations = reservationRepository.count();

        List<Reservation> allReservations = (statusFilter != null)
                ? reservationRepository.findByStatus(statusFilter)
                : reservationRepository.findAll();

        BigDecimal totalRevenue = reservationRepository.sumTotalAmountByStatus(ReservationStatus.CONFIRMED);

        long countAll = totalReservations;
        long countConfirmed = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
        long countPending = reservationRepository.countByStatus(ReservationStatus.PENDING);
        long countCancelled = reservationRepository.countByStatus(ReservationStatus.CANCELLED);
        long countExpired = reservationRepository.countByStatus(ReservationStatus.EXPIRED);

        // Compute 7-day trends for Chart.js
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM");
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartBookings = new ArrayList<>();
        List<BigDecimal> chartRevenue = new ArrayList<>();

        LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();
        List<Reservation> last7DaysReservations = reservationRepository.findByCreatedAtAfter(sevenDaysAgo);

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            chartLabels.add(day.format(dayFmt));

            long countForDay = last7DaysReservations.stream()
                    .filter(r -> r.getCreatedAt().toLocalDate().equals(day))
                    .count();
            chartBookings.add(countForDay);

            BigDecimal revenueForDay = last7DaysReservations.stream()
                    .filter(r -> r.getCreatedAt().toLocalDate().equals(day) && r.getStatus() == ReservationStatus.CONFIRMED)
                    .map(Reservation::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            chartRevenue.add(revenueForDay);
        }

        List<Object[]> topMoviesRaw = reservationRepository.findTopMoviesByReservationCount();
        List<String> movieTitles = new ArrayList<>();
        List<Long> movieCounts = new ArrayList<>();
        for (Object[] row : topMoviesRaw) {
            movieTitles.add((String) row[0]);
            movieCounts.add((Long) row[1]);
        }
        if (movieTitles.isEmpty()) {
            movieTitles.add("Dune: Part Two");
            movieCounts.add(0L);
        }

        List<Movie> movies = movieRepository.findAll();
        List<Theater> theaters = theaterRepository.findAll();
        List<Screen> screens = screenRepository.findAll();
        List<Showtime> showtimes = showtimeRepository.findAll();

        model.addAttribute("totalMovies", totalMovies);
        model.addAttribute("totalTheaters", totalTheaters);
        model.addAttribute("totalScreens", totalScreens);
        model.addAttribute("totalShowtimes", totalShowtimes);
        model.addAttribute("totalReservations", totalReservations);
        model.addAttribute("totalRevenue", totalRevenue);

        model.addAttribute("countAll", countAll);
        model.addAttribute("countConfirmed", countConfirmed);
        model.addAttribute("countPending", countPending);
        model.addAttribute("countCancelled", countCancelled);
        model.addAttribute("countExpired", countExpired);

        try {
            model.addAttribute("chartLabelsJson", objectMapper.writeValueAsString(chartLabels));
            model.addAttribute("chartBookingsJson", objectMapper.writeValueAsString(chartBookings));
            model.addAttribute("chartRevenueJson", objectMapper.writeValueAsString(chartRevenue));
            model.addAttribute("chartMovieTitlesJson", objectMapper.writeValueAsString(movieTitles));
            model.addAttribute("chartMovieCountsJson", objectMapper.writeValueAsString(movieCounts));
        } catch (Exception e) {
            log.error("Failed to serialize chart data to JSON", e);
        }

        model.addAttribute("movies", movies);
        model.addAttribute("theaters", theaters);
        model.addAttribute("screens", screens);
        model.addAttribute("showtimes", showtimes);
        model.addAttribute("reservations", allReservations);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("pageTitle", "Admin Console - CinePass");
        model.addAttribute("isTmdbConfigured", tmdbMovieSyncService.isApiKeyConfigured());

        return "admin-dashboard";
    }

    @PostMapping("/movies/sync-tmdb")
    public String syncTmdb(
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "autoSchedule", defaultValue = "true") boolean autoSchedule,
            RedirectAttributes redirectAttributes
    ) {
        try {
            TmdbMovieSyncService.SyncResult result = tmdbMovieSyncService.syncNowPlayingMovies(apiKey, autoSchedule);
            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
        } catch (Exception e) {
            log.error("Failed to sync movies from TMDb", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dashboard#movies";
    }

    @PostMapping("/movies")
    public String addMovie(
            @RequestParam("title") String title,
            @RequestParam("genre") String genre,
            @RequestParam("language") String language,
            @RequestParam("durationInMinutes") Integer duration,
            @RequestParam(value = "rating", required = false, defaultValue = "8.0") Double rating,
            @RequestParam(value = "posterUrl", required = false) String posterUrl,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        Movie movie = Movie.builder()
                .title(title.trim())
                .genre(genre.trim())
                .language(language.trim())
                .durationInMinutes(duration)
                .rating(rating)
                .posterUrl(posterUrl != null && !posterUrl.isBlank() ? posterUrl.trim() : "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop")
                .description(description)
                .releaseDate(LocalDate.now())
                .build();

        movieRepository.save(movie);
        redirectAttributes.addFlashAttribute("successMessage", "Movie '" + title + "' added successfully!");
        return "redirect:/admin/dashboard#movies";
    }

    @PostMapping("/movies/{id}/delete")
    public String deleteMovie(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movieRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Movie deleted successfully.");
        return "redirect:/admin/dashboard#movies";
    }

    @PostMapping("/theaters")
    public String addTheater(
            @RequestParam("name") String name,
            @RequestParam("location") String location,
            @RequestParam("city") String city,
            RedirectAttributes redirectAttributes
    ) {
        Theater theater = Theater.builder()
                .name(name.trim())
                .location(location.trim())
                .city(city.trim())
                .build();
        theaterRepository.save(theater);
        redirectAttributes.addFlashAttribute("successMessage", "Theater added successfully!");
        return "redirect:/admin/dashboard#theaters";
    }

    @PostMapping("/screens")
    public String addScreen(
            @RequestParam("theaterId") Long theaterId,
            @RequestParam("screenNumber") int screenNumber,
            @RequestParam("rows") int rows,
            @RequestParam("seatsPerRow") int seatsPerRow,
            @RequestParam(value = "reclinerRows", defaultValue = "0") int reclinerRows,
            @RequestParam(value = "premiumRows", defaultValue = "0") int premiumRows,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Screen screen = screenService.createScreen(theaterId, screenNumber, rows, seatsPerRow, reclinerRows, premiumRows);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Screen " + screen.getScreenNumber() + " added with " + screen.getSeatCapacity() + " seats.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/dashboard#theaters";
    }

    @PostMapping("/showtimes")
    public String addShowtime(
            @RequestParam("movieId") Long movieId,
            @RequestParam("screenId") Long screenId,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam("price") BigDecimal price,
            RedirectAttributes redirectAttributes
    ) {
        Movie movie = movieRepository.findById(movieId).orElseThrow();
        Screen screen = screenRepository.findById(screenId).orElseThrow();

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .screen(screen)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(movie.getDurationInMinutes() != null ? movie.getDurationInMinutes() : 120))
                .price(price)
                .build();

        showtimeRepository.save(showtime);
        redirectAttributes.addFlashAttribute("successMessage", "Showtime scheduled successfully!");
        return "redirect:/admin/dashboard#shows";
    }
}
