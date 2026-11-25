package com.example.moviereservation.controller;

import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.exception.ResourceNotFoundException;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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

        model.addAttribute("showtime", showtime);
        model.addAttribute("seats", seats);
        model.addAttribute("seatsByRow", seatsByRow);
        model.addAttribute("pageTitle", "Select Seats - " + showtime.getMovie().getTitle());

        return "seat-selection";
    }
}
