package com.rallycourt.court.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CourtManagementReservationSummary(
        Long reservationId,
        LocalDate reservationDate,
        LocalTime startTime,
        Integer durationMinutes,
        String paymentStatus,
        String reservationStatus,
        String playerName
) {
}
