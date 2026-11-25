package com.example.moviereservation.repository;

import com.example.moviereservation.entity.ReservationSeat;
import com.example.moviereservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

    @Query("SELECT rs.seat.id FROM ReservationSeat rs WHERE rs.showtime.id = :showtimeId AND rs.reservation.status IN (:activeStatuses)")
    Set<Long> findBookedSeatIdsByShowtimeIdAndStatuses(
            @Param("showtimeId") Long showtimeId,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );

    // Rows of [showtimeId, bookedSeatCount] for the showtimes page "fast filling" hint
    // How often each seat of a hall was bought, across all its shows (feeds the bestseller marks)
    @Query("SELECT rs.seat.id, COUNT(rs) FROM ReservationSeat rs WHERE rs.seat.screen.id = :screenId AND rs.reservation.status = :status GROUP BY rs.seat.id")
    java.util.List<Object[]> countBookingsPerSeat(@Param("screenId") Long screenId, @Param("status") ReservationStatus status);

    @Query("SELECT rs.showtime.id, COUNT(rs) FROM ReservationSeat rs WHERE rs.showtime.id IN (:showtimeIds) AND rs.reservation.status IN (:activeStatuses) GROUP BY rs.showtime.id")
    java.util.List<Object[]> countBookedSeatsByShowtime(
            @Param("showtimeIds") Collection<Long> showtimeIds,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );
}
