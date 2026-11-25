package com.example.moviereservation.dto;

import com.example.moviereservation.entity.SeatType;
import java.math.BigDecimal;

public record SeatResponse(
    Long id,
    String seatRow,
    Integer seatNumber,
    SeatType seatType,
    BigDecimal price,
    String status
) {
    public boolean isBooked() {
        return "BOOKED".equals(status);
    }
}
