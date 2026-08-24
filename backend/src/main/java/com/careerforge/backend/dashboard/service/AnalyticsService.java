package com.careerforge.backend.dashboard.service;

import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.dashboard.dto.AnalyticsSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TREND_MONTHS = 12;

    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummary getAnalytics(User user) {
        long applied   = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.APPLIED);
        long interview = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.INTERVIEW);
        long offer     = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.OFFER);
        long rejected  = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.REJECTED);

        LocalDate since = LocalDate.now().minusMonths(TREND_MONTHS - 1).withDayOfMonth(1);
        var trend = applicationRepository.findMonthlyTrend(user.getId(), since);

        return new AnalyticsSummary(applied, interview, offer, rejected, trend);
    }
}
