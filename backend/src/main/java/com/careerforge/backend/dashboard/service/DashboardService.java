package com.careerforge.backend.dashboard.service;

import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.billing.Subscription;
import com.careerforge.backend.billing.SubscriptionService;
import com.careerforge.backend.dashboard.dto.*;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.pdf.service.ExportLimitService;
import com.careerforge.backend.profile.domain.MasterProfile;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.resume.domain.Resume;
import com.careerforge.backend.resume.repository.ResumeRepository;
import com.careerforge.backend.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_LIMIT = 5;

    private final MasterProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ApplicationRepository applicationRepository;
    private final SubscriptionService subscriptionService;
    private final PdfExportUsageRepository pdfExportUsageRepository;
    private final AnalyticsService analyticsService;
    private final ActivityService activityService;

    @Transactional(readOnly = true)
    public DashboardSummary getDashboard(User user) {
        return new DashboardSummary(
                buildProfileSummary(user),
                buildResumeSummary(user),
                buildApplicationSummary(user),
                buildSubscriptionSummary(user),
                buildUsageSummary(user),
                buildQuickActions(user),
                analyticsService.getAnalytics(user),
                activityService.getRecentActivity(user)
        );
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    private ProfileSummary buildProfileSummary(User user) {
        MasterProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return new ProfileSummary(false, false, false, false, 0);
        }
        boolean hasTitle = profile.getProfessionalTitle() != null && !profile.getProfessionalTitle().isBlank();
        boolean hasSummary = profile.getProfessionalSummary() != null && !profile.getProfessionalSummary().isBlank();
        boolean hasContact = profile.getPhone() != null && !profile.getPhone().isBlank();
        int completion = computeProfileCompletion(hasTitle, hasSummary, hasContact);
        return new ProfileSummary(true, hasTitle, hasSummary, hasContact, completion);
    }

    private int computeProfileCompletion(boolean hasTitle, boolean hasSummary, boolean hasContact) {
        int score = 25;
        if (hasTitle)   score += 25;
        if (hasSummary) score += 25;
        if (hasContact) score += 25;
        return score;
    }

    // ── Resumes ───────────────────────────────────────────────────────────────

    private ResumeSummary buildResumeSummary(User user) {
        long resumeCount  = resumeRepository.countByUserId(user.getId());
        long versionCount = resumeVersionRepository.countByResumeUserId(user.getId());
        List<RecentResumeEntry> recent = resumeRepository
                .findTop5WithMaxVersionByUserId(user.getId())
                .stream()
                .map(row -> {
                    Resume r = (Resume) row[0];
                    int maxVersion = ((Number) row[1]).intValue();
                    return new RecentResumeEntry(r.getId(), r.getName(), maxVersion, r.getUpdatedAt());
                })
                .toList();
        return new ResumeSummary(resumeCount, versionCount, recent);
    }

    // ── Applications ──────────────────────────────────────────────────────────

    private ApplicationSummary buildApplicationSummary(User user) {
        long total     = applicationRepository.countByUserId(user.getId());
        long applied   = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.APPLIED);
        long interview = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.INTERVIEW);
        long offer     = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.OFFER);
        long rejected  = applicationRepository.countByUserIdAndStatus(user.getId(), ApplicationStatus.REJECTED);

        List<RecentApplicationEntry> recent = applicationRepository
                .findTop5ByUserIdOrderByApplicationDateDesc(user.getId())
                .stream()
                .map(a -> new RecentApplicationEntry(
                        a.getId(),
                        a.getCompanyName(),
                        a.getJobTitle(),
                        a.getApplicationDate(),
                        a.getStatus().name(),
                        a.getJobUrl()))
                .toList();

        return new ApplicationSummary(total, applied, interview, offer, rejected, recent);
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    private SubscriptionSummary buildSubscriptionSummary(User user) {
        Subscription sub = subscriptionService.findActiveSubscription(user).orElse(null);
        if (sub == null) {
            return new SubscriptionSummary(
                    user.getSubscriptionTier(), null, null, null, null);
        }
        return new SubscriptionSummary(
                sub.getTier(),
                sub.getStatus(),
                sub.getProvider(),
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd());
    }

    // ── Usage ─────────────────────────────────────────────────────────────────

    private UsageSummary buildUsageSummary(User user) {
        if (subscriptionService.isPro(user)) {
            return new UsageSummary(0, 0, false);
        }
        LocalDate period = LocalDate.now().withDayOfMonth(1);
        int used = pdfExportUsageRepository
                .findByUserIdAndBillingPeriod(user.getId(), period)
                .map(u -> u.getExportCount())
                .orElse(0);
        int limit = ExportLimitService.FREE_MONTHLY_LIMIT;
        return new UsageSummary(used, limit, used >= limit);
    }

    // ── Quick Actions ─────────────────────────────────────────────────────────

    private QuickActions buildQuickActions(User user) {
        boolean isPro = subscriptionService.isPro(user);
        long resumeCount = resumeRepository.countByUserId(user.getId());
        boolean canCreateResume = isPro || resumeCount < 2;
        return new QuickActions(canCreateResume, true, !isPro);
    }
}
