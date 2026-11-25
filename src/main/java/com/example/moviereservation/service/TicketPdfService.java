package com.example.moviereservation.service;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.exception.ApplicationException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPdfService {

    private final QrCodeService qrCodeService;

    @Value("${app.server.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, hh:mm a");
    private static final Color ACCENT_COLOR = new Color(225, 54, 86);
    private static final Color DARK_BG = new Color(17, 17, 24);
    private static final Color CARD_BG = new Color(245, 245, 247);
    private static final Color TEXT_MUTED = new Color(110, 110, 120);

    public byte[] generateTicketPdf(Reservation reservation) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5, 24, 24, 24, 24);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font headerBrandFont = new Font(Font.HELVETICA, 18, Font.BOLD, ACCENT_COLOR);
            Font headerPassFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font movieTitleFont = new Font(Font.HELVETICA, 16, Font.BOLD, DARK_BG);
            Font sectionTitleFont = new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_MUTED);
            Font boldValFont = new Font(Font.HELVETICA, 11, Font.BOLD, DARK_BG);
            Font regularValFont = new Font(Font.HELVETICA, 10, Font.NORMAL, DARK_BG);
            Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, TEXT_MUTED);

            // 1. Header Banner Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{65, 35});

            PdfPCell brandCell = new PdfPCell();
            brandCell.setBackgroundColor(DARK_BG);
            brandCell.setPadding(12);
            brandCell.setBorder(Rectangle.NO_BORDER);

            Paragraph pBrand = new Paragraph("CINEPASS", headerBrandFont);
            Paragraph pSub = new Paragraph("PREMIUM CINEMA PASS", new Font(Font.HELVETICA, 8, Font.NORMAL, Color.LIGHT_GRAY));
            brandCell.addElement(pBrand);
            brandCell.addElement(pSub);
            headerTable.addCell(brandCell);

            PdfPCell refCell = new PdfPCell();
            refCell.setBackgroundColor(ACCENT_COLOR);
            refCell.setPadding(12);
            refCell.setBorder(Rectangle.NO_BORDER);
            refCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pRefTitle = new Paragraph("BOOKING REF", headerPassFont);
            Paragraph pRefVal = new Paragraph(reservation.getBookingReference(), new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE));
            pRefTitle.setAlignment(Element.ALIGN_RIGHT);
            pRefVal.setAlignment(Element.ALIGN_RIGHT);
            refCell.addElement(pRefTitle);
            refCell.addElement(pRefVal);
            headerTable.addCell(refCell);

            document.add(headerTable);

            // 2. Movie Details Section
            PdfPTable movieTable = new PdfPTable(1);
            movieTable.setWidthPercentage(100);
            PdfPCell movieCell = new PdfPCell();
            movieCell.setBackgroundColor(CARD_BG);
            movieCell.setPadding(14);
            movieCell.setBorder(Rectangle.NO_BORDER);

            String movieTitle = reservation.getShowtime().getMovie().getTitle();
            String genreLang = reservation.getShowtime().getMovie().getLanguage() + " • " +
                    reservation.getShowtime().getMovie().getGenre() + " • " +
                    reservation.getShowtime().getMovie().getDurationInMinutes() + " mins";

            Paragraph pMovie = new Paragraph(movieTitle, movieTitleFont);
            Paragraph pGenre = new Paragraph(genreLang, new Font(Font.HELVETICA, 9, Font.BOLD, ACCENT_COLOR));
            movieCell.addElement(pMovie);
            movieCell.addElement(pGenre);
            movieTable.addCell(movieCell);

            document.add(movieTable);

            // 3. Middle Body: Left Info + Right QR Code
            PdfPTable bodyTable = new PdfPTable(2);
            bodyTable.setWidthPercentage(100);
            bodyTable.setWidths(new float[]{65, 35});

            PdfPCell infoCell = new PdfPCell();
            infoCell.setPadding(12);
            infoCell.setBorder(Rectangle.NO_BORDER);

            String theaterName = reservation.getShowtime().getScreen().getTheater().getName();
            String theaterLoc = reservation.getShowtime().getScreen().getTheater().getLocation() + ", " +
                    reservation.getShowtime().getScreen().getTheater().getCity();
            Integer screenNum = reservation.getShowtime().getScreen().getScreenNumber();
            String showtimeStr = reservation.getShowtime().getStartTime().format(DATE_TIME_FORMATTER);

            String seatSummary = reservation.getSeatSummary() != null ? reservation.getSeatSummary() : "N/A";

            infoCell.addElement(new Paragraph("THEATER & AUDITORIUM", sectionTitleFont));
            infoCell.addElement(new Paragraph(theaterName + " - Screen " + screenNum, boldValFont));
            infoCell.addElement(new Paragraph(theaterLoc, regularValFont));

            infoCell.addElement(new Paragraph("\nSHOWTIME", sectionTitleFont));
            infoCell.addElement(new Paragraph(showtimeStr, boldValFont));

            infoCell.addElement(new Paragraph("\nSEATS BOOKED", sectionTitleFont));
            infoCell.addElement(new Paragraph(seatSummary, boldValFont));

            infoCell.addElement(new Paragraph("\nGUEST & PAYMENT", sectionTitleFont));
            infoCell.addElement(new Paragraph(reservation.getUser().getName() + " (" + reservation.getUser().getEmail() + ")", regularValFont));
            infoCell.addElement(new Paragraph("TOTAL AMOUNT: ₹" + reservation.getTotalAmount() + " [" + reservation.getStatus() + "]", new Font(Font.HELVETICA, 11, Font.BOLD, ACCENT_COLOR)));

            bodyTable.addCell(infoCell);

            // Right column: QR Code Image
            PdfPCell qrCell = new PdfPCell();
            qrCell.setPadding(12);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            String verifyUrl = baseUrl + "/verify/" + reservation.getBookingReference();
            byte[] qrBytes = qrCodeService.generateQrCode(verifyUrl, 150, 150);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrImage.scalePercent(80);

            Paragraph pQrTitle = new Paragraph("ENTRY PASS QR", sectionTitleFont);
            pQrTitle.setAlignment(Element.ALIGN_CENTER);
            Paragraph pQrSub = new Paragraph("Scan at Cinema Gate", footerFont);
            pQrSub.setAlignment(Element.ALIGN_CENTER);

            qrCell.addElement(pQrTitle);
            qrCell.addElement(qrImage);
            qrCell.addElement(pQrSub);

            bodyTable.addCell(qrCell);
            document.add(bodyTable);

            // 4. Footer notice
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);
            PdfPCell footerCell = new PdfPCell();
            footerCell.setBackgroundColor(CARD_BG);
            footerCell.setPadding(8);
            footerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph pFooter = new Paragraph(
                    "Important: Please present this digital pass or a printed copy at the auditorium entrance. " +
                            "Admission is subject to cinema regulations. Tickets once confirmed are non-refundable.",
                    footerFont
            );
            pFooter.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(pFooter);
            footerTable.addCell(footerCell);

            document.add(footerTable);

            document.close();
            log.info("Successfully generated PDF ticket for reservation {}", reservation.getBookingReference());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF ticket for reservation {}: {}", reservation.getBookingReference(), e.getMessage(), e);
            throw new ApplicationException("Could not generate ticket PDF: " + e.getMessage());
        }
    }
}
