package com.rallycourt.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rallycourt.AbstractIntegrationTest;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.auth.service.JwtService;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.court.repository.CourtTypeRepository;
import com.rallycourt.court.repository.VenueTypeRepository;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CourtTypeRepository courtTypeRepository;

    @Autowired
    private VenueTypeRepository venueTypeRepository;

    private String adminToken;
    private User adminUser;
    private static final String ADMIN_EMAIL = "adminrallycourt@rallycourt.local";

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        courtRepository.deleteAll();
        adminUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        adminToken = jwtService.generateToken(adminUser);
    }

    @Test
    void processPaymentConfirmsReservationAndPersistsPayment() throws Exception {
        Reservation reservation = savePendingReservation();

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationId": %d,
                                  "amount": 750.00
                                }
                                """.formatted(reservation.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                .andExpect(jsonPath("$.amount").value(750.00))
                .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.name()));
    }

    @Test
    void processPaymentRejectsExpiredReservation() throws Exception {
        Reservation reservation = savePendingReservation();
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        reservationRepository.save(reservation);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationId": %d,
                                  "amount": 750.00
                                }
                                """.formatted(reservation.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reservation has expired"));
    }

    private Reservation savePendingReservation() {
        Court court = new Court();
        court.setName("Payment Court");
        court.setLocation("Makati City");
        court.setLatitude(14.5547);
        court.setLongitude(121.0244);
        court.setCourtType(courtTypeRepository.findByCode("BASKETBALL").orElseThrow());
        court.setVenueType(venueTypeRepository.findByCode("INDOOR").orElseThrow());
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setOwner(adminUser);
        court = courtRepository.save(court);

        Reservation reservation = new Reservation();
        reservation.setCourtId(court.getId());
        reservation.setReservedBy(adminUser.getEmail());
        reservation.setStartTime(LocalDateTime.now().plusHours(2));
        reservation.setEndTime(LocalDateTime.now().plusHours(3));
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        return reservationRepository.save(reservation);
    }
}
