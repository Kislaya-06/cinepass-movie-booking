package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationSeat;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Theater;
import com.example.moviereservation.exception.ApplicationException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** M-ticket PDF: film, show, seats, entry QR, fare breakup and cinema rules on one A5 page. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPdfService {

    private final QrCodeService qrCodeService;

    @Value("${app.server.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter SHOW_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter SHOW_TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter BOOKED_AT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final Color ACCENT = new Color(179, 18, 46);
    private static final Color TEXT = new Color(30, 20, 22);
    private static final Color MUTED = new Color(116, 102, 106);
    private static final Color LINE = new Color(232, 223, 212);
    private static final Color PANEL = new Color(251, 247, 242);

    // Built-in Helvetica has no rupee glyph, so amounts print as "Rs."
    static String money(BigDecimal amount) {
        return "Rs. " + String.format("%,.2f", amount == null ? BigDecimal.ZERO : amount);
    }

    public byte[] generateTicketPdf(Reservation reservation) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A5, 28, 28, 26, 26);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font brand = new Font(Font.HELVETICA, 16, Font.BOLD, ACCENT);
            Font small = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
            Font label = new Font(Font.HELVETICA, 7.5f, Font.BOLD, MUTED);
            Font value = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT);
            Font body = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT);
            Font title = new Font(Font.HELVETICA, 16, Font.BOLD, TEXT);

            Showtime show = reservation.getShowtime();
            Movie movie = show.getMovie();
            Theater theater = show.getScreen().getTheater();

            // Header: brand and ticket type
            PdfPTable header = table(new float[]{60, 40});
            header.addCell(cell(new Phrase("CinePass", brand), Element.ALIGN_LEFT));
            header.addCell(cell(new Phrase("M-Ticket", new Font(Font.HELVETICA, 9, Font.BOLD, MUTED)), Element.ALIGN_RIGHT));
            doc.add(header);
            doc.add(new Chunk(new LineSeparator(0.6f, 100, LINE, Element.ALIGN_CENTER, -4)));

            // Film
            Paragraph film = new Paragraph(movie.getTitle(), title);
            film.setSpacingBefore(10);
            doc.add(film);
            StringBuilder meta = new StringBuilder(movie.getLanguage() == null ? "" : movie.getLanguage());
            if (movie.getDurationInMinutes() != null) {
                meta.append(" | ").append(movie.getDurationInMinutes() / 60).append("h ").append(movie.getDurationInMinutes() % 60).append("m");
            }
            if (movie.getGenre() != null) {
                meta.append(" | ").append(movie.getGenre());
            }
            Paragraph metaLine = new Paragraph(meta.toString(), small);
            metaLine.setSpacingAfter(10);
            doc.add(metaLine);

            // Show details beside the entry QR
            Map<String, List<ReservationSeat>> byTier = reservation.getReservationSeats().stream()
                    .collect(Collectors.groupingBy(rs -> rs.getSeat().getSeatType().name(), LinkedHashMap::new, Collectors.toList()));
            String seats = byTier.entrySet().stream()
                    .map(e -> e.getKey() + " - " + e.getValue().stream()
                            .map(rs -> rs.getSeat().getSeatRow() + rs.getSeat().getSeatNumber())
                            .collect(Collectors.joining(", ")))
                    .collect(Collectors.joining("\n"));
            if (seats.isEmpty()) {
                seats = reservation.getSeatSummary() != null ? reservation.getSeatSummary() : "-";
            }
            int ticketCount = reservation.getReservationSeats().size();

            PdfPTable details = table(new float[]{60, 40});
            PdfPCell left = cell(null, Element.ALIGN_LEFT);
            addField(left, "DATE & TIME", show.getStartTime().format(SHOW_DATE) + " | " + show.getStartTime().format(SHOW_TIME), label, value);
            addField(left, "CINEMA", theater.getName(), label, value);
            left.addElement(new Paragraph(theater.getLocation() + ", " + theater.getCity(), small));
            addField(left, "SCREEN", "Screen " + show.getScreen().getScreenNumber(), label, value);
            addField(left, ticketCount == 1 ? "SEAT (1 TICKET)" : "SEATS (" + ticketCount + " TICKETS)", seats, label, value);
            details.addCell(left);

            PdfPCell right = cell(null, Element.ALIGN_CENTER);
            right.setBackgroundColor(PANEL);
            right.setPadding(10);
            Image qr = Image.getInstance(qrCodeService.generateQrCode(baseUrl + "/verify/" + reservation.getBookingReference(), 240, 240));
            qr.scaleToFit(118, 118);
            qr.setAlignment(Element.ALIGN_CENTER);
            right.addElement(qr);
            right.addElement(centered("BOOKING ID", label));
            right.addElement(centered(reservation.getBookingReference(), new Font(Font.COURIER, 11, Font.BOLD, TEXT)));
            right.addElement(centered("Show this QR at the cinema entry", small));
            details.addCell(right);
            doc.add(details);

            // Tear line
            Paragraph tear = new Paragraph(new Chunk(new DottedLineSeparator()));
            tear.setSpacingBefore(6);
            tear.setSpacingAfter(4);
            doc.add(tear);

            // Fare breakup
            PdfPTable fare = table(new float[]{70, 30});
            byTier.forEach((tier, list) -> {
                BigDecimal sum = list.stream().map(ReservationSeat::getPriceAtBooking).reduce(BigDecimal.ZERO, BigDecimal::add);
                fare.addCell(cell(new Phrase(tier + " x " + list.size(), body), Element.ALIGN_LEFT));
                fare.addCell(cell(new Phrase(money(sum), body), Element.ALIGN_RIGHT));
            });
            PdfPCell totalLabel = cell(new Phrase("Total amount", value), Element.ALIGN_LEFT);
            PdfPCell totalValue = cell(new Phrase(money(reservation.getTotalAmount()), value), Element.ALIGN_RIGHT);
            totalLabel.setBorder(Rectangle.TOP);
            totalValue.setBorder(Rectangle.TOP);
            totalLabel.setBorderColor(LINE);
            totalValue.setBorderColor(LINE);
            totalLabel.setPaddingTop(5);
            totalValue.setPaddingTop(5);
            fare.addCell(totalLabel);
            fare.addCell(totalValue);
            doc.add(fare);

            String status = reservation.getStatus().name().charAt(0) + reservation.getStatus().name().substring(1).toLowerCase();
            String bookedBy = reservation.getUser().getName() + " | " + reservation.getUser().getEmail()
                    + (reservation.getUser().getPhoneNumber() != null ? " | " + reservation.getUser().getPhoneNumber() : "");
            Paragraph who = new Paragraph("Booked by " + bookedBy, small);
            who.setSpacingBefore(6);
            doc.add(who);
            java.time.LocalDateTime bookedAt = reservation.getConfirmedAt() != null ? reservation.getConfirmedAt() : reservation.getCreatedAt();
            doc.add(new Paragraph("Status: " + status + (bookedAt != null ? " | Booked on " + bookedAt.format(BOOKED_AT) : ""), small));

            // Cinema rules
            Paragraph rulesTitle = new Paragraph("Important", new Font(Font.HELVETICA, 8.5f, Font.BOLD, TEXT));
            rulesTitle.setSpacingBefore(10);
            doc.add(rulesTitle);
            for (String rule : List.of(
                    "Show the QR code on your phone or on a printout at the cinema entry.",
                    "Please reach the cinema at least 15 minutes before the show.",
                    "You can cancel from My bookings until the show starts.",
                    "Entry is subject to the cinema's rules and the film's age rating.")) {
                doc.add(new Paragraph("- " + rule, small));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF ticket for reservation {}: {}", reservation.getBookingReference(), e.getMessage(), e);
            throw new ApplicationException("Could not generate ticket PDF: " + e.getMessage());
        }
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths.length);
        t.setWidthPercentage(100);
        try {
            t.setWidths(widths);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return t;
    }

    private static PdfPCell cell(Phrase content, int align) {
        PdfPCell c = content == null ? new PdfPCell() : new PdfPCell(content);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(2);
        return c;
    }

    private static void addField(PdfPCell target, String name, String text, Font labelFont, Font valueFont) {
        Paragraph l = new Paragraph(name, labelFont);
        l.setSpacingBefore(6);
        target.addElement(l);
        target.addElement(new Paragraph(text, valueFont));
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }
}
