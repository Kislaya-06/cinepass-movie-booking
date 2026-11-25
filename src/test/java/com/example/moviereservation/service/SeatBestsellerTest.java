package com.example.moviereservation.service;

import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.SeatType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SeatBestsellerTest {

    // 12 rows x 20 seats, ids 1..240 row by row
    private static List<SeatResponse> hall() {
        List<SeatResponse> seats = new ArrayList<>();
        long id = 1;
        for (char row = 'A'; row <= 'L'; row++) {
            for (int n = 1; n <= 20; n++) {
                seats.add(new SeatResponse(id++, String.valueOf(row), n, SeatType.REGULAR, BigDecimal.TEN, "AVAILABLE"));
            }
        }
        return seats;
    }

    private static long id(char row, int n) {
        return (row - 'A') * 20L + n;
    }

    @Test
    void withNoHistoryTheCentreBlockIsMarked() {
        Set<Long> picked = SeatService.pickBestsellers(hall(), Map.of());

        assertThat(picked).hasSize(29); // 12% of 240
        assertThat(picked).contains(id('D', 10), id('D', 11));
        assertThat(picked).doesNotContain(id('A', 1), id('L', 20), id('L', 10));
    }

    @Test
    void mostBookedSeatsWinOverTheCentre() {
        long corner = id('L', 1);
        Set<Long> picked = SeatService.pickBestsellers(hall(), Map.of(corner, 5L));

        assertThat(picked).contains(corner).hasSize(29);
    }
}
