package com.rallycourt.activity.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.activity.dto.AdminActivityHistoryPageResponse;
import com.rallycourt.activity.dto.AdminUserActivitySummaryResponse;
import com.rallycourt.activity.service.ActivityLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class ActivityController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityController.class);

    private final ActivityLogService activityLogService;

    @GetMapping("/users")
    @ActivityLogAnnotation("ADMIN_USER_ACTIVITY_VIEWED")
    public ResponseEntity<List<AdminUserActivitySummaryResponse>> getUserActivitySummaries() {
        LOGGER.info("Admin requested user activity summaries");
        return ResponseEntity.ok(activityLogService.getUserActivitySummaries());
    }

    @GetMapping("/users/{userId}/history")
    @ActivityLogAnnotation("ADMIN_USER_ACTIVITY_HISTORY_VIEWED")
    public ResponseEntity<AdminActivityHistoryPageResponse> getUserActivityHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        LOGGER.info("Admin requested activity history for userId {} page {} size {}", userId, page, size);
        return ResponseEntity.ok(activityLogService.getUserActivityHistory(userId, page, size));
    }
}
