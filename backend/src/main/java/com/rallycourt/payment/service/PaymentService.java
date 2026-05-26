package com.rallycourt.payment.service;

import com.rallycourt.payment.dto.ProcessPaymentRequest;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.exception.PaymentNotAllowedException;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Payment processPayment(ProcessPaymentRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new PaymentNotAllowedException("Reservation not found: " + request.getReservationId()));

        String currentUsername = getCurrentUsername();
        if (!reservation.getReservedBy().equals(currentUsername)) {
            throw new AccessDeniedException("You can only pay for your own reservation");
        }

        if (reservation.getStatus() != ReservationStatus.RESERVED_PENDING_PAYMENT) {
            throw new PaymentNotAllowedException("Reservation is not eligible for payment");
        }

        autoCancelIfApplicable(reservation);

        Payment payment = new Payment();
        payment.setReservationId(reservation.getId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.SUCCESS);
        Payment savedPayment = paymentRepository.save(payment);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(null);
        reservationRepository.save(reservation);

        return savedPayment;
    }

    private void autoCancelIfApplicable(Reservation reservation) {
        if (reservation.getExpiresAt() == null || reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            reservation.setStatus(ReservationStatus.AUTO_CANCELLED);
            reservation.setExpiresAt(null);
            reservationRepository.save(reservation);
            throw new PaymentNotAllowedException("Reservation has expired");
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication.getName();
    }
}
