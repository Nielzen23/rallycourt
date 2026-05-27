package com.rallycourt.reservation.dto;

import java.util.List;

public record ReservationPageResponse(
        List<ReservationSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
