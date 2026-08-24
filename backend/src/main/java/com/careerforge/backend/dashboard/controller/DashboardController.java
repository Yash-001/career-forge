package com.careerforge.backend.dashboard.controller;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.dashboard.dto.AnalyticsSummary;
import com.careerforge.backend.dashboard.dto.DashboardSummary;
import com.careerforge.backend.dashboard.service.AnalyticsService;
import com.careerforge.backend.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<DashboardSummary> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getDashboard(user));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsSummary> getAnalytics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getAnalytics(user));
    }
}
