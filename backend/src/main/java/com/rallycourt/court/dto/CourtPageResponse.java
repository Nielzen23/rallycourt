package com.rallycourt.court.dto;

import com.rallycourt.court.entity.Court;
import java.util.List;

public record CourtPageResponse(
        List<Court> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
