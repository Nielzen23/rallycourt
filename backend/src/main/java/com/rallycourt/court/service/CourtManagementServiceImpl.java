package com.rallycourt.court.service;

import com.rallycourt.court.dto.CourtManagementDetailsResponse;
import com.rallycourt.court.dto.CourtManagementRequest;
import com.rallycourt.court.dto.CourtManagementReservationSummary;
import com.rallycourt.court.dto.CourtAddressSuggestionResponse;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.entity.CourtStatus;
import com.rallycourt.court.entity.CourtType;
import com.rallycourt.court.entity.VenueType;
import com.rallycourt.court.exception.CourtNotFoundException;
import com.rallycourt.court.exception.CourtValidationException;
import com.rallycourt.court.geocoding.Coordinates;
import com.rallycourt.court.geocoding.GeocodingService;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.court.repository.CourtTypeRepository;
import com.rallycourt.court.repository.CourtTypeVenueTypeRepository;
import com.rallycourt.court.repository.VenueTypeRepository;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtManagementServiceImpl implements CourtManagementService {

    private static final LocalTime DEFAULT_OPEN_TIME = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSE_TIME = LocalTime.of(22, 0);
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtManagementServiceImpl.class);
    private static final Set<ReservationStatus> ACTIVE_FUTURE_RESERVATION_STATUSES = Set.of(
            ReservationStatus.RESERVED_PENDING_PAYMENT,
            ReservationStatus.CONFIRMED
    );

    private final CourtRepository courtRepository;
    private final CourtTypeRepository courtTypeRepository;
    private final VenueTypeRepository venueTypeRepository;
    private final CourtTypeVenueTypeRepository courtTypeVenueTypeRepository;
    private final CourtAccessService courtAccessService;
    private final GeocodingService geocodingService;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public CourtPageResponse getCourts(String courtTypeCode, String venueTypeCode, String statusCode, int page, int size) {
        LOGGER.info("Fetching managed courts with filters courtType={}, venueType={}, status={}, page={}, size={}",
                courtTypeCode, venueTypeCode, statusCode, page, size);
        String normalizedCourtTypeCode = normalizeFilter(courtTypeCode);
        String normalizedVenueTypeCode = normalizeFilter(venueTypeCode);
        CourtStatus status = resolveCourtStatus(statusCode);

        if (normalizedCourtTypeCode != null) {
            resolveCourtType(normalizedCourtTypeCode);
        }
        if (normalizedVenueTypeCode != null) {
            resolveVenueType(normalizedVenueTypeCode);
        }

        Page<Court> courts = courtRepository.findAllFiltered(
                normalizedCourtTypeCode,
                normalizedVenueTypeCode,
                status,
                PageRequest.of(page, size)
        );

        return new CourtPageResponse(
                courts.getContent(),
                courts.getNumber(),
                courts.getSize(),
                courts.getTotalElements(),
                courts.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<CourtAddressSuggestionResponse> getAddressSuggestions(String query) {
        LOGGER.info("Fetching address suggestions for query length {}", query == null ? 0 : query.trim().length());
        return geocodingService.autocomplete(query == null ? "" : query.trim());
    }

    @Transactional
    public Court createCourt(CourtManagementRequest request) {
        LOGGER.info("Creating managed court {}", request.getName());
        CourtType courtType = resolveCourtType(normalizeRequired(request.getCourtType()));
        VenueType venueType = resolveVenueType(normalizeRequired(request.getVenueType()));
        CourtStatus status = resolveRequiredCourtStatus(request.getStatus());
        validateCourtVenueCombination(courtType, venueType);

        LocalTime openTime = request.getOpenTime() != null ? request.getOpenTime() : DEFAULT_OPEN_TIME;
        LocalTime closeTime = request.getCloseTime() != null ? request.getCloseTime() : DEFAULT_CLOSE_TIME;
        validateOperatingHours(openTime, closeTime);

        Coordinates coordinates = geocodingService.geocode(request.getLocation());

        Court court = new Court();
        court.setName(request.getName().trim());
        court.setLocation(request.getLocation().trim());
        court.setCourtType(courtType);
        court.setVenueType(venueType);
        court.setStatus(status);
        court.setOpenTime(openTime);
        court.setCloseTime(closeTime);
        court.setHourlyRate(request.getHourlyRate());
        court.setLatitude(coordinates.latitude());
        court.setLongitude(coordinates.longitude());
        court.setOwner(courtAccessService.getCurrentUser());
        LOGGER.info("Managed court {} prepared for persistence", request.getName());
        return courtRepository.save(court);
    }

    @Transactional
    public Court updateCourt(Long courtId, CourtManagementRequest request) {
        LOGGER.info("Updating managed courtId {}", courtId);
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));

        CourtType courtType = resolveCourtType(normalizeRequired(request.getCourtType()));
        VenueType venueType = resolveVenueType(normalizeRequired(request.getVenueType()));
        CourtStatus status = resolveRequiredCourtStatus(request.getStatus());
        validateCourtVenueCombination(courtType, venueType);

        LocalTime openTime = request.getOpenTime() != null ? request.getOpenTime() : court.getOpenTime();
        LocalTime closeTime = request.getCloseTime() != null ? request.getCloseTime() : court.getCloseTime();
        validateOperatingHours(openTime, closeTime);

        court.setName(request.getName().trim());
        court.setCourtType(courtType);
        court.setVenueType(venueType);
        court.setStatus(status);
        court.setOpenTime(openTime);
        court.setCloseTime(closeTime);
        court.setHourlyRate(request.getHourlyRate());

        if (!court.getLocation().equals(request.getLocation().trim())) {
            LOGGER.info("Re-geocoding updated location for courtId {}", courtId);
            Coordinates coordinates = geocodingService.geocode(request.getLocation().trim());
            court.setLocation(request.getLocation().trim());
            court.setLatitude(coordinates.latitude());
            court.setLongitude(coordinates.longitude());
        }

        return courtRepository.save(court);
    }

    @Transactional
    public void deleteCourt(Long courtId) {
        LOGGER.info("Deleting managed courtId {}", courtId);
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));

        boolean hasActiveFutureReservations = reservationRepository.existsByCourtIdAndStatusInAndStartTimeGreaterThan(
                courtId,
                ACTIVE_FUTURE_RESERVATION_STATUSES,
                LocalDateTime.now()
        );

        if (hasActiveFutureReservations) {
            throw new CourtValidationException("Court cannot be deleted while it has active future reservations");
        }

        courtRepository.delete(court);
    }

    @Transactional(readOnly = true)
    public CourtManagementDetailsResponse getCourtDetails(Long courtId) {
        LOGGER.info("Fetching court management details for courtId {}", courtId);
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));

        List<CourtManagementReservationSummary> reservations = getUpcomingReservations(courtId);
        return new CourtManagementDetailsResponse(
                court.getId(),
                court.getName(),
                court.getCourtTypeCode(),
                court.getVenueTypeCode(),
                court.getLocation(),
                court.getLatitude(),
                court.getLongitude(),
                court.getStatus().name(),
                court.getOpenTime().toString(),
                court.getCloseTime().toString(),
                court.getHourlyRate(),
                reservations
        );
    }

    @Transactional(readOnly = true)
    public List<CourtManagementReservationSummary> getUpcomingReservations(Long courtId) {
        LOGGER.info("Fetching upcoming reservations for courtId {}", courtId);
        if (!courtRepository.existsById(courtId)) {
            throw new CourtNotFoundException(courtId);
        }

        List<Reservation> upcomingReservations = reservationRepository
                .findByCourtIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(courtId, LocalDateTime.now());
        Map<Long, Payment> paymentsByReservationId = paymentRepository
                .findByReservationIdIn(upcomingReservations.stream().map(Reservation::getId).toList())
                .stream()
                .collect(Collectors.toMap(Payment::getReservationId, Function.identity(), (left, right) -> right));

        return upcomingReservations.stream()
                .sorted(Comparator.comparing(Reservation::getStartTime))
                .map(reservation -> {
                    Payment payment = paymentsByReservationId.get(reservation.getId());
                    String paymentStatus = payment != null && payment.getStatus() == PaymentStatus.SUCCESS
                            ? "PAID"
                            : "PENDING";
                    return new CourtManagementReservationSummary(
                            reservation.getId(),
                            reservation.getStartTime().toLocalDate(),
                            reservation.getStartTime().toLocalTime(),
                            Math.toIntExact(Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes()),
                            paymentStatus,
                            reservation.getStatus().name(),
                            reservation.getReservedBy()
                    );
                })
                .toList();
    }

    private void validateOperatingHours(LocalTime openTime, LocalTime closeTime) {
        if (!openTime.isBefore(closeTime)) {
            throw new CourtValidationException("Open time must be earlier than close time");
        }
    }

    private CourtType resolveCourtType(String code) {
        return courtTypeRepository.findByCode(code)
                .orElseThrow(() -> new CourtValidationException("Unsupported court type: " + code));
    }

    private VenueType resolveVenueType(String code) {
        return venueTypeRepository.findByCode(code)
                .orElseThrow(() -> new CourtValidationException("Unsupported venue type: " + code));
    }

    private void validateCourtVenueCombination(CourtType courtType, VenueType venueType) {
        if (!courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code(
                courtType.getCode(),
                venueType.getCode()
        )) {
            throw new CourtValidationException(
                    courtType.getCode() + " is not supported for " + venueType.getCode() + " venues"
            );
        }
    }

    private String normalizeRequired(String value) {
        return value.trim().toUpperCase();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private CourtStatus resolveRequiredCourtStatus(String code) {
        CourtStatus status = resolveCourtStatus(code);
        if (status == null) {
            throw new CourtValidationException("Court status is required");
        }
        return status;
    }

    private CourtStatus resolveCourtStatus(String code) {
        String normalized = normalizeFilter(code);
        if (normalized == null) {
            return null;
        }
        try {
            return CourtStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new CourtValidationException("Unsupported court status: " + code);
        }
    }
}
