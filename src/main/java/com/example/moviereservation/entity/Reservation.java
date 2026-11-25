package com.example.moviereservation.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @NotBlank(message = "Booking reference is required")
    @Size(max = 64, message = "Booking reference must not exceed 64 characters")
    @Column(name = "booking_reference", nullable = false, unique = true, length = 64)
    private String bookingReference;

    @NotNull(message = "Reservation status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", message = "Total amount cannot be negative")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "seat_summary", length = 255)
    private String seatSummary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReservationSeat> reservationSeats = new ArrayList<>();

    public void addReservationSeat(ReservationSeat reservationSeat) {
        reservationSeats.add(reservationSeat);
        reservationSeat.setReservation(this);
        if (this.showtime != null) {
            reservationSeat.setShowtime(this.showtime);
        }
    }

    public void removeReservationSeat(ReservationSeat reservationSeat) {
        reservationSeats.remove(reservationSeat);
        reservationSeat.setReservation(null);
        reservationSeat.setShowtime(null);
    }
}
