package com.example.moviereservation.service;

import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.SeatType;
import com.example.moviereservation.entity.Theater;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.exception.ResourceNotFoundException;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScreenService {

    public static final int MAX_ROWS = 26; // one letter per row, A..Z
    public static final int MAX_SEATS_PER_ROW = 30;

    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;

    /**
     * Creates a screen with a rectangular seat grid: the first {@code reclinerRows} rows are RECLINER,
     * the next {@code premiumRows} are PREMIUM, the rest REGULAR.
     */
    @Transactional
    public Screen createScreen(Long theaterId, int screenNumber, int rows, int seatsPerRow,
                               int reclinerRows, int premiumRows) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found with ID: " + theaterId));

        if (screenNumber < 1) {
            throw new ApplicationException("Screen number must be at least 1");
        }
        if (rows < 1 || rows > MAX_ROWS) {
            throw new ApplicationException("Rows must be between 1 and " + MAX_ROWS);
        }
        if (seatsPerRow < 1 || seatsPerRow > MAX_SEATS_PER_ROW) {
            throw new ApplicationException("Seats per row must be between 1 and " + MAX_SEATS_PER_ROW);
        }
        if (reclinerRows < 0 || premiumRows < 0 || reclinerRows + premiumRows > rows) {
            throw new ApplicationException("Recliner + premium rows cannot exceed the total number of rows");
        }
        if (screenRepository.existsByTheaterIdAndScreenNumber(theaterId, screenNumber)) {
            throw new ApplicationException("Screen " + screenNumber + " already exists in " + theater.getName());
        }

        Screen screen = Screen.builder()
                .screenNumber(screenNumber)
                .seatCapacity(rows * seatsPerRow)
                .theater(theater)
                .build();

        for (int r = 0; r < rows; r++) {
            SeatType type = r < reclinerRows ? SeatType.RECLINER
                    : r < reclinerRows + premiumRows ? SeatType.PREMIUM
                    : SeatType.REGULAR;
            String rowLabel = String.valueOf((char) ('A' + r));
            for (int n = 1; n <= seatsPerRow; n++) {
                screen.addSeat(Seat.builder().seatRow(rowLabel).seatNumber(n).seatType(type).build());
            }
        }
        return screenRepository.save(screen); // seats cascade from Screen
    }
}
