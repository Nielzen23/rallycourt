package com.rallycourt.activity.dto;

import java.util.List;

public record AdminActivityHistoryPageResponse(
        List<AdminActivityHistoryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
