package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScreenIdOrderBySeatRowAscSeatNumberAsc(Long screenId);

    Optional<Seat> findByScreenIdAndSeatRowAndSeatNumber(Long screenId, String seatRow, Integer seatNumber);
}
