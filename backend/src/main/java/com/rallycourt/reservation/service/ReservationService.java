package com.rallycourt.reservation.service;

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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<ReservationStatus> BLOCKING_STATUSES = Set.of(
            ReservationStatus.RESERVED_PENDING_PAYMENT,
            ReservationStatus.CONFIRMED
    );

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final ActivityLogService activityLogService;
    private final ReservationProperties reservationProperties;

    @Transactional
    public Reservation createReservation(CreateReservationRequest request) {
        validateDuration(request.getDurationMinutes());
        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ReservationValidationException("Court not found: " + request.getCourtId()));

        LocalDateTime endTime = request.getStartTime().plusMinutes(request.getDurationMinutes());
        validateCourtOperatingHours(court, request.getStartTime(), endTime);
        String currentUsername = getCurrentUsername();
        courtRepository.findByIdForUpdate(request.getCourtId())
                .orElseThrow(() -> new ReservationValidationException("Court not found: " + request.getCourtId()));

        boolean overlaps = reservationRepository.existsByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                request.getCourtId(),
                BLOCKING_STATUSES,
                endTime,
                request.getStartTime()
        );

        if (overlaps) {
            throw new ReservationConflictException("Reservation time slot is not available");
        }

        Reservation reservation = new Reservation();
        reservation.setCourtId(request.getCourtId());
        reservation.setReservedBy(currentUsername);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(endTime);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse getMyReservations(int page, int size) {
        Page<Reservation> reservations = reservationRepository.findByReservedBy(
                getCurrentUsername(),
                PageRequest.of(page, size)
        );
        return new ReservationPageResponse(
                reservations.getContent(),
                reservations.getNumber(),
                reservations.getSize(),
                reservations.getTotalElements(),
                reservations.getTotalPages()
        );
    }

    public ReservationConfigResponse getReservationConfig() {
        return new ReservationConfigResponse(List.copyOf(reservationProperties.getAllowedDurationsMinutes()));
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        String currentUsername = getCurrentUsername();
        if (!reservation.getReservedBy().equals(currentUsername)) {
            throw new AccessDeniedException("You can only cancel your own reservations");
        }

        if (reservation.getStatus() != ReservationStatus.RESERVED_PENDING_PAYMENT
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ReservationValidationException("Reservation cannot be cancelled in its current state");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setExpiresAt(null);
        return reservationRepository.save(reservation);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void autoCancelExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED_PENDING_PAYMENT,
                LocalDateTime.now()
        );

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.AUTO_CANCELLED);
            reservation.setExpiresAt(null);
            activityLogService.log("RESERVATION_AUTO_CANCELLED", "reservation:" + reservation.getId());
        }
    }

    private void validateDuration(Integer durationMinutes) {
        Set<Integer> allowedDurations = reservationProperties.getAllowedDurationsMinutes().stream()
                .collect(Collectors.toSet());
        if (!allowedDurations.contains(durationMinutes)) {
            throw new ReservationValidationException(
                    "Allowed reservation durations are " + reservationProperties.getAllowedDurationsMinutes()
            );
        }
    }

    private void validateCourtOperatingHours(Court court, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.toLocalTime().isBefore(court.getOpenTime()) || endTime.toLocalTime().isAfter(court.getCloseTime())) {
            throw new ReservationValidationException("Reservation must be within court operating hours");
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
