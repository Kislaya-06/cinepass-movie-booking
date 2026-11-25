package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.MovieCredit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbDetailsTest {

    @Test
    void copiesRuntimeCastCrewAndOfficialTrailer() throws Exception {
        String json = """
                {"runtime": 148,
                 "credits": {
                   "cast": [{"name": "Lead Actor", "character": "Hero", "profile_path": "/a.jpg"},
                            {"name": "No Photo", "character": "Villain", "profile_path": null}],
                   "crew": [{"name": "Jane Doe", "job": "Director"},
                            {"name": "Jane Doe", "job": "Screenplay"},
                            {"name": "Grip Guy", "job": "Key Grip"},
                            {"name": "Composer", "job": "Original Music Composer"}]},
                 "videos": {"results": [
                   {"site": "YouTube", "type": "Teaser", "key": "teaser1", "official": true},
                   {"site": "YouTube", "type": "Trailer", "key": "fanTrailer", "official": false},
                   {"site": "Vimeo", "type": "Trailer", "key": "vimeo1", "official": true},
                   {"site": "YouTube", "type": "Trailer", "key": "official1", "official": true}]}}
                """;
        Movie movie = Movie.builder().title("T").durationInMinutes(120).build();

        TmdbMovieSyncService.applyDetails(movie, new ObjectMapper().readTree(json));

        assertThat(movie.getDurationInMinutes()).isEqualTo(148);
        assertThat(movie.getCast()).extracting(MovieCredit::getName).containsExactly("Lead Actor", "No Photo");
        assertThat(movie.getCast().get(0).getPhotoUrl()).isEqualTo("https://image.tmdb.org/t/p/w185/a.jpg");
        assertThat(movie.getCast().get(1).getPhotoUrl()).isNull();
        // the writer-director appears once, as director; grips are left out
        assertThat(movie.getCrew()).extracting(MovieCredit::getRole).containsExactly("Director", "Original Music Composer");
        assertThat(movie.getTrailerKey()).isEqualTo("official1");
    }

    @Test
    void missingDetailsLeaveMovieUntouched() {
        Movie movie = Movie.builder().title("T").durationInMinutes(120).build();
        TmdbMovieSyncService.applyDetails(movie, null);
        assertThat(movie.getDurationInMinutes()).isEqualTo(120);
        assertThat(movie.getCredits()).isEmpty();
        assertThat(movie.getTrailerKey()).isNull();
    }
}
