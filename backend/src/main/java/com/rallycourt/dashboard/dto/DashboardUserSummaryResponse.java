package com.rallycourt.dashboard.dto;

public record DashboardUserSummaryResponse(
        long usersWithBookings,
        long activeUsersWithoutBookings,
        long inactiveUsers
) {
}
