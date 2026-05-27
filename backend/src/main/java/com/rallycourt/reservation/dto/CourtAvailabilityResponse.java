package com.rallycourt.reservation.dto;

import java.time.LocalDateTime;

public record CourtAvailabilityResponse(
        Long reservationId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String reservationStatus,
        String paymentStatus,
        String label
) {
}
