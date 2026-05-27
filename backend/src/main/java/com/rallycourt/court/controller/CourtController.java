package com.rallycourt.court.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.dto.CourtRequest;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.service.CourtService;
import com.rallycourt.reservation.dto.CourtAvailabilityResponse;
import com.rallycourt.reservation.service.ReservationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
@Validated
public class CourtController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtController.class);

    private final CourtService courtService;
    private final ReservationService reservationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @ActivityLogAnnotation("COURTS_VIEWED")
    public ResponseEntity<CourtPageResponse> getCourts(
            @RequestParam(required = false) String courtType,
            @RequestParam(required = false) String venueType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        LOGGER.info("Court list requested with filters courtType={}, venueType={}, status={}, page={}, size={}",
                courtType, venueType, status, page, size);
        return ResponseEntity.ok(courtService.getCourts(courtType, venueType, status, page, size));
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("isAuthenticated()")
    @ActivityLogAnnotation("COURT_AVAILABILITY_VIEWED")
    public ResponseEntity<List<CourtAvailabilityResponse>> getCourtAvailability(
            @PathVariable Long id,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ) {
        LOGGER.info("Court availability requested for courtId {} from {} to {}", id, from, to);
        return ResponseEntity.ok(reservationService.getCourtAvailability(id, from, to));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COURT_OWNER')")
    @ActivityLogAnnotation("COURT_CREATED")
    public ResponseEntity<Court> createCourt(@Valid @RequestBody CourtRequest request) {
        LOGGER.info("Court creation requested for name {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(courtService.createCourt(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURT_OWNER')")
    @ActivityLogAnnotation("COURT_UPDATED")
    public ResponseEntity<Court> updateCourt(@PathVariable Long id, @Valid @RequestBody CourtRequest request) {
        LOGGER.info("Court update requested for courtId {}", id);
        return ResponseEntity.ok(courtService.updateCourt(id, request));
    }
}
