package com.example.moviereservation;

import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.service.TmdbMovieSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TmdbMovieSyncServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TmdbMovieSyncService tmdbMovieSyncService;

    @Test
    @DisplayName("syncNowPlayingMovies throws IllegalArgumentException when no API key is provided")
    void testSyncWithoutApiKeyThrowsException() {
        assertThatThrownBy(() -> tmdbMovieSyncService.syncNowPlayingMovies("", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TMDb API Key is required");
    }

    @Test
    @DisplayName("isApiKeyConfigured returns false when defaultApiKey is empty")
    void testIsApiKeyConfiguredFalseWhenEmpty() {
        assertThat(tmdbMovieSyncService.isApiKeyConfigured()).isFalse();
    }
}
