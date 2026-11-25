package com.example.moviereservation.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen extends BaseEntity {

    @NotNull(message = "Screen number is required")
    @Min(value = 1, message = "Screen number must be at least 1")
    @Column(name = "screen_number", nullable = false)
    private Integer screenNumber;

    @NotNull(message = "Seat capacity is required")
    @Min(value = 1, message = "Seat capacity must be at least 1")
    @Column(name = "seat_capacity", nullable = false)
    private Integer seatCapacity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Showtime> showtimes = new ArrayList<>();

    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setScreen(this);
    }

    public void removeSeat(Seat seat) {
        seats.remove(seat);
        seat.setScreen(null);
    }

    public void addShowtime(Showtime showtime) {
        showtimes.add(showtime);
        showtime.setScreen(this);
    }

    public void removeShowtime(Showtime showtime) {
        showtimes.remove(showtime);
        showtime.setScreen(null);
    }
}
