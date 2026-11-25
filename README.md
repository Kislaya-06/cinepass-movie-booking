# 🎬 CinePass - Movie Reservation System

A modern, full-stack Movie Reservation System engineered with **Spring Boot 3.3.4** and **Thymeleaf**, inspired by top cinema-ticketing platforms like BookMyShow. The application features a dark-themed UI, real-time seat selection with tiered pricing, atomic double-booking protection, downloadable PDF tickets with embedded QR codes, automated seat hold expiration, asynchronous email confirmations, and an administrative console with Chart.js analytics.

---

## 🚀 Tech Stack

- **Backend Core**: Java 17, Spring Boot 3.3.4
- **Web & Templating**: Spring Web MVC, Thymeleaf, Thymeleaf Spring Security 6 Dialect
- **Security**: Spring Security 6 (form login with BCrypt password hashing, role-based authorization, CSRF protection)
- **Persistence**: Spring Data JPA, Hibernate, Jakarta Validation
- **Databases**:
  - **Dev Profile**: H2 file database (`./data`) with `/h2-console` (admin only)
  - **Prod Profile**: PostgreSQL with Flyway migrations
- **PDF Generation**: OpenPDF 2.0.3 (`com.github.librepdf:openpdf`)
- **QR Code Encoding**: Google ZXing 3.5.3 (`zxing:core`, `zxing:javase`)
- **Email Dispatch**: Spring Boot Mail (`spring-boot-starter-mail`) with `@Async` background execution
- **Analytics & Visuals**: Chart.js 4.4, Bootstrap 5.3, FontAwesome 6.5
- **Caching & Tasks**: Spring Cache (`@Cacheable`), Spring Scheduler (`@Scheduled`), Spring Async (`@EnableAsync`)
- **Developer Tools**: Project Lombok, Spring Boot DevTools, Spring Boot Actuator

---

## 🌟 Key Features

1. **Movie Catalog & Showtime Discovery**:
   - Hero banner featuring trending premieres.
   - Horizontal scrollable rows for Now Showing and Coming Soon movies with genre pills and live search.
   - Showtime grouping: `Movie Details -> Theaters -> Audi Screens -> Showtime Pills` with tiered pricing.

2. **Seat Map & Tiered Dynamic Pricing**:
   - Visual cinema hall layout with curved screen arc, natural aisle gaps, and seat row markers.
   - Dynamic pricing tiers:
     - **Recliner (2.0x base price)**
     - **Premium (1.5x base price)**
     - **Regular (1.0x base price)**
   - Client-side live price & seat count calculator with 10-seat booking limits.

3. **Atomic Concurrency Defense & Double-Booking Protection**:
   - **Tier 1**: In-memory pre-validation rejecting seats under `PENDING` or `CONFIRMED` holds.
   - **Tier 2**: Database engine unique constraint `uk_showtime_seat (showtime_id, seat_id)` on `reservation_seats` guaranteeing zero double-bookings under concurrent multi-user race conditions.

4. **10-Minute Seat Hold Expiration Scheduler**:
   - Background daemon runs every 60 seconds (`ReservationExpiryScheduler`) automatically transitioning stale unconfirmed bookings to `EXPIRED`, immediately releasing held seats back to the public.
   - Real-time JavaScript countdown timer on the payment review page providing visual feedback.

5. **Downloadable PDF Ticket Pass (OpenPDF)**:
   - High-resolution boarding pass with CinePass branding, cinema location, screen, showtime, seat coordinates, guest info, and payment breakdown.
   - Streams directly from `GET /reservations/{id}/ticket` with proper `Content-Disposition: attachment`.

6. **QR Code Generation & Verification Portal (ZXing)**:
   - Encodes a digital pass verification URL (`http://localhost:8080/verify/{bookingReference}`).
   - Publicly accessible scanner portal (`GET /verify/{bookingReference}`) confirming pass validity, guest name, auditorium details, and entry clearance.

7. **Asynchronous Email Confirmation (Spring Mail)**:
   - Decoupled from the database write transaction using `@Async`.
   - Sends rich HTML confirmation via Thymeleaf with the PDF ticket attached.
   - Graceful local development fallback: logs complete simulated email and PDF payload to console if no SMTP server is configured locally.

8. **Admin Management Console & Chart.js Analytics**:
   - System KPIs: Total Movies, Multiplexes, Screens, Showtimes, Reservations, and Gross Revenue.
   - Interactive charts:
     - **Daily Bookings** (7-day bar chart)
     - **Daily Revenue** (7-day gradient area chart)
     - **Popular Movies** (doughnut chart by ticket volume)
   - Real-time reservation status badges: `All (N)`, `Confirmed (N)`, `Pending (N)`, `Cancelled (N)`, `Expired (N)`.
   - Modals for adding movies, registering new theaters, and scheduling new showtimes.

---

## 🏃 Getting Started

### Prerequisites
- **JDK 17** or higher
- **Apache Maven 3.9+**

### Running in Development Mode (Default)
By default, the application runs under the `dev` profile with an in-memory H2 database seeded with sample users, movies, theaters, and showtimes:

```bash
# Clone the repository
git clone <repo-url>
cd "Movie Reservation System"

# Run with Maven
mvn spring-boot:run
```

The application will start on: **`http://localhost:8080`**

- **H2 Database Console**: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:movierestdb`
  - Username: `sa`
  - Password: *(leave blank)*
- **Actuator Health**: `http://localhost:8080/actuator/health`

### Running in Production Mode (PostgreSQL)
The schema is created by Flyway from `src/main/resources/db/migration` on first boot; Hibernate only validates it.
Any free Postgres works (Neon, Supabase, Render).

Add the `demo` profile (`prod,demo`) for a public portfolio site: it seeds the sample catalog and demo customers,
and tops up showtimes daily so there is always something to book.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod,demo \
  -DDB_URL="jdbc:postgresql://<host>:5432/<db>?sslmode=require" \
  -DDB_USERNAME=<user> \
  -DDB_PASSWORD=<password> \
  -DAPP_BASE_URL=https://<your-public-domain> \
  -DADMIN_EMAIL=<your-admin-email> \
  -DADMIN_PASSWORD=<strong-password> \
  -DTMDB_API_KEY=<optional-tmdb-key> \
  -DMAIL_HOST=smtp.gmail.com \
  -DMAIL_PORT=587 \
  -DMAIL_USERNAME=your-email@gmail.com \
  -DMAIL_PASSWORD=your-app-password
```

---

## 🔑 Default Seeded Accounts

The admin is created by `AdminBootstrap.java` (local `dev` default below; in prod it comes from `ADMIN_EMAIL` / `ADMIN_PASSWORD`).
Demo customers are seeded by `DataLoader.java` in the `dev` and `demo` profiles:

| Role | Email | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Administrator** (dev only) | `admin@cinepass.com` | `admin123` | Full access, Admin Console (`/admin/**`), Movie & Showtime management |
| **Customer 1** | `rahul.sharma@example.com` | `password123` | Browse catalog, select seats, book passes, view history |
| **Customer 2** | `priya.patel@example.com` | `password123` | Browse catalog, select seats, book passes, view history |

*(New users can also register freely via the public `/register` portal with instant BCrypt password hashing).*

---

## ✉️ Configuring Real SMTP Email Dispatch

By default in development, Spring Mail attempts to connect to `localhost:1025`. If no SMTP server is running locally, `EmailServiceImpl` catches the connection notice and outputs the simulated email and PDF generation details to the log without failing the user journey.

To dispatch live emails via Gmail, Mailtrap, or SendGrid, configure your credentials in `application.yml` or via environment variables:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-gmail-address@gmail.com
    password: your-16-digit-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

---

## 🧪 Testing & Verification

Run the automated end-to-end regression script:

```powershell
powershell -ExecutionPolicy Bypass -File scratch/verify_polish_e2e.ps1
```

Verified test coverage includes:
1. User registration & session-based authentication.
2. Seat selection with hold countdown timer.
3. Payment confirmation & ticket issuance.
4. Binary PDF streaming with OpenPDF and ZXing QR code.
5. Public ticket verification via QR scanner endpoint (`/verify/{bookingReference}`).
6. Double-booking race condition defense.
7. Admin Dashboard Chart.js rendering & status badge filters.
