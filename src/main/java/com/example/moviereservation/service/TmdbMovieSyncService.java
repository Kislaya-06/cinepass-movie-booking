package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbMovieSyncService {

    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.tmdb.api-key:}")
    private String configuredApiKey;

    @Value("${app.tmdb.base-url:https://api.tmdb.org/3}")
    private String baseUrl;

    @Value("${app.tmdb.image-base-url:https://image.tmdb.org/t/p/w500}")
    private String imageBaseUrl;

    private static final Map<Integer, String> GENRE_MAP = Map.ofEntries(
            Map.entry(28, "Action"),
            Map.entry(12, "Adventure"),
            Map.entry(16, "Animation"),
            Map.entry(35, "Comedy"),
            Map.entry(80, "Crime"),
            Map.entry(99, "Documentary"),
            Map.entry(18, "Drama"),
            Map.entry(10751, "Family"),
            Map.entry(14, "Fantasy"),
            Map.entry(36, "History"),
            Map.entry(27, "Horror"),
            Map.entry(10402, "Music"),
            Map.entry(9648, "Mystery"),
            Map.entry(10749, "Romance"),
            Map.entry(878, "Sci-Fi"),
            Map.entry(10770, "TV Movie"),
            Map.entry(53, "Thriller"),
            Map.entry(10752, "War"),
            Map.entry(37, "Western")
    );

    private static final Map<String, String> LANGUAGE_MAP = Map.of(
            "en", "English",
            "hi", "Hindi",
            "es", "Spanish",
            "fr", "French",
            "de", "German",
            "ja", "Japanese",
            "ko", "Korean",
            "zh", "Chinese",
            "ta", "Tamil",
            "te", "Telugu"
    );

    @Getter
    @Builder
    public static class SyncResult {
        private final int importedCount;
        private final int skippedCount;
        private final int showtimesCreated;
        private final String message;
    }

    public boolean isApiKeyConfigured() {
        return configuredApiKey != null && !configuredApiKey.isBlank();
    }

    @Transactional
    public SyncResult syncNowPlayingMovies(String overrideApiKey, boolean autoScheduleShows) {
        String activeKey = (overrideApiKey != null && !overrideApiKey.isBlank())
                ? overrideApiKey.trim()
                : configuredApiKey;

        if (activeKey == null || activeKey.isBlank()) {
            throw new IllegalArgumentException("TMDb API Key is required. Please provide it in the admin modal or set TMDB_API_KEY in application.yml.");
        }

        // Use api.tmdb.org to avoid regional ISP SSL handshake terminations on api.themoviedb.org
        String effectiveBaseUrl = (baseUrl != null && baseUrl.contains("api.themoviedb.org"))
                ? baseUrl.replace("api.themoviedb.org", "api.tmdb.org")
                : (baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.tmdb.org/3");

        RestClient client = RestClient.builder()
                .baseUrl(effectiveBaseUrl)
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json")
                .build();

        // Now playing fills "Now showing"; upcoming fills "Coming soon" on the home page
        List<String> responses = new ArrayList<>();
        try {
            for (String path : List.of("/movie/now_playing", "/movie/upcoming")) {
                responses.add(client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(path)
                                .queryParam("api_key", activeKey)
                                .queryParam("language", "en-US")
                                .queryParam("page", 1)
                                .build())
                        .retrieve()
                        .body(String.class));
            }
        } catch (Exception e) {
            log.error("Failed to connect to TMDb API: {}", e.getMessage(), e);
            // e.getMessage() can contain the request URL with ?api_key=..., so keep it out of the UI
            throw new RuntimeException("TMDb API call failed. Please verify your API Key and network connection.", e);
        }

        int imported = 0;
        int skipped = 0;
        int showtimesAdded = 0;

        List<Screen> availableScreens = screenRepository.findAll();

        try {
            for (String jsonResponse : responses) {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.path("results");

            if (results.isArray()) {
                for (JsonNode item : results) {
                    String title = item.path("title").asText("").trim();
                    if (title.isBlank()) {
                        continue;
                    }

                    // Avoid duplicate movies in catalog
                    if (movieRepository.existsByTitleIgnoreCase(title)) {
                        skipped++;
                        continue;
                    }

                    int tmdbId = item.path("id").asInt(0);
                    String overview = item.path("overview").asText("");
                    if (overview.length() > 2000) {
                        overview = overview.substring(0, 1997) + "...";
                    }

                    double voteAverage = item.path("vote_average").asDouble(7.5);
                    double rating = Math.min(10.0, Math.max(1.0, Math.round(voteAverage * 10.0) / 10.0));

                    String posterPath = item.path("poster_path").asText(null);
                    String posterUrl = (posterPath != null && !posterPath.isBlank())
                            ? (imageBaseUrl + posterPath)
                            : "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop";

                    String releaseDateStr = item.path("release_date").asText(null);
                    LocalDate releaseDate = LocalDate.now();
                    if (releaseDateStr != null && !releaseDateStr.isBlank()) {
                        try {
                            releaseDate = LocalDate.parse(releaseDateStr);
                        } catch (Exception ignored) {
                        }
                    }

                    String langCode = item.path("original_language").asText("en").toLowerCase();
                    String langName = java.util.Locale.forLanguageTag(langCode).getDisplayLanguage(java.util.Locale.ENGLISH);
                    String language = LANGUAGE_MAP.getOrDefault(langCode, langName.isEmpty() ? langCode.toUpperCase() : langName);

                    List<String> genresList = new ArrayList<>();
                    JsonNode genreIds = item.path("genre_ids");
                    if (genreIds.isArray()) {
                        for (JsonNode gid : genreIds) {
                            String gName = GENRE_MAP.get(gid.asInt());
                            if (gName != null) {
                                genresList.add(gName);
                            }
                        }
                    }
                    String genre = genresList.isEmpty() ? "Drama" : String.join(", ", genresList);

                    // Fetch accurate runtime from movie details if possible
                    int runtimeMinutes = fetchRuntime(client, activeKey, tmdbId);

                    Movie movie = Movie.builder()
                            .title(title)
                            .description(overview)
                            .genre(genre)
                            .language(language)
                            .durationInMinutes(runtimeMinutes > 0 ? runtimeMinutes : 120)
                            .releaseDate(releaseDate)
                            .posterUrl(posterUrl)
                            .rating(rating)
                            .build();

                    Movie saved = movieRepository.save(movie);
                    imported++;

                    // Auto-schedule sample shows if requested; unreleased films get none
                    if (autoScheduleShows && !availableScreens.isEmpty() && !releaseDate.isAfter(LocalDate.now())) {
                        showtimesAdded += scheduleSampleShowtimes(saved, availableScreens, imported);
                    }
                }
            }
            }
        } catch (Exception e) {
            log.error("Error processing TMDb JSON response", e);
            throw new RuntimeException("Error parsing movie data from TMDb: " + e.getMessage(), e);
        }

        return SyncResult.builder()
                .importedCount(imported)
                .skippedCount(skipped)
                .showtimesCreated(showtimesAdded)
                .message(String.format("Synced %d live movies from TMDb (%d already existed). %d showtimes scheduled.",
                        imported, skipped, showtimesAdded))
                .build();
    }

    private int fetchRuntime(RestClient client, String apiKey, int tmdbId) {
        if (tmdbId <= 0) {
            return 120;
        }
        try {
            String detailJson = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/movie/" + tmdbId)
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (detailJson != null) {
                JsonNode detailNode = objectMapper.readTree(detailJson);
                int runtime = detailNode.path("runtime").asInt(0);
                if (runtime > 0) {
                    return runtime;
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch runtime for tmdbId {}: {}", tmdbId, e.getMessage());
        }
        return 120;
    }

    private int scheduleSampleShowtimes(Movie movie, List<Screen> screens, int movieIndex) {
        // ponytail: Naive round-robin screen allocation for new movies. In production, multiplex managers map shows via admin UI.
        Screen screen = screens.get((movieIndex - 1) % screens.size());
        LocalDate today = LocalDate.now();

        List<Showtime> shows = List.of(
                Showtime.builder()
                        .movie(movie)
                        .screen(screen)
                        .startTime(LocalDateTime.of(today, LocalTime.of(18, 30)))
                        .endTime(LocalDateTime.of(today, LocalTime.of(18, 30)).plusMinutes(movie.getDurationInMinutes()))
                        .price(BigDecimal.valueOf(280.00))
                        .build(),
                Showtime.builder()
                        .movie(movie)
                        .screen(screen)
                        .startTime(LocalDateTime.of(today.plusDays(1), LocalTime.of(21, 15)))
                        .endTime(LocalDateTime.of(today.plusDays(1), LocalTime.of(21, 15)).plusMinutes(movie.getDurationInMinutes()))
                        .price(BigDecimal.valueOf(320.00))
                        .build()
        );

        showtimeRepository.saveAll(shows);
        return shows.size();
    }
}
