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
}
