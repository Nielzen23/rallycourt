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
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.entity.CourtType;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private UserRepository userRepository;

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
                userRepository,
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
    void createReservationRejectsStartTimeInPast() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().minusMinutes(1));
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
    void getCourtAvailabilityRejectsInvalidRange() {
        assertThrows(
                ReservationValidationException.class,
                () -> reservationService.getCourtAvailability(1L, LocalDateTime.now(), LocalDateTime.now())
        );
    }

    @Test
    void getCourtAvailabilityRejectsNullFrom() {
        assertThrows(
                ReservationValidationException.class,
                () -> reservationService.getCourtAvailability(1L, null, LocalDateTime.now().plusHours(1))
        );
    }

    @Test
    void getCourtAvailabilityRejectsNullTo() {
        assertThrows(
                ReservationValidationException.class,
                () -> reservationService.getCourtAvailability(1L, LocalDateTime.now(), null)
        );
    }

    @Test
    void getCourtAvailabilityRejectsMissingCourt() {
        when(courtRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                ReservationValidationException.class,
                () -> reservationService.getCourtAvailability(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1))
        );
    }

    @Test
    void getCourtAvailabilityReturnsPendingWhenConfirmedReservationHasNoSuccessfulPayment() {
        Court court = new Court();
        court.setId(1L);
        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));

        Reservation confirmedReservation = new Reservation();
        confirmedReservation.setId(11L);
        confirmedReservation.setCourtId(1L);
        confirmedReservation.setStartTime(LocalDateTime.now().plusDays(1).plusHours(2));
        confirmedReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        confirmedReservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
                eq(1L), any(), any(), any()
        )).thenReturn(List.of(confirmedReservation));
        when(paymentRepository.findByReservationIdIn(List.of(11L))).thenReturn(List.of());

        List<CourtAvailabilityResponse> responses = reservationService.getCourtAvailability(
                1L,
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(23).withMinute(59)
        );

        assertEquals("PENDING", responses.getFirst().paymentStatus());
    }

    @Test
    void getCourtAvailabilityUsesFirstPaymentWhenDuplicatePaymentsExist() {
        Court court = new Court();
        court.setId(1L);
        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));

        Reservation confirmedReservation = new Reservation();
        confirmedReservation.setId(11L);
        confirmedReservation.setCourtId(1L);
        confirmedReservation.setStartTime(LocalDateTime.now().plusDays(1).plusHours(2));
        confirmedReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        confirmedReservation.setStatus(ReservationStatus.CONFIRMED);

        Payment firstPayment = new Payment();
        firstPayment.setReservationId(11L);
        firstPayment.setStatus(PaymentStatus.FAILED);

        Payment secondPayment = new Payment();
        secondPayment.setReservationId(11L);
        secondPayment.setStatus(PaymentStatus.SUCCESS);

        when(reservationRepository.findByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
                eq(1L), any(), any(), any()
        )).thenReturn(List.of(confirmedReservation));
        when(paymentRepository.findByReservationIdIn(List.of(11L))).thenReturn(List.of(firstPayment, secondPayment));

        List<CourtAvailabilityResponse> responses = reservationService.getCourtAvailability(
                1L,
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(23).withMinute(59)
        );

        assertEquals("PENDING", responses.getFirst().paymentStatus());
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
    void getMyReservationsFallsBackWhenCourtOrUserDataMissing() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("missing@rallycourt.local");

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setCourtId(99L);
        reservation.setReservedBy("missing@rallycourt.local");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        when(reservationRepository.findByReservedBy(eq("missing@rallycourt.local"), any()))
                .thenReturn(new PageImpl<>(List.of(reservation), PageRequest.of(0, 10), 1));
        when(courtRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.findByEmailIn(List.of("missing@rallycourt.local"))).thenReturn(List.of());
        when(paymentRepository.findByReservationIdIn(List.of(10L))).thenReturn(List.of());

        ReservationPageResponse response = reservationService.getMyReservations(0, 10);

        assertEquals("Court #99", response.content().getFirst().courtName());
        assertEquals("missing@rallycourt.local", response.content().getFirst().contactName());
    }

    @Test
    void getAllReservationsReturnsMappedPage() {
        SecurityContextHolder.clearContext();
        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setCourtId(1L);
        reservation.setReservedBy("player@rallycourt.local");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        reservation.setAmountDue(java.math.BigDecimal.TEN);

        Court court = new Court();
        court.setId(1L);
        court.setName("Reservation Court");
        CourtType courtType = new CourtType();
        courtType.setCode("BADMINTON");
        court.setCourtType(courtType);
        court.setLocation("Makati");
        court.setLatitude(1.0);
        court.setLongitude(2.0);

        User user = new User();
        user.setEmail("player@rallycourt.local");
        user.setFirstName("Player");
        user.setLastName("One");
        user.setMobileNumber("0917");

        Payment payment = new Payment();
        payment.setReservationId(10L);
        payment.setStatus(PaymentStatus.SUCCESS);

        when(reservationRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(reservation), PageRequest.of(0, 10), 1));
        when(courtRepository.findAllById(any())).thenReturn(List.of(court));
        when(userRepository.findByEmailIn(List.of("player@rallycourt.local"))).thenReturn(List.of(user));
        when(paymentRepository.findByReservationIdIn(List.of(10L))).thenReturn(List.of(payment));

        ReservationPageResponse response = reservationService.getAllReservations(0, 10);

        assertEquals(1, response.content().size());
        assertEquals("One, Player", response.content().getFirst().contactName());
        assertEquals("SUCCESS", response.content().getFirst().paymentStatus());
    }

    @Test
    void getAllReservationsUsesFirstPaymentWhenDuplicatePaymentsExist() {
        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setCourtId(1L);
        reservation.setReservedBy("player@rallycourt.local");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        Court court = new Court();
        court.setId(1L);
        court.setName("Reservation Court");

        Payment firstPayment = new Payment();
        firstPayment.setReservationId(10L);
        firstPayment.setStatus(PaymentStatus.FAILED);

        Payment secondPayment = new Payment();
        secondPayment.setReservationId(10L);
        secondPayment.setStatus(PaymentStatus.SUCCESS);

        when(reservationRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(reservation), PageRequest.of(0, 10), 1));
        when(courtRepository.findAllById(any())).thenReturn(List.of(court));
        when(userRepository.findByEmailIn(List.of("player@rallycourt.local"))).thenReturn(List.of());
        when(paymentRepository.findByReservationIdIn(List.of(10L))).thenReturn(List.of(firstPayment, secondPayment));

        ReservationPageResponse response = reservationService.getAllReservations(0, 10);

        assertEquals("FAILED", response.content().getFirst().paymentStatus());
    }

    @Test
    void getAllReservationsFormatsContactNameVariants() {
        Reservation firstOnlyReservation = new Reservation();
        firstOnlyReservation.setId(10L);
        firstOnlyReservation.setCourtId(1L);
        firstOnlyReservation.setReservedBy("first@rallycourt.local");
        firstOnlyReservation.setStatus(ReservationStatus.CONFIRMED);
        firstOnlyReservation.setStartTime(LocalDateTime.now().plusDays(1));
        firstOnlyReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        Reservation lastOnlyReservation = new Reservation();
        lastOnlyReservation.setId(11L);
        lastOnlyReservation.setCourtId(1L);
        lastOnlyReservation.setReservedBy("last@rallycourt.local");
        lastOnlyReservation.setStatus(ReservationStatus.CONFIRMED);
        lastOnlyReservation.setStartTime(LocalDateTime.now().plusDays(1).plusHours(2));
        lastOnlyReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));

        Reservation blankNamesReservation = new Reservation();
        blankNamesReservation.setId(12L);
        blankNamesReservation.setCourtId(1L);
        blankNamesReservation.setReservedBy("blank@rallycourt.local");
        blankNamesReservation.setStatus(ReservationStatus.CONFIRMED);
        blankNamesReservation.setStartTime(LocalDateTime.now().plusDays(1).plusHours(4));
        blankNamesReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(5));

        Court court = new Court();
        court.setId(1L);
        court.setName("Reservation Court");

        User firstOnlyUser = new User();
        firstOnlyUser.setEmail("first@rallycourt.local");
        firstOnlyUser.setFirstName("Player");
        firstOnlyUser.setLastName(" ");

        User lastOnlyUser = new User();
        lastOnlyUser.setEmail("last@rallycourt.local");
        lastOnlyUser.setFirstName(" ");
        lastOnlyUser.setLastName("Owner");

        User blankNamesUser = new User();
        blankNamesUser.setEmail("blank@rallycourt.local");
        blankNamesUser.setFirstName(" ");
        blankNamesUser.setLastName(" ");

        when(reservationRepository.findAll(any(PageRequest.class))).thenReturn(
                new PageImpl<>(List.of(firstOnlyReservation, lastOnlyReservation, blankNamesReservation), PageRequest.of(0, 10), 3)
        );
        when(courtRepository.findAllById(any())).thenReturn(List.of(court));
        when(userRepository.findByEmailIn(List.of("first@rallycourt.local", "last@rallycourt.local", "blank@rallycourt.local")))
                .thenReturn(List.of(firstOnlyUser, lastOnlyUser, blankNamesUser));
        when(paymentRepository.findByReservationIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of());

        ReservationPageResponse response = reservationService.getAllReservations(0, 10);

        assertEquals("Player", response.content().get(0).contactName());
        assertEquals("Owner", response.content().get(1).contactName());
        assertEquals("blank@rallycourt.local", response.content().get(2).contactName());
    }

    @Test
    void getAllReservationsFormatsContactNameWhenNamesAreNullAndSkipsBlankReservedByLookup() {
        Reservation blankReservedBy = new Reservation();
        blankReservedBy.setId(10L);
        blankReservedBy.setCourtId(1L);
        blankReservedBy.setReservedBy(" ");
        blankReservedBy.setStatus(ReservationStatus.CONFIRMED);
        blankReservedBy.setStartTime(LocalDateTime.now().plusDays(1));
        blankReservedBy.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        Reservation nullNameReservation = new Reservation();
        nullNameReservation.setId(11L);
        nullNameReservation.setCourtId(1L);
        nullNameReservation.setReservedBy("nullnames@rallycourt.local");
        nullNameReservation.setStatus(ReservationStatus.CONFIRMED);
        nullNameReservation.setStartTime(LocalDateTime.now().plusDays(1).plusHours(2));
        nullNameReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));

        Court court = new Court();
        court.setId(1L);
        court.setName("Reservation Court");

        User nullNamesUser = new User();
        nullNamesUser.setEmail("nullnames@rallycourt.local");
        nullNamesUser.setFirstName(null);
        nullNamesUser.setLastName(null);

        when(reservationRepository.findAll(any(PageRequest.class))).thenReturn(
                new PageImpl<>(List.of(blankReservedBy, nullNameReservation), PageRequest.of(0, 10), 2)
        );
        when(courtRepository.findAllById(any())).thenReturn(List.of(court));
        when(userRepository.findByEmailIn(List.of("nullnames@rallycourt.local"))).thenReturn(List.of(nullNamesUser));
        when(paymentRepository.findByReservationIdIn(List.of(10L, 11L))).thenReturn(List.of());

        ReservationPageResponse response = reservationService.getAllReservations(0, 10);

        assertEquals(" ", response.content().get(0).contactName());
        assertEquals("nullnames@rallycourt.local", response.content().get(1).contactName());
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

    @Test
    void createReservationRejectsMissingHourlyRate() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        request.setDurationMinutes(60);

        Court court = new Court();
        court.setId(1L);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setHourlyRate(null);

        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));
        when(courtRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(court));
        when(authentication.getName()).thenReturn("AdminRallyCourt");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ReservationValidationException exception = assertThrows(
                ReservationValidationException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals("Court hourly rate is not configured", exception.getMessage());
    }

    @Test
    void createReservationSavesPendingReservationWithComputedAmount() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setCourtId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        request.setDurationMinutes(90);

        Court court = new Court();
        court.setId(1L);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setHourlyRate(new java.math.BigDecimal("800.00"));

        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));
        when(courtRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(court));
        when(reservationRepository.existsByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L), any(), any(), any()
        )).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authentication.getName()).thenReturn("AdminRallyCourt");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Reservation saved = reservationService.createReservation(request);

        assertEquals("AdminRallyCourt", saved.getReservedBy());
        assertEquals(ReservationStatus.RESERVED_PENDING_PAYMENT, saved.getStatus());
        assertEquals(new java.math.BigDecimal("1200.00"), saved.getAmountDue());
        assertNotNull(saved.getExpiresAt());
    }

    @Test
    void autoCancelExpiredReservationsDoesNothingWhenNoExpiredReservationsExist() {
        when(reservationRepository.findByStatusAndExpiresAtBefore(
                eq(ReservationStatus.RESERVED_PENDING_PAYMENT),
                any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        reservationService.autoCancelExpiredReservations();

        verify(activityLogService, never()).log(any(), any());
    }
}
