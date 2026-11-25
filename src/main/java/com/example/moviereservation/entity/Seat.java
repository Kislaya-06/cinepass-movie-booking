package com.example.moviereservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_screen_seat_position", columnNames = {"screen_id", "seat_row", "seat_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends BaseEntity {

    @NotBlank(message = "Seat row is required")
    @Size(max = 5, message = "Seat row must not exceed 5 characters")
    @Column(name = "seat_row", nullable = false, length = 5)
    private String seatRow;

    @NotNull(message = "Seat number is required")
    @Min(value = 1, message = "Seat number must be at least 1")
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @NotNull(message = "Seat type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 30)
    @Builder.Default
    private SeatType seatType = SeatType.REGULAR;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;
}
