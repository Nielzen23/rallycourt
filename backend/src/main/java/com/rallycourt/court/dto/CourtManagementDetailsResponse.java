package com.rallycourt.court.dto;

import java.util.List;
import java.math.BigDecimal;

public record CourtManagementDetailsResponse(
        Long id,
        String name,
        String courtType,
        String venueType,
        String location,
        Double latitude,
        Double longitude,
        String status,
        String openTime,
        String closeTime,
        BigDecimal hourlyRate,
        List<CourtManagementReservationSummary> upcomingReservations
) {
}
