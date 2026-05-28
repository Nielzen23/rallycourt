package com.rallycourt.reservation.service;

import com.rallycourt.activity.service.ActivityLogService;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.dto.CourtAvailabilityResponse;
import com.rallycourt.reservation.config.ReservationProperties;
import com.rallycourt.reservation.dto.CreateReservationRequest;
import com.rallycourt.reservation.dto.ReservationConfigResponse;
import com.rallycourt.reservation.dto.ReservationPageResponse;
import com.rallycourt.reservation.dto.ReservationSummaryResponse;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.exception.ReservationConflictException;
import com.rallycourt.reservation.exception.ReservationNotFoundException;
import com.rallycourt.reservation.exception.ReservationValidationException;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ReservationServiceImpl implements ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceImpl.class);
    private static final Set<ReservationStatus> BLOCKING_STATUSES = Set.of(
            ReservationStatus.RESERVED_PENDING_PAYMENT,
            ReservationStatus.CONFIRMED
    );

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final ReservationProperties reservationProperties;

    @Transactional
    public Reservation createReservation(CreateReservationRequest request) {
        LOGGER.info("Creating reservation for courtId {} at {} for {} minutes",
                request.getCourtId(), request.getStartTime(), request.getDurationMinutes());
        validateDuration(request.getDurationMinutes());
        validateReservationWindow(request.getStartTime());
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
            LOGGER.warn("Reservation overlap detected for courtId {} at {}", request.getCourtId(), request.getStartTime());
            throw new ReservationConflictException("Selected time slot is no longer available.");
        }

        Reservation reservation = new Reservation();
        reservation.setCourtId(request.getCourtId());
        reservation.setReservedBy(currentUsername);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(endTime);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        reservation.setStatus(ReservationStatus.RESERVED_PENDING_PAYMENT);
        reservation.setAmountDue(calculateAmountDue(court, request.getDurationMinutes()));
        LOGGER.info("Reservation prepared for user {} with pending-payment status", currentUsername);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<CourtAvailabilityResponse> getCourtAvailability(Long courtId, LocalDateTime from, LocalDateTime to) {
        LOGGER.info("Fetching reservation availability for courtId {} from {} to {}", courtId, from, to);
        if (from == null || to == null || !from.isBefore(to)) {
            throw new ReservationValidationException("Availability range is invalid.");
        }

        courtRepository.findById(courtId)
                .orElseThrow(() -> new ReservationValidationException("Court not found: " + courtId));

        List<Reservation> reservations = reservationRepository
                .findByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
                        courtId,
                        BLOCKING_STATUSES,
                        to,
                        from
                );

        Map<Long, Payment> paymentsByReservationId = paymentRepository.findByReservationIdIn(
                        reservations.stream().map(Reservation::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        Payment::getReservationId,
                        payment -> payment,
                        (current, ignored) -> current,
                        HashMap::new
                ));

        return reservations.stream()
                .map(reservation -> mapAvailability(reservation, paymentsByReservationId.get(reservation.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse getMyReservations(int page, int size) {
        LOGGER.info("Fetching reservations for current user page={} size={}", page, size);
        Page<Reservation> reservations = reservationRepository.findByReservedBy(
                getCurrentUsername(),
                PageRequest.of(page, size)
        );
        return mapReservationPage(reservations);
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse getAllReservations(int page, int size) {
        LOGGER.info("Fetching all reservations page={} size={}", page, size);
        Page<Reservation> reservations = reservationRepository.findAll(PageRequest.of(page, size));
        return mapReservationPage(reservations);
    }

    private ReservationPageResponse mapReservationPage(Page<Reservation> reservations) {
        Map<Long, Payment> paymentsByReservationId = paymentRepository.findByReservationIdIn(
                        reservations.getContent().stream().map(Reservation::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        Payment::getReservationId,
                        Function.identity(),
                        (current, ignored) -> current,
                        HashMap::new
                ));
        Map<Long, Court> courtsById = courtRepository.findAllById(
                        reservations.getContent().stream().map(Reservation::getCourtId).toList()
                ).stream()
                .collect(Collectors.toMap(Court::getId, Function.identity()));
        Map<String, User> usersByEmail = userRepository.findByEmailIn(
                        reservations.getContent().stream()
                                .map(Reservation::getReservedBy)
                                .filter(email -> email != null && !email.isBlank())
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(User::getEmail, Function.identity()));
        return new ReservationPageResponse(
                reservations.getContent().stream()
                        .map(reservation -> mapReservationSummary(
                                reservation,
                                courtsById.get(reservation.getCourtId()),
                                usersByEmail.get(reservation.getReservedBy()),
                                paymentsByReservationId.get(reservation.getId())
                        ))
                        .toList(),
                reservations.getNumber(),
                reservations.getSize(),
                reservations.getTotalElements(),
                reservations.getTotalPages()
        );
    }

    public ReservationConfigResponse getReservationConfig() {
        LOGGER.debug("Fetching reservation config");
        return new ReservationConfigResponse(List.copyOf(reservationProperties.getAllowedDurationsMinutes()));
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        LOGGER.info("Cancelling reservationId {}", reservationId);
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
        LOGGER.info("ReservationId {} cancelled by user {}", reservationId, currentUsername);
        return reservationRepository.save(reservation);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void autoCancelExpiredReservations() {
        LOGGER.debug("Running expired reservation auto-cancel job");
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED_PENDING_PAYMENT,
                LocalDateTime.now()
        );

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.AUTO_CANCELLED);
            LOGGER.info("ReservationId {} auto-cancelled after payment window expiry", reservation.getId());
            activityLogService.log("RESERVATION_AUTO_CANCELLED", "SUCCESS");
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

    private void validateReservationWindow(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate earliestReservationDate = now.toLocalDate().plusDays(1);
        if (startTime.isBefore(now)
                || startTime.toLocalDate().isBefore(earliestReservationDate)
                || startTime.isAfter(now.plusDays(14))) {
            throw new ReservationValidationException("Reservations must start from tomorrow and be within the next 2 weeks.");
        }
    }

    private void validateCourtOperatingHours(Court court, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.toLocalTime().isBefore(court.getOpenTime()) || endTime.toLocalTime().isAfter(court.getCloseTime())) {
            throw new ReservationValidationException("Reservation must be within court operating hours");
        }
    }

    private BigDecimal calculateAmountDue(Court court, Integer durationMinutes) {
        if (court.getHourlyRate() == null) {
            throw new ReservationValidationException("Court hourly rate is not configured");
        }

        return court.getHourlyRate()
                .multiply(BigDecimal.valueOf(durationMinutes.longValue()))
                .divide(BigDecimal.valueOf(60L), 2, RoundingMode.HALF_UP);
    }

    private CourtAvailabilityResponse mapAvailability(Reservation reservation, Payment payment) {
        if (reservation.getStatus() == ReservationStatus.CONFIRMED && payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            return new CourtAvailabilityResponse(
                    reservation.getId(),
                    reservation.getStartTime(),
                    reservation.getEndTime(),
                    reservation.getStatus().name(),
                    "PAID",
                    "Paid"
            );
        }

        return new CourtAvailabilityResponse(
                reservation.getId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus().name(),
                "PENDING",
                "Booking Pending"
        );
    }

    private ReservationSummaryResponse mapReservationSummary(Reservation reservation, Court court, User user, Payment payment) {
        return new ReservationSummaryResponse(
                reservation.getId(),
                reservation.getCourtId(),
                court != null ? court.getName() : "Court #" + reservation.getCourtId(),
                court != null ? court.getCourtTypeCode() : null,
                court != null ? court.getLocation() : null,
                court != null ? court.getLatitude() : null,
                court != null ? court.getLongitude() : null,
                reservation.getReservedBy(),
                formatContactName(user, reservation.getReservedBy()),
                user != null ? user.getMobileNumber() : null,
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getExpiresAt(),
                reservation.getStatus().name(),
                payment != null ? payment.getStatus().name() : null,
                reservation.getAmountDue()
        );
    }

    private String formatContactName(User user, String fallback) {
        if (user == null) {
            return fallback;
        }

        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";

        if (!lastName.isEmpty() && !firstName.isEmpty()) {
            return lastName + ", " + firstName;
        }
        if (!lastName.isEmpty()) {
            return lastName;
        }
        if (!firstName.isEmpty()) {
            return firstName;
        }
        return fallback;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication.getName();
    }
}
