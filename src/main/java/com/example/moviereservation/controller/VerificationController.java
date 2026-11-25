package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final ReservationService reservationService;

    @GetMapping("/{bookingReference}")
    public String verifyTicket(@PathVariable String bookingReference, Model model) {
        log.info("Ticket verification scanned for booking reference: {}", bookingReference);
        try {
            Reservation reservation = reservationService.getReservationByBookingReference(bookingReference);
            model.addAttribute("reservation", reservation);
            model.addAttribute("guestName", maskName(reservation.getUser().getName()));
            model.addAttribute("valid", true);
            model.addAttribute("verifiedAt", LocalDateTime.now());
            model.addAttribute("pageTitle", "Pass Verification - " + reservation.getBookingReference());
        } catch (ApplicationException ex) {
            log.warn("Invalid ticket verification attempt: {}", ex.getMessage());
            model.addAttribute("valid", false);
            model.addAttribute("bookingReference", bookingReference);
            model.addAttribute("errorMessage", "Ticket reference not found or invalid.");
            model.addAttribute("pageTitle", "Invalid Ticket - CinePass");
        }
        return "verify-ticket";
    }

    /** Public page: "Rahul Sharma" -> "Rahul S." so a guessed reference doesn't reveal full names. */
    static String maskName(String name) {
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0] : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
