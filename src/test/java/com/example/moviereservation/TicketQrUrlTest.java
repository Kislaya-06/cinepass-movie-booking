package com.example.moviereservation;

import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.service.QrCodeService;
import com.example.moviereservation.service.TicketPdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.server.base-url=https://cinepass.example.com")
@ActiveProfiles({"dev", "test"})
class TicketQrUrlTest {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TicketPdfService ticketPdfService;

    @Test
    @Transactional // lazy showtime/movie/theater loading inside the PDF builder
    void qrCodePointsAtConfiguredPublicUrl() {
        List<String> encoded = new ArrayList<>();
        QrCodeService capturing = new QrCodeService() {
            @Override
            public byte[] generateQrCode(String content, int width, int height) {
                encoded.add(content);
                return super.generateQrCode(content, width, height);
            }
        };
        ReflectionTestUtils.setField(ticketPdfService, "qrCodeService", capturing);
        try {
            var reservation = reservationRepository.findByBookingReference("BMS-928410").orElseThrow();
            ticketPdfService.generateTicketPdf(reservation);
        } finally {
            ReflectionTestUtils.setField(ticketPdfService, "qrCodeService", new QrCodeService());
        }

        assertThat(encoded).containsExactly("https://cinepass.example.com/verify/BMS-928410");
    }
}
