package com.rallycourt.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rallycourt.AbstractIntegrationTest;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.auth.service.JwtService;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.repository.CourtTypeRepository;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.court.repository.VenueTypeRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.repository.ReservationRepository;
import com.rallycourt.reservation.service.ReservationService;
import com.rallycourt.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ReservationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourtTypeRepository courtTypeRepository;

    @Autowired
    private VenueTypeRepository venueTypeRepository;

    private String adminToken;
    private Court court;
    private User adminUser;
    private static final String ADMIN_EMAIL = "adminrallycourt@rallycourt.local";

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        courtRepository.deleteAll();
        adminUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        adminToken = jwtService.generateToken(adminUser);
        court = new Court();
        court.setName("Reservation Court");
        court.setLocation("Makati City");
        court.setLatitude(14.5547);
        court.setLongitude(121.0244);
        court.setCourtType(courtTypeRepository.findByCode("BASKETBALL").orElseThrow());
        court.setVenueType(venueTypeRepository.findByCode("INDOOR").orElseThrow());
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setOwner(adminUser);
    }

    @Test
    void createReservationStoresPendingPaymentWithExpiration() throws Exception {
        Court savedCourt = saveOwnedCourt();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courtId": %d,
                                  "startTime": "%s",
                                  "durationMinutes": 90
                                }
                                """.formatted(savedCourt.getId(), format(LocalDateTime.now().plusHours(2)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED_PENDING_PAYMENT"))
                .andExpect(jsonPath("$.reservedBy").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void createReservationRejectsOverlap() throws Exception {
        Court savedCourt = saveOwnedCourt();
        LocalDateTime existingStart = LocalDateTime.now()
                .plusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        Reservation existing = new Reservation();
        existing.setCourtId(savedCourt.getId());
        existing.setReservedBy(ADMIN_EMAIL);
        existing.setStartTime(existingStart);
        existing.setEndTime(existingStart.plusHours(1));
        existing.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        existing.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservationRepository.save(existing);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courtId": %d,
                                  "startTime": "%s",
                                  "durationMinutes": 60
                                }
                                """.formatted(savedCourt.getId(), format(existingStart.plusMinutes(30)))))
                .andExpect(status().isConflict());
    }

    @Test
    void createReservationRejectsDurationOutsideConfiguredOptions() throws Exception {
        Court savedCourt = saveOwnedCourt();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courtId": %d,
                                  "startTime": "%s",
                                  "durationMinutes": 75
                                }
                                """.formatted(savedCourt.getId(), format(LocalDateTime.now().plusHours(3)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservationRejectsTimeOutsideCourtOperatingHours() throws Exception {
        Court savedCourt = saveOwnedCourt();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courtId": %d,
                                  "startTime": "%s",
                                  "durationMinutes": 120
                                }
                                """.formatted(savedCourt.getId(), format(LocalDateTime.now().withHour(21).withMinute(0).plusDays(1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyReservationsReturnsAuthenticatedUsersReservations() throws Exception {
        Court savedCourt = saveOwnedCourt();
        Reservation mine = new Reservation();
        mine.setCourtId(savedCourt.getId());
        mine.setReservedBy(ADMIN_EMAIL);
        mine.setStartTime(LocalDateTime.now().plusHours(5));
        mine.setEndTime(LocalDateTime.now().plusHours(6));
        mine.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        mine.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservationRepository.save(mine);

        Reservation other = new Reservation();
        other.setCourtId(savedCourt.getId());
        other.setReservedBy("courtownerone@rallycourt.local");
        other.setStartTime(LocalDateTime.now().plusHours(7));
        other.setEndTime(LocalDateTime.now().plusHours(8));
        other.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        other.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservationRepository.save(other);

        mockMvc.perform(get("/api/reservations")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].reservedBy").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMyReservationsRejectsInvalidPaginationLimits() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .param("page", "-1")
                        .param("size", "51")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReservationConfigReturnsConfiguredDurations() throws Exception {
        mockMvc.perform(get("/api/reservations/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedDurationsMinutes[0]").value(60))
                .andExpect(jsonPath("$.allowedDurationsMinutes[1]").value(90))
                .andExpect(jsonPath("$.allowedDurationsMinutes[2]").value(120));
    }

    @Test
    void cancelReservationUpdatesState() throws Exception {
        Court savedCourt = saveOwnedCourt();
        Reservation reservation = new Reservation();
        reservation.setCourtId(savedCourt.getId());
        reservation.setReservedBy(ADMIN_EMAIL);
        reservation.setStartTime(LocalDateTime.now().plusHours(4));
        reservation.setEndTime(LocalDateTime.now().plusHours(5));
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation = reservationRepository.save(reservation);

        mockMvc.perform(delete("/api/reservations/{id}", reservation.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void autoCancelExpiredReservationsTransitionsState() {
        Court savedCourt = saveOwnedCourt();
        Reservation expired = new Reservation();
        expired.setCourtId(savedCourt.getId());
        expired.setReservedBy(ADMIN_EMAIL);
        expired.setStartTime(LocalDateTime.now().plusHours(1));
        expired.setEndTime(LocalDateTime.now().plusHours(2));
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expired.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        expired = reservationRepository.save(expired);

        reservationService.autoCancelExpiredReservations();

        Reservation updated = reservationRepository.findById(expired.getId()).orElseThrow();
        Assertions.assertEquals(ReservationStatus.AUTO_CANCELLED, updated.getStatus());
        Assertions.assertNull(updated.getExpiresAt());
    }

    private Court saveOwnedCourt() {
        return courtRepository.save(court);
    }

    private String format(LocalDateTime value) {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
