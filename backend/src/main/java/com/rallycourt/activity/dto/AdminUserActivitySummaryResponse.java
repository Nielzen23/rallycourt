package com.rallycourt.activity.dto;

import java.time.Instant;

public record AdminUserActivitySummaryResponse(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role,
        Instant lastActivityAt,
        String lastActivityAction,
        int successfulBookings,
        int failedBookings,
        int totalBookings,
        int successRatioPercentage
) {
}
