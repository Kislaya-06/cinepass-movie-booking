package com.example.moviereservation.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie extends BaseEntity {

    @NotBlank(message = "Movie title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Genre is required")
    @Size(max = 100, message = "Genre must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String genre;

    @NotBlank(message = "Language is required")
    @Size(max = 50, message = "Language must not exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String language;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "duration_in_minutes", nullable = false)
    private Integer durationInMinutes;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Size(max = 500, message = "Poster URL must not exceed 500 characters")
    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @DecimalMin(value = "0.0", message = "Rating cannot be less than 0")
    @DecimalMax(value = "10.0", message = "Rating cannot be greater than 10")
    @Column(name = "rating")
    private Double rating;

    /** YouTube video id of the official trailer, from TMDb. */
    @Column(name = "trailer_key", length = 20)
    private String trailerKey;

    @ElementCollection
    @CollectionTable(name = "movie_credits", joinColumns = @JoinColumn(name = "movie_id"))
    @OrderColumn(name = "position")
    @Builder.Default
    private List<MovieCredit> credits = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Showtime> showtimes = new ArrayList<>();

    public List<MovieCredit> getCast() {
        return credits.stream().filter(c -> !c.isCrew()).toList();
    }

    public List<MovieCredit> getCrew() {
        return credits.stream().filter(MovieCredit::isCrew).toList();
    }

    public void addShowtime(Showtime showtime) {
        showtimes.add(showtime);
        showtime.setMovie(this);
    }

    public void removeShowtime(Showtime showtime) {
        showtimes.remove(showtime);
        showtime.setMovie(null);
    }
}
