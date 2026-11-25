package com.example.moviereservation;

import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.SeatType;
import com.example.moviereservation.entity.Theater;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.TheaterRepository;
import com.example.moviereservation.service.ScreenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
class ScreenServiceTest {

    @Autowired private ScreenService screenService;
    @Autowired private TheaterRepository theaterRepository;
    @Autowired private SeatRepository seatRepository;

    private Long newTheater() {
        return theaterRepository.save(Theater.builder().name("Test Cinema").location("X").city("Pune").build()).getId();
    }

    @Test
    void buildsSeatGridWithTiers() {
        Screen screen = screenService.createScreen(newTheater(), 1, 5, 8, 1, 2);

        var seats = seatRepository.findByScreenIdOrderBySeatRowAscSeatNumberAsc(screen.getId());
        assertThat(seats).hasSize(40);
        assertThat(screen.getSeatCapacity()).isEqualTo(40);
        assertThat(seats.get(0).getSeatRow()).isEqualTo("A");
        assertThat(seats.get(0).getSeatType()).isEqualTo(SeatType.RECLINER);
        assertThat(seats.get(8).getSeatType()).isEqualTo(SeatType.PREMIUM);   // B1
        assertThat(seats.get(16).getSeatType()).isEqualTo(SeatType.PREMIUM);  // C1
        assertThat(seats.get(24).getSeatType()).isEqualTo(SeatType.REGULAR);  // D1
        assertThat(seats.get(39).getSeatRow() + seats.get(39).getSeatNumber()).isEqualTo("E8");
    }

    @Test
    void rejectsDuplicateScreenNumberAndBadTiers() {
        Long theaterId = newTheater();
        screenService.createScreen(theaterId, 1, 2, 5, 0, 0);

        assertThatThrownBy(() -> screenService.createScreen(theaterId, 1, 2, 5, 0, 0))
                .isInstanceOf(ApplicationException.class).hasMessageContaining("already exists");
        assertThatThrownBy(() -> screenService.createScreen(theaterId, 2, 3, 5, 2, 2))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> screenService.createScreen(theaterId, 3, 27, 5, 0, 0))
                .isInstanceOf(ApplicationException.class);
    }
}
