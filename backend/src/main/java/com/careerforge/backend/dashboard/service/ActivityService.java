package com.careerforge.backend.dashboard.service;

import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.dashboard.dto.RecentActivityEntry;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.resume.repository.ResumeRepository;
import com.careerforge.backend.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final int FEED_LIMIT = 10;

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ApplicationRepository applicationRepository;
    private final PdfExportUsageRepository pdfExportUsageRepository;

    @Transactional(readOnly = true)
    public List<RecentActivityEntry> getRecentActivity(User user) {
        List<RecentActivityEntry> events = new ArrayList<>();

        // Resume updates
        resumeRepository.findTop5ByUserIdOrderByUpdatedAtDesc(user.getId())
                .forEach(r -> events.add(new RecentActivityEntry(
                        "RESUME_UPDATED",
                        "Updated resume",
                        r.getName(),
                        "/resumes/" + r.getId(),
                        r.getUpdatedAt())));

        // New resume versions
        resumeVersionRepository.findTop5ByResumeUserIdOrderByCreatedAtDesc(user.getId())
                .forEach(v -> events.add(new RecentActivityEntry(
                        "VERSION_CREATED",
                        "Created version v" + v.getVersionNumber(),
                        v.getResume().getName(),
                        "/resumes/" + v.getResume().getId(),
                        v.getCreatedAt())));

        // New applications
        applicationRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .forEach(a -> events.add(new RecentActivityEntry(
                        "APPLICATION_ADDED",
                        "Applied to " + a.getCompanyName(),
                        a.getJobTitle(),
                        "/applications",
                        a.getCreatedAt())));

        // PDF exports
        pdfExportUsageRepository.findTop5ByUserIdOrderByUpdatedAtDesc(user.getId())
                .forEach(p -> events.add(new RecentActivityEntry(
                        "PDF_EXPORTED",
                        "Exported PDF",
                        p.getBillingPeriod().getMonth().name().charAt(0)
                                + p.getBillingPeriod().getMonth().name().substring(1).toLowerCase()
                                + " " + p.getBillingPeriod().getYear()
                                + " (" + p.getExportCount() + " exports)",
                        "/resumes",
                        p.getUpdatedAt())));

        return events.stream()
                .sorted(Comparator.comparing(RecentActivityEntry::occurredAt).reversed())
                .limit(FEED_LIMIT)
                .toList();
    }
}
