package com.careerforge.backend.application.service;

import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.application.dto.CreateApplicationRequest;
import com.careerforge.backend.application.dto.UpdateApplicationRequest;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.repository.ResumeVersionRepository;
import com.careerforge.backend.shared.exception.DomainExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ResumeVersionRepository resumeVersionRepository;

    @Transactional
    public Application create(User user, CreateApplicationRequest req) {
        ResumeVersion version = resolveOwnedVersion(user, req.resumeVersionId());
        Application app = Application.builder()
                .user(user)
                .companyName(req.companyName())
                .jobTitle(req.jobTitle())
                .applicationDate(req.applicationDate())
                .jobUrl(req.jobUrl())
                .status(req.status() != null ? req.status() : ApplicationStatus.APPLIED)
                .resumeVersion(version)
                .build();
        return applicationRepository.save(app);
    }

    @Transactional(readOnly = true)
    public List<Application> listForUser(User user) {
        return applicationRepository.findByUserIdOrderByApplicationDateDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public Application getOwned(User user, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, user.getId())
                .orElseThrow(DomainExceptions::applicationNotFound);
    }

    @Transactional
    public Application update(User user, UUID applicationId, UpdateApplicationRequest req) {
        Application app = applicationRepository.findByIdAndUserId(applicationId, user.getId())
                .orElseThrow(DomainExceptions::applicationNotFound);
        ResumeVersion version = resolveOwnedVersion(user, req.resumeVersionId());
        app.setCompanyName(req.companyName());
        app.setJobTitle(req.jobTitle());
        app.setApplicationDate(req.applicationDate());
        app.setJobUrl(req.jobUrl());
        app.setStatus(req.status());
        app.setResumeVersion(version);
        return applicationRepository.save(app);
    }

    @Transactional
    public void delete(User user, UUID applicationId) {
        Application app = applicationRepository.findByIdAndUserId(applicationId, user.getId())
                .orElseThrow(DomainExceptions::applicationNotFound);
        applicationRepository.delete(app);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns null when versionId is null (optional link).
     * Returns the version only if it belongs to a resume owned by the user.
     * Returns 404 (no existence leakage) for non-existent or cross-user versions.
     */
    private ResumeVersion resolveOwnedVersion(User user, UUID versionId) {
        if (versionId == null) return null;
        return resumeVersionRepository.findByIdAndUserId(versionId, user.getId())
                .orElseThrow(DomainExceptions::resumeVersionNotFound);
    }
}
