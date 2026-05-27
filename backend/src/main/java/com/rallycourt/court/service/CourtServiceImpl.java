package com.rallycourt.court.service;

import com.rallycourt.auth.entity.User;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.dto.CourtRequest;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourtServiceImpl implements CourtService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtServiceImpl.class);

    private final CourtRepository courtRepository;
    private final GeocodingService geocodingService;
    private final CourtAccessService courtAccessService;
    private final CourtTypeRepository courtTypeRepository;
    private final VenueTypeRepository venueTypeRepository;
    private final CourtTypeVenueTypeRepository courtTypeVenueTypeRepository;

    @Transactional(readOnly = true)
    public CourtPageResponse getCourts(String courtTypeCode, String venueTypeCode, String statusCode, int page, int size) {
        LOGGER.info("Fetching courts with filters courtType={}, venueType={}, status={}, page={}, size={}",
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

    @Transactional
    public Court createCourt(CourtRequest request) {
        LOGGER.info("Creating court {}", request.getName());
        User currentUser = courtAccessService.getCurrentUser();
        courtAccessService.verifyCanCreateCourt(currentUser);
        validateOperatingHours(request);
        CourtType courtType = resolveCourtType(request.getCourtType());
        VenueType venueType = resolveVenueType(request.getVenueType());
        validateCourtVenueCombination(courtType, venueType);
        Coordinates coordinates = geocodingService.geocode(request.getLocation());

        Court court = new Court();
        court.setName(request.getName());
        court.setLocation(request.getLocation());
        court.setLatitude(coordinates.latitude());
        court.setLongitude(coordinates.longitude());
        court.setCourtType(courtType);
        court.setVenueType(venueType);
        court.setStatus(CourtStatus.AVAILABLE);
        court.setOpenTime(request.getOpenTime());
        court.setCloseTime(request.getCloseTime());
        court.setHourlyRate(request.getHourlyRate());
        court.setOwner(currentUser);

        LOGGER.info("Court {} prepared for persistence by userId {}", request.getName(), currentUser.getId());
        return courtRepository.save(court);
    }

    @Transactional
    public Court updateCourt(Long courtId, CourtRequest request) {
        LOGGER.info("Updating courtId {}", courtId);
        User currentUser = courtAccessService.getCurrentUser();
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));
        courtAccessService.verifyCanUpdateCourt(currentUser, court);
        validateOperatingHours(request);
        CourtType courtType = resolveCourtType(request.getCourtType());
        VenueType venueType = resolveVenueType(request.getVenueType());
        validateCourtVenueCombination(courtType, venueType);

        court.setName(request.getName());
        court.setCourtType(courtType);
        court.setVenueType(venueType);
        court.setOpenTime(request.getOpenTime());
        court.setCloseTime(request.getCloseTime());
        court.setHourlyRate(request.getHourlyRate());

        if (!court.getLocation().equals(request.getLocation())) {
            LOGGER.info("Re-geocoding updated location for courtId {}", courtId);
            Coordinates coordinates = geocodingService.geocode(request.getLocation());
            court.setLocation(request.getLocation());
            court.setLatitude(coordinates.latitude());
            court.setLongitude(coordinates.longitude());
        }

        return courtRepository.save(court);
    }

    private void validateOperatingHours(CourtRequest request) {
        if (!request.getOpenTime().isBefore(request.getCloseTime())) {
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

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
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
