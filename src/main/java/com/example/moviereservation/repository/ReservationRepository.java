package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Admin table: one page at a time, with the row's user/movie/theater fetched in the same query (no N+1)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "showtime.movie", "showtime.screen.theater"})
    org.springframework.data.domain.Page<Reservation> findByStatus(ReservationStatus status, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "showtime.movie", "showtime.screen.theater"})
    org.springframework.data.domain.Page<Reservation> findAllBy(org.springframework.data.domain.Pageable pageable);

    Optional<Reservation> findByBookingReference(String bookingReference);

    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, java.time.LocalDateTime cutoff);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r WHERE r.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@org.springframework.data.repository.query.Param("status") ReservationStatus status);

    long countByStatus(ReservationStatus status);

    long countByShowtime_Movie_Id(Long movieId);

    List<Reservation> findByUserIdAndShowtimeIdAndStatus(Long userId, Long showtimeId, ReservationStatus status);

    long countByUserIdAndStatus(Long userId, ReservationStatus status);

    List<Reservation> findByCreatedAtAfter(java.time.LocalDateTime cutoff);

    @org.springframework.data.jpa.repository.Query("SELECT r.showtime.movie.title, COUNT(r) FROM Reservation r WHERE r.status = com.example.moviereservation.entity.ReservationStatus.CONFIRMED GROUP BY r.showtime.movie.title ORDER BY COUNT(r) DESC")
    List<Object[]> findTopMoviesByReservationCount();
}
