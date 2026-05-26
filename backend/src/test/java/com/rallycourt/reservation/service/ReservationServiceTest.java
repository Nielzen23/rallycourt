package com.rallycourt.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.activity.service.ActivityLogService;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.reservation.config.ReservationProperties;
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
import org.mockito.InjectMocks;
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
    private Authentication authentication;

    @InjectMocks
    private ReservationService reservationService;

    private ReservationProperties reservationProperties;

    @BeforeEach
    void setUp() {
        reservationProperties = new ReservationProperties();
        reservationProperties.setAllowedDurationsMinutes(List.of(60, 90, 120));
        reservationService = new ReservationService(
                reservationRepository,
                courtRepository,
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

        assertThrows(
                ReservationConflictException.class,
                () -> reservationService.createReservation(request)
        );
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
        verify(activityLogService).log("RESERVATION_AUTO_CANCELLED", "reservation:20");
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
        when(reservationRepository.findByReservedBy(eq("AdminRallyCourt"), any()))
                .thenReturn(new PageImpl<>(List.of(reservation)));

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
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(reservationRepository.findById(13L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        Reservation cancelled = reservationService.cancelReservation(13L);

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertNull(cancelled.getExpiresAt());
    }
}
