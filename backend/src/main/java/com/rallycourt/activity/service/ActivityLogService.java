package com.rallycourt.activity.service;

import com.rallycourt.activity.dto.AdminActivityHistoryPageResponse;
import com.rallycourt.activity.dto.AdminUserActivitySummaryResponse;
import java.util.List;

public interface ActivityLogService {

    void log(String action, String status);

    List<AdminUserActivitySummaryResponse> getUserActivitySummaries();

    AdminActivityHistoryPageResponse getUserActivityHistory(Long userId, int page, int size);
}
