package com.rallycourt.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.activity.service.ActivityLogService;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.config.ReservationProperties;
import com.rallycourt.reservation.dto.CourtAvailabilityResponse;
import com.rallycourt.reservation.dto.CreateReservationRequest;
import com.rallycourt.reservation.dto.ReservationConfigResponse;
import com.rallycourt.reservation.dto.ReservationPageResponse;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.exception.ReservationConflictException;
import com.rallycourt.reservation.exception.ReservationNotFoundException;
import com.rallycourt.reservation.exception.ReservationValidationException;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private Authentication authentication;

    private ReservationServiceImpl reservationService;

    private ReservationProperties reservationProperties;

    @BeforeEach
    void setUp() {
        reservationProperties = new ReservationProperties();
        reservationProperties.setAllowedDurationsMinutes(List.of(60, 90, 120));
        reservationService = new ReservationServiceImpl(
                reservationRepository,
                courtRepository,
                paymentRepository,
                activityLogService,
                reservationProperties
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getReservationConfigReturnsConfiguredDurations() {
        ReservationConfigResponse response = reservationService.getReservationConfig();

        assertEquals(List.of(60, 90, 120), response.allowedDurationsMinutes());
    }

    @Test
    void createReservationRejectsDurationOutsideConfiguredOptions() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setDurationMinutes(75);

        assertThrows(ReservationValidationException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservationRejectsOperatingHoursViolation() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1).withHour(21).withMinute(0));
        request.setDurationMinutes(120);

        Court court = new Court();
        court.setId(1L);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));

        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));

        assertThrows(ReservationValidationException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservationRejectsStartTimeBeforeCourtOpens() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1).withHour(7).withMinute(0));
        request.setDurationMinutes(60);

        Court court = new Court();
        court.setId(1L);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));

        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));

        assertThrows(ReservationValidationException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservationRejectsOverlap() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setDurationMinutes(60);

        Court court = new Court();
        court.setId(1L);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));

        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));
        when(courtRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(court));
        when(reservationRepository.existsByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L),
                any(),
                any(),
                any()
        )).thenReturn(true);
        when(authentication.getName()).thenReturn("AdminRallyCourt");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ReservationConflictException exception = assertThrows(
                ReservationConflictException.class,
                () -> reservationService.createReservation(request)
        );
        assertEquals("Selected time slot is no longer available.", exception.getMessage());
    }

    @Test
    void createReservationRejectsStartTimeBeyondTwoWeeks() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(15));
        request.setDurationMinutes(60);

        ReservationValidationException exception = assertThrows(
                ReservationValidationException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals("Reservations can only be created within the next 2 weeks.", exception.getMessage());
    }

    @Test
    void getCourtAvailabilityReturnsPendingAndPaidLabels() {
        Court court = new Court();
        court.setId(1L);
        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));

        Reservation pendingReservation = new Reservation();
        pendingReservation.setId(10L);
        pendingReservation.setCourtId(1L);
        pendingReservation.setStartTime(LocalDateTime.now().plusDays(1));
        pendingReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        pendingReservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);

        Reservation confirmedReservation = new Reservation();
        confirmedReservation.setId(11L);
        confirmedReservation.setCourtId(1L);
        confirmedReservation.setStartTime(LocalDateTime.now().plusDays(1).plusHours(2));
        confirmedReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        confirmedReservation.setStatus(ReservationStatus.CONFIRMED);

        com.rallycourt.payment.entity.Payment payment = new com.rallycourt.payment.entity.Payment();
        payment.setReservationId(11L);
        payment.setStatus(com.rallycourt.payment.entity.PaymentStatus.SUCCESS);

        when(reservationRepository.findByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
                eq(1L), any(), any(), any()
        )).thenReturn(List.of(pendingReservation, confirmedReservation));
        when(paymentRepository.findByReservationIdIn(List.of(10L, 11L))).thenReturn(List.of(payment));

        List<CourtAvailabilityResponse> responses = reservationService.getCourtAvailability(
                1L,
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(23).withMinute(59)
        );

        assertEquals(2, responses.size());
        assertEquals("Booking Pending", responses.get(0).label());
        assertEquals("PENDING", responses.get(0).paymentStatus());
        assertEquals("Paid", responses.get(1).label());
        assertEquals("PAID", responses.get(1).paymentStatus());
    }

    @Test
    void autoCancelExpiredReservationsUpdatesStatusAndLogs() {
        Reservation expired = new Reservation();
        expired.setId(20L);
        expired.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(reservationRepository.findByStatusAndExpiresAtBefore(
                eq(ReservationStatus.RESERVED_PENDING_PAYMENT),
                any(LocalDateTime.class)
        )).thenReturn(List.of(expired));

        reservationService.autoCancelExpiredReservations();

        assertEquals(ReservationStatus.AUTO_CANCELLED, expired.getStatus());
        assertNotNull(expired.getExpiresAt());
        verify(activityLogService).log("RESERVATION_AUTO_CANCELLED", "SUCCESS");
    }

    @Test
    void createReservationRejectsMissingCourtBeforeLocking() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(99L);
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setDurationMinutes(60);

        when(courtRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservationValidationException.class, () -> reservationService.createReservation(request));
        verify(courtRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void createReservationRejectsMissingCourtDuringLocking() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setDurationMinutes(60);

        Court court = new Court();
        court.setId(1L);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));

        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));
        when(courtRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        when(authentication.getName()).thenReturn("AdminRallyCourt");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(ReservationValidationException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void getMyReservationsRejectsMissingAuthentication() {
        assertThrows(AccessDeniedException.class, () -> reservationService.getMyReservations(0, 10));
    }

    @Test
    void getMyReservationsRejectsAuthenticationWithoutName() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> reservationService.getMyReservations(0, 10));
    }

    @Test
    void getMyReservationsReturnsCurrentUsersReservations() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setCourtId(1L);
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        when(reservationRepository.findByReservedBy(eq("AdminRallyCourt"), any()))
                .thenReturn(new PageImpl<>(List.of(reservation)));

        Court court = new Court();
        court.setId(1L);
        court.setName("Reservation Court");
        when(courtRepository.findAllById(any())).thenReturn(List.of(court));

        ReservationPageResponse response = reservationService.getMyReservations(0, 10);

        assertEquals(1, response.content().size());
        assertEquals(1L, response.totalElements());
    }

    @Test
    void cancelReservationRejectsMissingReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.cancelReservation(100L));
    }

    @Test
    void cancelReservationRejectsDifferentUser() {
        Reservation reservation = new Reservation();
        reservation.setId(11L);
        reservation.setReservedBy("AnotherUser");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);

        when(reservationRepository.findById(11L)).thenReturn(Optional.of(reservation));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        assertThrows(AccessDeniedException.class, () -> reservationService.cancelReservation(11L));
    }

    @Test
    void cancelReservationRejectsUnsupportedStatus() {
        Reservation reservation = new Reservation();
        reservation.setId(12L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findById(12L)).thenReturn(Optional.of(reservation));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        assertThrows(ReservationValidationException.class, () -> reservationService.cancelReservation(12L));
    }

    @Test
    void cancelReservationCancelsConfirmedReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(13L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(reservationRepository.findById(13L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        Reservation cancelled = reservationService.cancelReservation(13L);

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getExpiresAt());
    }
}
