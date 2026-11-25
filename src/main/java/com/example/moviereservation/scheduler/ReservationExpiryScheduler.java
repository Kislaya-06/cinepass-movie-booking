package com.example.moviereservation.scheduler;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to detect and automatically expire stale PENDING reservations.
 * Expiring these reservations automatically releases their held seats for other customers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryScheduler {

    private final ReservationRepository reservationRepository;

    @Value("${app.reservation.expiry-minutes:10}")
    private int expiryMinutes;

    /**
     * Periodically checks for and expires unconfirmed reservations older than the configured threshold.
     */
    @Scheduled(fixedDelayString = "${app.reservation.expiry-check-interval:60000}")
    @Transactional
    public void expireUnconfirmedReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expiryMinutes);

        List<Reservation> staleReservations = reservationRepository.findByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING,
                cutoff
        );

        if (staleReservations.isEmpty()) {
            return;
        }

        log.info("Found {} stale PENDING reservations older than {} minutes. Transitioning to EXPIRED...",
                staleReservations.size(), expiryMinutes);

        for (Reservation reservation : staleReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.getReservationSeats().clear();
            log.debug("Expired reservation ID {} with booking reference {}",
                    reservation.getId(), reservation.getBookingReference());
        }

        reservationRepository.saveAll(staleReservations);
        log.info("Successfully expired {} stale reservations.", staleReservations.size());
    }
}
