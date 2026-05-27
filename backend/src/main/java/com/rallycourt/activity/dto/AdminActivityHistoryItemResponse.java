package com.rallycourt.activity.dto;

import java.time.Instant;

public record AdminActivityHistoryItemResponse(
        String id,
        String action,
        String status,
        Instant createdAt
) {
}
