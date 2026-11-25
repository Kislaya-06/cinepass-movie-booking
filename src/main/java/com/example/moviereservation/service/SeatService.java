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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Share of a hall marked as bestseller on the seat map. */
    static final double BESTSELLER_SHARE = 0.12;

    public Set<Long> getBestsellerSeatIds(Long screenId, List<SeatResponse> seats) {
        Map<Long, Long> bookings = reservationSeatRepository.countBookingsPerSeat(screenId, ReservationStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return pickBestsellers(seats, bookings);
    }

    /**
     * The most-bought seats of the hall. Ties (and a hall with no history yet) go to the
     * sweet spot: the middle of the row, about a third of the way forward from the back row.
     */
    static Set<Long> pickBestsellers(List<SeatResponse> seats, Map<Long, Long> bookings) {
        List<String> rows = seats.stream().map(SeatResponse::seatRow).distinct().sorted().toList();
        Map<String, Long> rowSize = seats.stream().collect(Collectors.groupingBy(SeatResponse::seatRow, Collectors.counting()));
        double bestRow = (rows.size() - 1) / 3.0;
        // Rows weigh 3x so the block comes out wide and shallow, like a real centre block
        Comparator<SeatResponse> rank = Comparator
                .comparingLong((SeatResponse s) -> -bookings.getOrDefault(s.id(), 0L))
                .thenComparingDouble(s -> Math.max(3 * Math.abs(rows.indexOf(s.seatRow()) - bestRow),
                        Math.abs(s.seatNumber() - (rowSize.get(s.seatRow()) + 1) / 2.0)));
        return seats.stream()
                .sorted(rank)
                .limit(Math.round(seats.size() * BESTSELLER_SHARE))
                .map(SeatResponse::id)
                .collect(Collectors.toSet());
    }
}
