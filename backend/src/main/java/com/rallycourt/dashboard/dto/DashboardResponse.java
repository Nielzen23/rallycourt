package com.rallycourt.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        long totalBookings,
        long paidBookings,
        long unpaidBookings,
        List<DashboardCountItemResponse> bookingPaymentBreakdown,
        List<DashboardCountItemResponse> popularCourts,
        List<DashboardCountItemResponse> popularSports,
        DashboardUserSummaryResponse userSummary
) {
}
