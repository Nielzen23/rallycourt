package com.rallycourt.court.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.court.dto.CourtAddressSuggestionResponse;
import com.rallycourt.court.dto.CourtManagementDetailsResponse;
import com.rallycourt.court.dto.CourtManagementRequest;
import com.rallycourt.court.dto.CourtManagementReservationSummary;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.service.CourtManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/court-management/courts")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'COURT_OWNER')")
public class CourtManagementController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtManagementController.class);

    private final CourtManagementService courtManagementService;

    @GetMapping
    @ActivityLogAnnotation("COURT_MANAGEMENT_VIEWED")
    public ResponseEntity<CourtPageResponse> getCourts(
            @RequestParam(required = false) String courtType,
            @RequestParam(required = false) String venueType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        LOGGER.info("Court management list requested with filters courtType={}, venueType={}, status={}, page={}, size={}",
                courtType, venueType, status, page, size);
        return ResponseEntity.ok(courtManagementService.getCourts(courtType, venueType, status, page, size));
    }

    @GetMapping("/address-suggestions")
    @ActivityLogAnnotation("COURT_ADDRESS_SUGGESTIONS_VIEWED")
    public ResponseEntity<List<CourtAddressSuggestionResponse>> getAddressSuggestions(
            @RequestParam String query
    ) {
        LOGGER.info("Court address suggestions requested for query length {}", query != null ? query.length() : 0);
        return ResponseEntity.ok(courtManagementService.getAddressSuggestions(query));
    }

    @PostMapping
    @ActivityLogAnnotation("ADMIN_COURT_CREATED")
    public ResponseEntity<Court> createCourt(@Valid @RequestBody CourtManagementRequest request) {
        LOGGER.info("Court management creation requested for name {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(courtManagementService.createCourt(request));
    }

    @PutMapping("/{courtId}")
    @ActivityLogAnnotation("ADMIN_COURT_UPDATED")
    public ResponseEntity<Court> updateCourt(
            @PathVariable Long courtId,
            @Valid @RequestBody CourtManagementRequest request
    ) {
        LOGGER.info("Court management update requested for courtId {}", courtId);
        return ResponseEntity.ok(courtManagementService.updateCourt(courtId, request));
    }

    @DeleteMapping("/{courtId}")
    @ActivityLogAnnotation("ADMIN_COURT_DELETED")
    public ResponseEntity<Void> deleteCourt(@PathVariable Long courtId) {
        LOGGER.info("Court deletion requested for courtId {}", courtId);
        courtManagementService.deleteCourt(courtId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courtId}")
    @ActivityLogAnnotation("COURT_DETAILS_VIEWED")
    public ResponseEntity<CourtManagementDetailsResponse> getCourtDetails(@PathVariable Long courtId) {
        LOGGER.info("Court details requested for courtId {}", courtId);
        return ResponseEntity.ok(courtManagementService.getCourtDetails(courtId));
    }

    @GetMapping("/{courtId}/reservations")
    @ActivityLogAnnotation("COURT_RESERVATIONS_VIEWED")
    public ResponseEntity<List<CourtManagementReservationSummary>> getCourtReservations(@PathVariable Long courtId) {
        LOGGER.info("Court reservations requested for courtId {}", courtId);
        return ResponseEntity.ok(courtManagementService.getUpcomingReservations(courtId));
    }
}
