package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    List<Showtime> findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(Long movieId, LocalDateTime time);

    boolean existsByStartTimeBetween(LocalDateTime from, LocalDateTime to);
}
