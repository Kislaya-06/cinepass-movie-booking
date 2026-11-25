package com.example.moviereservation.service;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationSeat;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.exception.ResourceNotFoundException;
import com.example.moviereservation.exception.SeatAlreadyBookedException;
import com.example.moviereservation.exception.UnauthorizedActionException;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.ReservationSeatRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @Transactional
    public Reservation createReservation(Long userId, Long showtimeId, List<Long> seatIds) {
        log.info("Processing reservation request for user ID {} on showtime ID {} with seats {}",
                userId, showtimeId, seatIds);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with ID: " + showtimeId));

        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("Cannot book tickets for a showtime that has already started");
        }

        List<Long> uniqueSeatIds = (seatIds != null) ? seatIds.stream().distinct().toList() : List.of();
        if (uniqueSeatIds.isEmpty()) {
            throw new ApplicationException("Please select at least one seat to proceed.");
        }

        List<Seat> seats = seatRepository.findAllById(uniqueSeatIds);
        if (seats.size() != uniqueSeatIds.size()) {
            throw new ResourceNotFoundException("One or more selected seats do not exist in the system");
        }

        Long expectedScreenId = showtime.getScreen().getId();
        boolean allOnSameScreen = seats.stream()
                .allMatch(seat -> seat.getScreen().getId().equals(expectedScreenId));
        if (!allOnSameScreen) {
            throw new ApplicationException("All selected seats must belong to the auditorium assigned to this showtime");
        }

        Set<Long> alreadyBookedSeatIds = reservationSeatRepository.findBookedSeatIdsByShowtimeIdAndStatuses(
                showtime.getId(),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );

        List<Long> conflictingSeatIds = uniqueSeatIds.stream()
                .filter(alreadyBookedSeatIds::contains)
                .toList();
        if (!conflictingSeatIds.isEmpty()) {
            throw new SeatAlreadyBookedException(conflictingSeatIds, showtime.getId());
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        String bookingReference = generateBookingReference();

        String seatSummary = seats.stream()
                .map(s -> s.getSeatRow() + s.getSeatNumber())
                .collect(Collectors.joining(", "));

        Reservation reservation = Reservation.builder()
                .bookingReference(bookingReference)
                .status(ReservationStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .seatSummary(seatSummary)
                .user(user)
                .showtime(showtime)
                .build();

        for (Seat seat : seats) {
            BigDecimal seatPrice = seat.getSeatType() != null
                    ? seat.getSeatType().calculatePrice(showtime.getPrice())
                    : showtime.getPrice();
            totalAmount = totalAmount.add(seatPrice);

            ReservationSeat reservationSeat = ReservationSeat.builder()
                    .seat(seat)
                    .showtime(showtime)
                    .priceAtBooking(seatPrice)
                    .build();

            reservation.addReservationSeat(reservationSeat);
        }

        reservation.setTotalAmount(totalAmount);

        try {
            Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
            log.info("Successfully created PENDING reservation {} with total amount ₹{}",
                    savedReservation.getBookingReference(), savedReservation.getTotalAmount());
            return savedReservation;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Database unique constraint uk_showtime_seat triggered during booking race: {}", ex.getMessage());
            throw new SeatAlreadyBookedException("One or more selected seats were just reserved by another customer. Please select different seats.");
        }
    }

    @Transactional
    public Reservation confirmReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You are not authorized to confirm this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApplicationException(
                    String.format("Only PENDING reservations can be confirmed. Current status: %s", reservation.getStatus())
            );
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (userId != null && !reservation.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You are not authorized to cancel this reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return reservation;
        }

        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new ApplicationException("Cannot cancel an already expired reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        // Release held seats so uk_showtime_seat constraint unblocks future bookings for this showtime
        reservation.getReservationSeats().clear();
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Reservation getReservationByIdAndUser(Long id, Long userId) {
        return reservationRepository.findByIdAndUserId(id, userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Reservation getReservationByBookingReference(String bookingReference) {
        return reservationRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with reference: " + bookingReference));
    }

    @Transactional(readOnly = true)
    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
    }

    private String generateBookingReference() {
        return "BMS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
