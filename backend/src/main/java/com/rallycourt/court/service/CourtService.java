package com.rallycourt.court.service;

import com.rallycourt.auth.entity.User;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.dto.CourtRequest;
import com.rallycourt.court.entity.Court;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final GeocodingService geocodingService;
    private final CourtAccessService courtAccessService;
    private final CourtTypeRepository courtTypeRepository;
    private final VenueTypeRepository venueTypeRepository;
    private final CourtTypeVenueTypeRepository courtTypeVenueTypeRepository;

    @Transactional(readOnly = true)
    public CourtPageResponse getCourts(int page, int size) {
        Page<Court> courts = courtRepository.findAll(PageRequest.of(page, size));
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
        court.setOpenTime(request.getOpenTime());
        court.setCloseTime(request.getCloseTime());
        court.setOwner(currentUser);

        return courtRepository.save(court);
    }

    @Transactional
    public Court updateCourt(Long courtId, CourtRequest request) {
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

        if (!court.getLocation().equals(request.getLocation())) {
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
}
