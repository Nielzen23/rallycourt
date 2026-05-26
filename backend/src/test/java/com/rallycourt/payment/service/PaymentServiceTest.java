package com.rallycourt.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.payment.dto.ProcessPaymentRequest;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.exception.PaymentNotAllowedException;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentService paymentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void processPaymentConfirmsEligibleReservation() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(10L);
        request.setAmount(new BigDecimal("750.00"));

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.processPayment(request);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(new BigDecimal("750.00"), payment.getAmount());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertNull(reservation.getExpiresAt());
    }

    @Test
    void processPaymentRejectsExpiredReservation() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(10L);
        request.setAmount(new BigDecimal("750.00"));

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentNotAllowedException exception = assertThrows(
                PaymentNotAllowedException.class,
                () -> paymentService.processPayment(request)
        );

        assertEquals("Reservation has expired", exception.getMessage());
        assertEquals(ReservationStatus.AUTO_CANCELLED, reservation.getStatus());
        assertNull(reservation.getExpiresAt());
    }

    @Test
    void processPaymentRejectsMissingReservation() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(999L);
        request.setAmount(new BigDecimal("750.00"));

        PaymentNotAllowedException exception = assertThrows(
                PaymentNotAllowedException.class,
                () -> paymentService.processPayment(request)
        );

        assertEquals("Reservation not found: 999", exception.getMessage());
    }

    @Test
    void processPaymentRejectsDifferentUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(10L);
        request.setAmount(new BigDecimal("750.00"));

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setReservedBy("AnotherUser");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () -> paymentService.processPayment(request));
    }

    @Test
    void processPaymentRejectsReservationWithIneligibleStatus() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(10L);
        request.setAmount(new BigDecimal("750.00"));

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        PaymentNotAllowedException exception = assertThrows(
                PaymentNotAllowedException.class,
                () -> paymentService.processPayment(request)
        );

        assertEquals("Reservation is not eligible for payment", exception.getMessage());
    }

    @Test
    void processPaymentRejectsReservationWithoutExpiration() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("AdminRallyCourt");

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(10L);
        request.setAmount(new BigDecimal("750.00"));

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setExpiresAt(null);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentNotAllowedException exception = assertThrows(
                PaymentNotAllowedException.class,
                () -> paymentService.processPayment(request)
        );

        assertEquals("Reservation has expired", exception.getMessage());
        assertEquals(ReservationStatus.AUTO_CANCELLED, reservation.getStatus());
        assertNull(reservation.getExpiresAt());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPaymentRejectsMissingAuthentication() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(10L);
        request.setAmount(new BigDecimal("750.00"));

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () -> paymentService.processPayment(request));
    }

    @Test
    void processPaymentRejectsAuthenticationWithoutName() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn(null);

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setReservationId(10L);
        request.setAmount(new BigDecimal("750.00"));

        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setReservedBy("AdminRallyCourt");
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () -> paymentService.processPayment(request));
    }
}
