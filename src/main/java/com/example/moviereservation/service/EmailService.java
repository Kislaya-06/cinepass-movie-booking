package com.example.moviereservation.service;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.repository.ReservationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final ReservationRepository reservationRepository;
    private final TicketPdfService ticketPdfService;

    @Value("${app.mail.from:tickets@cinepass.com}")
    private String mailFrom;

    @Value("${app.server.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, hh:mm a");

    @Async
    @Transactional(readOnly = true)
    public void sendBookingConfirmation(Long reservationId) {
        log.info("Starting async booking confirmation email dispatch for reservation ID: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            log.warn("Reservation ID {} not found during email dispatch. Aborting email.", reservationId);
            return;
        }

        String recipientEmail = reservation.getUser().getEmail();
        String customerName = reservation.getUser().getName();
        String bookingRef = reservation.getBookingReference();
        String movieTitle = reservation.getShowtime().getMovie().getTitle();
        String theaterName = reservation.getShowtime().getScreen().getTheater().getName();
        Integer screenNumber = reservation.getShowtime().getScreen().getScreenNumber();
        String showtime = reservation.getShowtime().getStartTime().format(DATE_FORMATTER);
        String seatList = reservation.getSeatSummary() != null ? reservation.getSeatSummary() : "N/A";

        try {
            byte[] pdfBytes = ticketPdfService.generateTicketPdf(reservation);

            Context thymeleafContext = new Context();
            thymeleafContext.setVariable("userName", customerName);
            thymeleafContext.setVariable("bookingReference", bookingRef);
            thymeleafContext.setVariable("movieTitle", movieTitle);
            thymeleafContext.setVariable("theaterName", theaterName);
            thymeleafContext.setVariable("screenNumber", screenNumber);
            thymeleafContext.setVariable("showtime", showtime);
            thymeleafContext.setVariable("seats", seatList);
            thymeleafContext.setVariable("totalAmount", reservation.getTotalAmount());
            thymeleafContext.setVariable("verifyUrl", baseUrl + "/verify/" + bookingRef);

            String htmlBody = templateEngine.process("mail/booking-confirmation", thymeleafContext);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom, "CinePass Box Office");
            helper.setTo(recipientEmail);
            helper.setSubject("🎬 Booking Confirmed! Ticket Pass for " + movieTitle + " [" + bookingRef + "]");
            helper.setText(htmlBody, true);

            String attachmentName = "CinePass-Ticket-" + bookingRef + ".pdf";
            helper.addAttachment(attachmentName, new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(mimeMessage);
            log.info("Booking confirmation email successfully dispatched via SMTP to {}", recipientEmail);

        } catch (MailException mailEx) {
            log.warn("==================================================================");
            log.warn(" [LOCAL DEV NOTICE] SMTP server unreachable: {}", mailEx.getMessage());
            log.warn(" SIMULATED EMAIL CONFIRMATION DISPATCH:");
            log.warn("   To: {} ({})", recipientEmail, customerName);
            log.warn("   Subject: Booking Confirmed! Ticket Pass for {} [{}]", movieTitle, bookingRef);
            log.warn("   Seats: {} | Total: ₹{}", seatList, reservation.getTotalAmount());
            log.warn("   PDF Ticket generated successfully with attached QR code.");
            log.warn("==================================================================");
        } catch (Exception ex) {
            log.error("Unexpected error assembling email for reservation {}: {}", bookingRef, ex.getMessage(), ex);
        }
    }
}
