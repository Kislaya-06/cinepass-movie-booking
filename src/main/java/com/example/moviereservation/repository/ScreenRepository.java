package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    boolean existsByTheaterIdAndScreenNumber(Long theaterId, Integer screenNumber);
}
