package com.example.moviereservation.exception;

import java.util.Collection;

/**
 * Thrown when an attempt is made to reserve one or more seats that are already
 * occupied by an active (PENDING or CONFIRMED) reservation for the same showtime.
 */
public class SeatAlreadyBookedException extends ApplicationException {

    public SeatAlreadyBookedException(String message) {
        super(message);
    }

    public SeatAlreadyBookedException(Collection<Long> seatIds, Long showtimeId) {
        super(String.format("One or more requested seats %s are already reserved for showtime %d", seatIds, showtimeId));
    }
}
