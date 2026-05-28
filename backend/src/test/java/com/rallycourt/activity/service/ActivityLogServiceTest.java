package com.rallycourt.activity.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.activity.dto.AdminActivityHistoryPageResponse;
import com.rallycourt.activity.dto.AdminUserActivitySummaryResponse;
import com.rallycourt.activity.entity.ActivityLog;
import com.rallycourt.activity.repository.ActivityLogRepository;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logPersistsActionStatusAndActor() {
        User user = new User();
        user.setId(42L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("adminrallycourt@rallycourt.local", "ignored", "ROLE_ADMIN")
        );
        when(userRepository.findByEmail("adminrallycourt@rallycourt.local")).thenReturn(java.util.Optional.of(user));

        activityLogService.log("LOGIN_SUCCESS", "SUCCESS");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog savedLog = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("LOGIN_SUCCESS", savedLog.getAction());
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS", savedLog.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("42", savedLog.getActor());
        org.junit.jupiter.api.Assertions.assertNotNull(savedLog.getCreatedAt());
    }

    @Test
    void logSwallowsRepositoryFailure() {
        doThrow(new RuntimeException("Mongo unavailable"))
                .when(activityLogRepository)
                .save(any(ActivityLog.class));

        activityLogService.log("PAYMENT_SUCCESS", "FAIL");
    }

    @Test
    void logUsesSystemActorWhenAuthenticationMissing() {
        activityLogService.log("SYSTEM_ACTION", "SUCCESS");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        Assertions.assertEquals("system", captor.getValue().getActor());
    }

    @Test
    void logUsesSystemActorWhenAuthenticationNotAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("adminrallycourt@rallycourt.local", "ignored")
        );
        SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);

        activityLogService.log("SYSTEM_ACTION", "SUCCESS");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        Assertions.assertEquals("system", captor.getValue().getActor());
    }

    @Test
    void logUsesSystemActorWhenUserCannotBeResolved() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("missing@rallycourt.local", "ignored", "ROLE_ADMIN")
        );
        when(userRepository.findByEmail("missing@rallycourt.local")).thenReturn(Optional.empty());

        activityLogService.log("SYSTEM_ACTION", "SUCCESS");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        Assertions.assertEquals("system", captor.getValue().getActor());
    }

    @Test
    void logUsesSystemActorWhenAuthenticationNameIsNull() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        activityLogService.log("SYSTEM_ACTION", "SUCCESS");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        Assertions.assertEquals("system", captor.getValue().getActor());
    }

    @Test
    void getUserActivitySummariesReturnsLatestActivityAndBookingStats() {
        User user = new User();
        user.setId(42L);
        user.setEmail("player@rallycourt.local");
        user.setFirstName("Player");
        user.setLastName("One");
        var role = new com.rallycourt.auth.entity.Role();
        role.setCode("PLAYER");
        user.setRole(role);

        ActivityLog older = new ActivityLog();
        older.setActor("42");
        older.setAction("LOGIN");
        older.setCreatedAt(Instant.now().minusSeconds(60));

        ActivityLog newer = new ActivityLog();
        newer.setActor("42");
        newer.setAction("RESERVATION_CREATED");
        newer.setCreatedAt(Instant.now());

        Reservation successfulReservation = new Reservation();
        successfulReservation.setId(100L);
        successfulReservation.setReservedBy("player@rallycourt.local");

        Reservation failedReservation = new Reservation();
        failedReservation.setId(101L);
        failedReservation.setReservedBy("player@rallycourt.local");

        Payment successPayment = new Payment();
        successPayment.setReservationId(100L);
        successPayment.setStatus(PaymentStatus.SUCCESS);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(activityLogRepository.findByActorIn(List.of("42"))).thenReturn(List.of(older, newer));
        when(reservationRepository.findAll()).thenReturn(List.of(successfulReservation, failedReservation));
        when(paymentRepository.findByReservationIdIn(List.of(100L, 101L))).thenReturn(List.of(successPayment));

        List<AdminUserActivitySummaryResponse> responses = activityLogService.getUserActivitySummaries();

        Assertions.assertEquals(1, responses.size());
        AdminUserActivitySummaryResponse response = responses.getFirst();
        Assertions.assertEquals("RESERVATION_CREATED", response.lastActivityAction());
        Assertions.assertEquals(1, response.successfulBookings());
        Assertions.assertEquals(1, response.failedBookings());
        Assertions.assertEquals(2, response.totalBookings());
        Assertions.assertEquals(50, response.successRatioPercentage());
    }

    @Test
    void getUserActivitySummariesReturnsDefaultsWhenNoActivityOrBookingsExist() {
        User user = new User();
        user.setId(7L);
        user.setEmail("idle@rallycourt.local");
        user.setFirstName("Idle");
        user.setLastName("User");
        var role = new com.rallycourt.auth.entity.Role();
        role.setCode("PLAYER");
        user.setRole(role);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(activityLogRepository.findByActorIn(List.of("7"))).thenReturn(List.of());
        when(reservationRepository.findAll()).thenReturn(List.of());
        when(paymentRepository.findByReservationIdIn(List.of())).thenReturn(List.of());

        List<AdminUserActivitySummaryResponse> responses = activityLogService.getUserActivitySummaries();

        AdminUserActivitySummaryResponse response = responses.getFirst();
        Assertions.assertEquals(0, response.successfulBookings());
        Assertions.assertEquals(0, response.failedBookings());
        Assertions.assertEquals(0, response.totalBookings());
        Assertions.assertEquals(0, response.successRatioPercentage());
        Assertions.assertNull(response.lastActivityAction());
    }

    @Test
    void getUserActivitySummariesUsesLatestActivityWhenReplacementIsOlderAndKeepsLastDuplicatePayment() {
        User user = new User();
        user.setId(42L);
        user.setEmail("player@rallycourt.local");
        user.setFirstName("Player");
        user.setLastName("One");
        var role = new com.rallycourt.auth.entity.Role();
        role.setCode("PLAYER");
        user.setRole(role);

        ActivityLog newer = new ActivityLog();
        newer.setActor("42");
        newer.setAction("LATEST");
        newer.setCreatedAt(Instant.now());

        ActivityLog older = new ActivityLog();
        older.setActor("42");
        older.setAction("OLDER");
        older.setCreatedAt(Instant.now().minusSeconds(60));

        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setReservedBy("player@rallycourt.local");

        Payment firstPayment = new Payment();
        firstPayment.setReservationId(100L);
        firstPayment.setStatus(PaymentStatus.SUCCESS);

        Payment replacementPayment = new Payment();
        replacementPayment.setReservationId(100L);
        replacementPayment.setStatus(PaymentStatus.FAILED);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(activityLogRepository.findByActorIn(List.of("42"))).thenReturn(List.of(newer, older));
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));
        when(paymentRepository.findByReservationIdIn(List.of(100L))).thenReturn(List.of(firstPayment, replacementPayment));

        List<AdminUserActivitySummaryResponse> responses = activityLogService.getUserActivitySummaries();

        AdminUserActivitySummaryResponse response = responses.getFirst();
        Assertions.assertEquals("LATEST", response.lastActivityAction());
        Assertions.assertEquals(0, response.successfulBookings());
        Assertions.assertEquals(1, response.failedBookings());
    }

    @Test
    void getUserActivityHistoryReturnsMappedPage() {
        User user = new User();
        user.setId(42L);

        ActivityLog log = new ActivityLog();
        log.setId("abc");
        log.setAction("LOGIN");
        log.setStatus("SUCCESS");
        log.setCreatedAt(Instant.now());

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(activityLogRepository.findByActorOrderByCreatedAtDesc(ArgumentMatchers.eq("42"), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        AdminActivityHistoryPageResponse response = activityLogService.getUserActivityHistory(42L, 0, 10);

        Assertions.assertEquals(1, response.content().size());
        Assertions.assertEquals("LOGIN", response.content().getFirst().action());
        Assertions.assertEquals("SUCCESS", response.content().getFirst().status());
    }

    @Test
    void getUserActivityHistoryThrowsWhenUserMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                com.rallycourt.activity.exception.ActivityUserNotFoundException.class,
                () -> activityLogService.getUserActivityHistory(42L, 0, 10)
        );
    }
}
