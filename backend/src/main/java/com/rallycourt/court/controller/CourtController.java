package com.rallycourt.court.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.dto.CourtRequest;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.service.CourtService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final CourtService courtService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CourtPageResponse> getCourts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(courtService.getCourts(page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COURT_OWNER')")
    @ActivityLogAnnotation("COURT_CREATED")
    public ResponseEntity<Court> createCourt(@Valid @RequestBody CourtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courtService.createCourt(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURT_OWNER')")
    @ActivityLogAnnotation("COURT_UPDATED")
    public ResponseEntity<Court> updateCourt(@PathVariable Long id, @Valid @RequestBody CourtRequest request) {
        return ResponseEntity.ok(courtService.updateCourt(id, request));
    }
}
