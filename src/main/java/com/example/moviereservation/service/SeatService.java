package com.example.moviereservation.service;

import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.exception.ResourceNotFoundException;
import com.example.moviereservation.repository.ReservationSeatRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    public List<SeatResponse> getSeatsForShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with ID: " + showtimeId));

        Long screenId = showtime.getScreen().getId();
        List<Seat> seats = seatRepository.findByScreenIdOrderBySeatRowAscSeatNumberAsc(screenId);

        Set<Long> bookedSeatIds = reservationSeatRepository.findBookedSeatIdsByShowtimeIdAndStatuses(
                showtimeId,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );

        BigDecimal basePrice = showtime.getPrice();

        return seats.stream()
                .map(seat -> {
                    BigDecimal calculatedPrice = seat.getSeatType() != null
                            ? seat.getSeatType().calculatePrice(basePrice)
                            : basePrice;
                    boolean isBooked = bookedSeatIds.contains(seat.getId());
                    return new SeatResponse(
                            seat.getId(),
                            seat.getSeatRow(),
                            seat.getSeatNumber(),
                            seat.getSeatType(),
                            calculatedPrice,
                            isBooked ? "BOOKED" : "AVAILABLE"
                    );
                })
                .toList();
    }
}
