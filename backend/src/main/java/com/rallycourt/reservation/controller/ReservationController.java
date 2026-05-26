package com.rallycourt.reservation.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.reservation.dto.CreateReservationRequest;
import com.rallycourt.reservation.dto.ReservationConfigResponse;
import com.rallycourt.reservation.dto.ReservationPageResponse;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ActivityLogAnnotation("RESERVATION_CREATED")
    public ResponseEntity<Reservation> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @ActivityLogAnnotation("RESERVATIONS_VIEWED")
    public ResponseEntity<ReservationPageResponse> getMyReservations(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(reservationService.getMyReservations(page, size));
    }

    @GetMapping("/config")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationConfigResponse> getReservationConfig() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(reservationService.getReservationConfig());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ActivityLogAnnotation("RESERVATION_CANCELLED")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }
}
