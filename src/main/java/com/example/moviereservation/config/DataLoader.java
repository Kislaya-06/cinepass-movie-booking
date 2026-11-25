package com.example.moviereservation.config;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.ReservationSeat;
import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.Role;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.SeatType;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Theater;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.ReservationSeatRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.TheaterRepository;
import com.example.moviereservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"dev", "demo"}) // demo = prod + sample catalog for the public portfolio site
@org.springframework.core.annotation.Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.example.moviereservation.service.ScreenService screenService;

    @Override
    @Transactional
    public void run(String... args) {
        // Admin comes from AdminBootstrap, so check the catalog, not the user count
        if (movieRepository.count() > 0) {
            log.info("Database already seeded with records. Checking if additional cities need seeding...");
            seedAdditionalCitiesIfMissing();
            topUpShowtimes();
            return;
        }

        log.info("Starting Dev Data Seeding...");

        // 1. Seed demo customers (admin is created by AdminBootstrap)
        User rahul = User.builder()
                .name("Rahul Sharma")
                .email("rahul.sharma@example.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("+91 98765 11111")
                .role(Role.USER)
                .build();

        User priya = User.builder()
                .name("Priya Patel")
                .email("priya.patel@example.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("+91 98765 22222")
                .role(Role.USER)
                .build();

        userRepository.saveAll(List.of(rahul, priya));
        log.info("Seeded 2 demo users: rahul, priya");

        // 2. Seed Movies (rich diverse catalog)
        Movie dune = Movie.builder()
                .title("Dune: Part Two")
                .description("Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.")
                .genre("Sci-Fi, Adventure, Action")
                .language("English")
                .durationInMinutes(166)
                .releaseDate(LocalDate.of(2024, 3, 1))
                .posterUrl("https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop")
                .rating(9.2)
                .build();

        Movie oppenheimer = Movie.builder()
                .title("Oppenheimer")
                .description("The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.")
                .genre("Biography, Drama, History")
                .language("English")
                .durationInMinutes(180)
                .releaseDate(LocalDate.of(2023, 7, 21))
                .posterUrl("https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=600&auto=format&fit=crop")
                .rating(9.0)
                .build();

        Movie interstellar = Movie.builder()
                .title("Interstellar")
                .description("A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.")
                .genre("Sci-Fi, Drama, Mystery")
                .language("English")
                .durationInMinutes(169)
                .releaseDate(LocalDate.of(2014, 11, 7))
                .posterUrl("https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600&auto=format&fit=crop")
                .rating(9.4)
                .build();

        Movie darkKnight = Movie.builder()
                .title("The Dark Knight")
                .description("When the menace known as the Joker wreaks havoc and chaos on Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.")
                .genre("Action, Crime, Drama")
                .language("English")
                .durationInMinutes(152)
                .releaseDate(LocalDate.of(2008, 7, 18))
                .posterUrl("https://images.unsplash.com/photo-1509281373149-e957c6296406?q=80&w=600&auto=format&fit=crop")
                .rating(9.0)
                .build();

        Movie spiderVerse = Movie.builder()
                .title("Spider-Man: Across the Spider-Verse")
                .description("Miles Morales catapults across the Multiverse, where he encounters a team of Spider-People charged with protecting its very existence.")
                .genre("Animation, Action, Adventure")
                .language("English")
                .durationInMinutes(140)
                .releaseDate(LocalDate.of(2023, 6, 2))
                .posterUrl("https://images.unsplash.com/photo-1635805737707-575885ab0820?q=80&w=600&auto=format&fit=crop")
                .rating(8.7)
                .build();

        Movie inception = Movie.builder()
                .title("Inception")
                .description("A thief who steals corporate secrets through dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.")
                .genre("Sci-Fi, Action, Thriller")
                .language("English")
                .durationInMinutes(148)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .posterUrl("https://images.unsplash.com/photo-1478720568477-152d9b164e26?q=80&w=600&auto=format&fit=crop")
                .rating(8.8)
                .build();

        movieRepository.saveAll(List.of(dune, oppenheimer, interstellar, darkKnight, spiderVerse, inception));
        log.info("Seeded 6 diverse movies across multiple genres");

        // 3. Seed Theaters
        Theater pvrIcon = Theater.builder()
                .name("PVR ICON: Phoenix Palladium")
                .location("Senapati Bapat Marg, Lower Parel")
                .city("Mumbai")
                .build();

        Theater inoxMegaplex = Theater.builder()
                .name("INOX: Megaplex")
                .location("Inorbit Mall, Link Road, Malad West")
                .city("Mumbai")
                .build();

        theaterRepository.saveAll(List.of(pvrIcon, inoxMegaplex));
        log.info("Seeded 2 theaters");

        // 4. Seed Screens
        Screen pvrScreen1 = Screen.builder()
                .screenNumber(1)
                .seatCapacity(56)
                .theater(pvrIcon)
                .build();

        Screen pvrScreen2 = Screen.builder()
                .screenNumber(2)
                .seatCapacity(40)
                .theater(pvrIcon)
                .build();

        Screen inoxScreen1 = Screen.builder()
                .screenNumber(6)
                .seatCapacity(36)
                .theater(inoxMegaplex)
                .build();

        screenRepository.saveAll(List.of(pvrScreen1, pvrScreen2, inoxScreen1));
        log.info("Seeded 3 screens across theaters");

        // 5. Seed Seats for Screens
        List<Seat> seatsToSave = new ArrayList<>();

        // PVR Screen 1: Rows A & B (10 Recliner), Rows C, D, E (12 Premium)
        for (String row : List.of("A", "B")) {
            for (int num = 1; num <= 10; num++) {
                seatsToSave.add(Seat.builder()
                        .seatRow(row)
                        .seatNumber(num)
                        .seatType(SeatType.RECLINER)
                        .screen(pvrScreen1)
                        .build());
            }
        }
        for (String row : List.of("C", "D", "E")) {
            for (int num = 1; num <= 12; num++) {
                seatsToSave.add(Seat.builder()
                        .seatRow(row)
                        .seatNumber(num)
                        .seatType(SeatType.PREMIUM)
                        .screen(pvrScreen1)
                        .build());
            }
        }

        // INOX Screen 1: Rows A, B (8 Premium), Rows C, D (10 Regular)
        for (String row : List.of("A", "B")) {
            for (int num = 1; num <= 8; num++) {
                seatsToSave.add(Seat.builder()
                        .seatRow(row)
                        .seatNumber(num)
                        .seatType(SeatType.PREMIUM)
                        .screen(inoxScreen1)
                        .build());
            }
        }
        for (String row : List.of("C", "D")) {
            for (int num = 1; num <= 10; num++) {
                seatsToSave.add(Seat.builder()
                        .seatRow(row)
                        .seatNumber(num)
                        .seatType(SeatType.REGULAR)
                        .screen(inoxScreen1)
                        .build());
            }
        }

        seatRepository.saveAll(seatsToSave);
        log.info("Seeded {} seats across screens", seatsToSave.size());

        // 6. Seed Showtimes (set in future relative to now)
        LocalDateTime now = LocalDateTime.now();
        Showtime duneShow1 = Showtime.builder()
                .startTime(now.plusHours(2))
                .endTime(now.plusHours(4).plusMinutes(46))
                .price(new BigDecimal("350.00"))
                .movie(dune)
                .screen(pvrScreen1)
                .build();

        Showtime duneShow2 = Showtime.builder()
                .startTime(now.plusHours(5))
                .endTime(now.plusHours(7).plusMinutes(46))
                .price(new BigDecimal("400.00"))
                .movie(dune)
                .screen(pvrScreen1)
                .build();

        Showtime duneShow3 = Showtime.builder()
                .startTime(now.plusHours(8))
                .endTime(now.plusHours(10).plusMinutes(46))
                .price(new BigDecimal("450.00"))
                .movie(dune)
                .screen(pvrScreen1)
                .build();

        Showtime oppenheimerShow1 = Showtime.builder()
                .startTime(now.plusDays(1).withHour(18).withMinute(0))
                .endTime(now.plusDays(1).withHour(21).withMinute(0))
                .price(new BigDecimal("380.00"))
                .movie(oppenheimer)
                .screen(inoxScreen1)
                .build();

        Showtime darkKnightShow1 = Showtime.builder()
                .startTime(now.plusHours(3))
                .endTime(now.plusHours(5).plusMinutes(32))
                .price(new BigDecimal("300.00"))
                .movie(darkKnight)
                .screen(pvrScreen1)
                .build();

        Showtime spiderVerseShow1 = Showtime.builder()
                .startTime(now.plusHours(6))
                .endTime(now.plusHours(8).plusMinutes(20))
                .price(new BigDecimal("320.00"))
                .movie(spiderVerse)
                .screen(inoxScreen1)
                .build();

        Showtime inceptionShow1 = Showtime.builder()
                .startTime(now.plusDays(1).withHour(21).withMinute(30))
                .endTime(now.plusDays(2).withHour(0).withMinute(0))
                .price(new BigDecimal("360.00"))
                .movie(inception)
                .screen(pvrScreen1)
                .build();

        showtimeRepository.saveAll(List.of(
                duneShow1, duneShow2, duneShow3,
                oppenheimerShow1, darkKnightShow1,
                spiderVerseShow1, inceptionShow1
        ));
        log.info("Seeded 7 showtimes in future relative to now");

        // 7. Seed Sample Confirmed Reservation (matching UI view: BMS-928410 for seats C4, C5)
        Seat seatC4 = seatRepository.findByScreenIdAndSeatRowAndSeatNumber(pvrScreen1.getId(), "C", 4).orElseThrow();
        Seat seatC5 = seatRepository.findByScreenIdAndSeatRowAndSeatNumber(pvrScreen1.getId(), "C", 5).orElseThrow();

        Reservation sampleReservation = Reservation.builder()
                .bookingReference("BMS-928410")
                .status(ReservationStatus.CONFIRMED)
                .seatSummary("C4, C5")
                .totalAmount(new BigDecimal("560.00"))
                .confirmedAt(LocalDateTime.now())
                .user(rahul)
                .showtime(duneShow3)
                .build();

        Reservation savedReservation = reservationRepository.save(sampleReservation);

        ReservationSeat rs1 = ReservationSeat.builder()
                .reservation(savedReservation)
                .seat(seatC4)
                .showtime(duneShow3)
                .priceAtBooking(new BigDecimal("280.00"))
                .build();

        ReservationSeat rs2 = ReservationSeat.builder()
                .reservation(savedReservation)
                .seat(seatC5)
                .showtime(duneShow3)
                .priceAtBooking(new BigDecimal("280.00"))
                .build();

        reservationSeatRepository.saveAll(List.of(rs1, rs2));
        log.info("Seeded sample reservation with booking reference BMS-928410 and 2 seats (C4, C5)");

        seedAdditionalCitiesIfMissing();
        log.info("Dev Data Seeding successfully finished!");
    }

    private void seedAdditionalCitiesIfMissing() {
        if (!theaterRepository.findByCityIgnoreCase("Delhi-NCR").isEmpty()) {
            return;
        }
        log.info("Seeding theaters and showtimes for Delhi-NCR, Bengaluru, Hyderabad...");

        List<Screen> newScreens = List.of(
                createTheaterWithScreen("PVR Director's Cut: Ambience Mall", "Vasant Kunj, New Delhi", "Delhi-NCR", 1),
                createTheaterWithScreen("INOX: Nehru Place", "Eros Cinema Building, Nehru Place", "Delhi-NCR", 2),
                createTheaterWithScreen("PVR: Forum Mall, Koramangala", "Hosur Road, Koramangala", "Bengaluru", 1),
                createTheaterWithScreen("Cinepolis: Nexus Shantiniketan", "Whitefield Main Road", "Bengaluru", 3),
                createTheaterWithScreen("AMB Cinemas: Gachibowli", "Sarath City Capital Mall, Gachibowli", "Hyderabad", 1)
        );

        seedShowtimes(newScreens);
    }

    /**
     * Seeded showtimes are relative to boot time, so a long-running demo would run out of bookable shows.
     * Daily (and on boot) re-adds shows on every screen once nothing is left in the next 24 hours.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void topUpShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        if (showtimeRepository.existsByStartTimeBetween(now, now.plusHours(24))) {
            return;
        }
        log.info("No showtimes in the next 24h, topping up demo schedule...");
        seedShowtimes(screenRepository.findAll());
    }

    private void seedShowtimes(List<Screen> screens) {
        List<Movie> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Showtime> showtimes = new ArrayList<>();
        for (Screen screen : screens) {
            for (Movie movie : movies) {
                showtimes.add(Showtime.builder()
                        .startTime(now.plusHours(2))
                        .endTime(now.plusHours(2).plusMinutes(movie.getDurationInMinutes() > 0 ? movie.getDurationInMinutes() : 120))
                        .price(new BigDecimal("350.00"))
                        .movie(movie)
                        .screen(screen)
                        .build());
                showtimes.add(Showtime.builder()
                        .startTime(now.plusHours(6))
                        .endTime(now.plusHours(6).plusMinutes(movie.getDurationInMinutes() > 0 ? movie.getDurationInMinutes() : 120))
                        .price(new BigDecimal("420.00"))
                        .movie(movie)
                        .screen(screen)
                        .build());
            }
        }
        showtimeRepository.saveAll(showtimes);
        log.info("Seeded {} showtimes across {} screens", showtimes.size(), screens.size());
    }

    private Screen createTheaterWithScreen(String name, String location, String city, int screenNum) {
        Theater theater = theaterRepository.save(Theater.builder()
                .name(name)
                .location(location)
                .city(city)
                .build());
        // 4 rows x 10: A recliner, B premium, C-D regular
        return screenService.createScreen(theater.getId(), screenNum, 4, 10, 1, 1);
    }
}
