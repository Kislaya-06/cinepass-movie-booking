package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {

    List<Theater> findByCityIgnoreCase(String city);

    boolean existsByCityIgnoreCase(String city);
}
