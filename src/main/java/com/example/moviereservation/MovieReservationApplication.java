package com.example.moviereservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
public class MovieReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieReservationApplication.class, args);
    }
}
