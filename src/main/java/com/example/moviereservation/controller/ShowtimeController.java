package com.example.moviereservation.controller;

import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.exception.ResourceNotFoundException;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.service.ReservationService;
import com.example.moviereservation.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeRepository showtimeRepository;
    private final SeatService seatService;

    @GetMapping("/{id}/seats")
    public String showSeatSelection(@PathVariable Long id, Model model) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with ID: " + id));

        List<SeatResponse> seats = seatService.getSeatsForShowtime(id);

        Map<String, List<SeatResponse>> seatsByRow = seats.stream()
                .collect(Collectors.groupingBy(
                        SeatResponse::seatRow,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Seat-count popup: price per tier, and whether any seat in it is still free
        Map<String, List<SeatResponse>> byTier = seats.stream()
                .collect(Collectors.groupingBy(s -> s.seatType().name(), LinkedHashMap::new, Collectors.toList()));

        // Other times of this film at the same cinema that day, for the time strip
        List<Showtime> sameDay = showtimeRepository
                .findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(showtime.getMovie().getId(), LocalDateTime.now()).stream()
                .filter(st -> st.getScreen().getTheater().getId().equals(showtime.getScreen().getTheater().getId())
                        && st.getStartTime().toLocalDate().equals(showtime.getStartTime().toLocalDate()))
                .toList();

        model.addAttribute("showtime", showtime);
        model.addAttribute("seats", seats);
        model.addAttribute("seatsByRow", seatsByRow);
        model.addAttribute("seatTiers", byTier);
        model.addAttribute("bestsellers", seatService.getBestsellerSeatIds(showtime.getScreen().getId(), seats));
        model.addAttribute("sameDayShowtimes", sameDay);
        model.addAttribute("maxSeats", ReservationService.MAX_SEATS_PER_BOOKING);
        model.addAttribute("pageTitle", "Select Seats - " + showtime.getMovie().getTitle());

        return "seat-selection";
    }
}
