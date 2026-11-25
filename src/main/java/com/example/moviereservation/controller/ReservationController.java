package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Role;
import com.example.moviereservation.exception.ApplicationException;
import com.example.moviereservation.exception.UnauthorizedActionException;
import com.example.moviereservation.security.CustomUserDetails;
import com.example.moviereservation.service.EmailService;
import com.example.moviereservation.service.ReservationService;
import com.example.moviereservation.service.TicketPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;
    private final TicketPdfService ticketPdfService;
    private final EmailService emailService;
    private final com.example.moviereservation.service.QrCodeService qrCodeService;

    @org.springframework.beans.factory.annotation.Value("${app.server.base-url}")
    private String baseUrl;

    @PostMapping("/reservations")
    public String createReservation(
            @RequestParam("showtimeId") Long showtimeId,
            @RequestParam("seatIds") List<Long> seatIds,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one seat to proceed.");
            return "redirect:/showtimes/" + showtimeId + "/seats";
        }

        try {
            Reservation reservation = reservationService.createReservation(userDetails.getId(), showtimeId, seatIds);
            return "redirect:/reservations/" + reservation.getId() + "/payment";
        } catch (ApplicationException ex) {
            log.warn("Failed to create reservation: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/showtimes/" + showtimeId + "/seats";
        }
    }

    @GetMapping("/reservations/{id}/payment")
    public String paymentConfirmPage(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        Reservation reservation = reservationService.getReservationByIdAndUser(id, userDetails.getId());
        // Only an unpaid hold can be paid; cancelled/expired/confirmed ones belong in My Bookings
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            return "redirect:/my-bookings";
        }

        model.addAttribute("reservation", reservation);
        model.addAttribute("pageTitle", "Review & Pay - " + reservation.getBookingReference());
        return "payment-confirm";
    }

    @PostMapping("/reservations/{id}/confirm")
    public String confirmPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Reservation confirmed = reservationService.confirmReservation(id, userDetails.getId());
            emailService.sendBookingConfirmation(confirmed.getId());
            return "redirect:/reservations/" + confirmed.getId() + "/success";
        } catch (ApplicationException ex) {
            log.error("Payment confirmation failed for reservation ID {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/my-bookings";
        }
    }

    @GetMapping("/reservations/{id}/ticket")
    public ResponseEntity<byte[]> downloadTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Reservation reservation = (userDetails.getRole() == Role.ADMIN)
                ? reservationService.getReservationById(id)
                : reservationService.getReservationByIdAndUser(id, userDetails.getId());

        if (reservation == null) {
            throw new UnauthorizedActionException("You are not authorized to download this ticket pass.");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApplicationException("A ticket is only issued for a confirmed booking.");
        }

        byte[] pdfBytes = ticketPdfService.generateTicketPdf(reservation);
        String filename = "CinePass-Ticket-" + reservation.getBookingReference() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    /** Entry QR shown on the booking-confirmed page; same owner check as the PDF. */
    @GetMapping(value = "/reservations/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> ticketQr(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Reservation reservation = reservationService.getReservationByIdAndUser(id, userDetails.getId());
        if (reservation == null || reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new UnauthorizedActionException("No ticket QR for this booking.");
        }
        byte[] png = qrCodeService.generateQrCode(baseUrl + "/verify/" + reservation.getBookingReference(), 280, 280);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/reservations/{id}/success")
    public String bookingSuccessPage(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        Reservation reservation = reservationService.getReservationByIdAndUser(id, userDetails.getId());
        if (reservation == null) {
            return "redirect:/my-bookings";
        }

        model.addAttribute("reservation", reservation);
        model.addAttribute("pageTitle", "Booking Confirmed - " + reservation.getBookingReference());
        return "booking-success";
    }

    @GetMapping("/my-bookings")
    public String myBookings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        List<Reservation> reservations = reservationService.getReservationsForUser(userDetails.getId());
        model.addAttribute("reservations", reservations);
        model.addAttribute("pageTitle", "My Bookings & Passes - CinePass");
        return "my-bookings";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservationService.cancelReservation(id, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Reservation cancelled successfully. Held seats have been released.");
        } catch (ApplicationException ex) {
            log.warn("Failed to cancel reservation ID {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/my-bookings";
    }
}
