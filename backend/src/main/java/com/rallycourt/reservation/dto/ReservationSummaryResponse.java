package com.rallycourt.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationSummaryResponse(
        Long id,
        Long courtId,
        String courtName,
        String courtType,
        String location,
        Double latitude,
        Double longitude,
        String reservedBy,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime expiresAt,
        String status,
        BigDecimal amountDue
) {
}
