package com.rallycourt.dashboard.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.dashboard.dto.DashboardResponse;
import com.rallycourt.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    @GetMapping
    @ActivityLogAnnotation("ADMIN_DASHBOARD_VIEWED")
    public ResponseEntity<DashboardResponse> getDashboard() {
        LOGGER.info("Dashboard requested");
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
