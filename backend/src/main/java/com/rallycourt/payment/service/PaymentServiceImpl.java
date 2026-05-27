package com.rallycourt.payment.service;

import com.rallycourt.payment.dto.ProcessPaymentRequest;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.exception.PaymentNotAllowedException;
import com.rallycourt.payment.gateway.PaymentGateway;
import com.rallycourt.payment.gateway.PaymentGatewayChargeRequest;
import com.rallycourt.payment.gateway.PaymentGatewayChargeResult;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentGateway paymentGateway;

    @Transactional
    public Payment processPayment(ProcessPaymentRequest request) {
        LOGGER.info("Processing payment for reservationId {}", request.getReservationId());
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
        if (reservation.getAmountDue() != null && request.getAmount().compareTo(reservation.getAmountDue()) != 0) {
            throw new PaymentNotAllowedException("Payment amount does not match the reservation amount due");
        }

        PaymentGatewayChargeResult gatewayResult = paymentGateway.charge(new PaymentGatewayChargeRequest(
                reservation.getId(),
                request.getAmount(),
                request.getPaymentMethodToken()
        ));
        LOGGER.info("Payment gateway responded for reservationId {} with success={}",
                reservation.getId(), gatewayResult.successful());

        Payment payment = new Payment();
        payment.setReservationId(reservation.getId());
        payment.setAmount(request.getAmount());

        if (!gatewayResult.successful()) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            LOGGER.warn("Payment failed for reservationId {}", reservation.getId());
            throw new PaymentNotAllowedException(gatewayResult.message());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        Payment savedPayment = paymentRepository.save(payment);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(null);
        reservationRepository.save(reservation);
        LOGGER.info("Payment succeeded and reservationId {} confirmed", reservation.getId());

        return savedPayment;
    }

    private void autoCancelIfApplicable(Reservation reservation) {
        if (reservation.getExpiresAt() == null || reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            reservation.setStatus(ReservationStatus.AUTO_CANCELLED);
            reservationRepository.save(reservation);
            LOGGER.info("ReservationId {} auto-cancelled before payment because it expired", reservation.getId());
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
