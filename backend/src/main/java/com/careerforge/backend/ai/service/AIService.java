package com.careerforge.backend.ai.service;

import com.careerforge.backend.ai.dto.*;
import com.careerforge.backend.ai.provider.AIProvider;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.resume.domain.ResumeExperience;
import com.careerforge.backend.resume.domain.ResumeSkill;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.service.ResumeService;
import com.careerforge.backend.shared.exception.DomainExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business layer for AI operations.
 *
 * Security contract:
 * - All methods require an authenticated {@link User}.
 * - Resume ownership is verified via {@link ResumeService} before any AI operation.
 * - The authenticated user's ID is never overridden by client-supplied values.
 * - No resume or profile entities are mutated by analyze/tailor; results are suggestions only.
 * - acceptTailoring creates a NEW ResumeVersion; the source version is never modified.
 */
@Service
@RequiredArgsConstructor
public class AIService {

    private final AIProvider aiProvider;
    private final ResumeService resumeService;

    public JobAnalysisResponse analyzeJobDescription(User user, UUID resumeId, UUID versionId,
                                                     String jobDescription) {
        ResumeVersion version = resumeService.getVersionById(user, resumeId, versionId);

        List<String> resumeSkills = version.getSkills().stream()
                .map(ResumeSkill::getName)
                .toList();

        return aiProvider.analyzeJobDescription(
                new JobAnalysisRequest(jobDescription, resumeSkills));
    }

    public TailoringResponse tailorResume(User user, UUID resumeId, UUID versionId,
                                          String jobDescription) {
        ResumeVersion version = resumeService.getVersionById(user, resumeId, versionId);

        List<BulletWithId> bullets = version.getExperiences().stream()
                .filter(e -> e.getDescription() != null && !e.getDescription().isBlank())
                .map(e -> new BulletWithId(e.getId(), e.getDescription()))
                .toList();

        List<String> resumeSkills = version.getSkills().stream()
                .map(ResumeSkill::getName)
                .toList();

        return aiProvider.tailorResume(
                new TailoringRequest(jobDescription, bullets, resumeSkills));
    }

    /**
     * Accepts a set of AI tailoring suggestions by cloning the source version and
     * applying the accepted description overrides. The source version is never modified.
     *
     * Security: ownership is verified via ResumeService before any write occurs.
     * Each experienceId is validated against the source version to prevent injection.
     */
    public ResumeVersion acceptTailoring(User user, UUID resumeId, UUID versionId,
                                         AcceptTailoringRequest request) {
        ResumeVersion sourceVersion = resumeService.getVersionById(user, resumeId, versionId);

        // Build a map of experienceId → suggestedText for fast lookup
        Map<UUID, String> acceptedMap = request.acceptedSuggestions().stream()
                .collect(Collectors.toMap(AcceptedSuggestion::experienceId,
                                          AcceptedSuggestion::suggestedText));

        // Validate every submitted experienceId belongs to the source version
        Map<UUID, ResumeExperience> sourceExpMap = sourceVersion.getExperiences().stream()
                .collect(Collectors.toMap(ResumeExperience::getId, e -> e));

        for (UUID expId : acceptedMap.keySet()) {
            if (!sourceExpMap.containsKey(expId)) {
                throw DomainExceptions.invalidSuggestion();
            }
        }

        return resumeService.cloneVersionWithTailoring(resumeId, sourceVersion, acceptedMap);
    }

    public String activeProviderName() {
        return aiProvider.providerName();
    }
}
