package com.careerforge.backend.ai;

import com.careerforge.backend.ai.dto.AIAnalyzeRequest;
import com.careerforge.backend.ai.dto.AITailorRequest;
import com.careerforge.backend.ai.dto.AcceptTailoringRequest;
import com.careerforge.backend.ai.dto.JobAnalysisResponse;
import com.careerforge.backend.ai.dto.TailoringResponse;
import com.careerforge.backend.ai.service.AIService;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.resume.domain.ResumeEducation;
import com.careerforge.backend.resume.domain.ResumeExperience;
import com.careerforge.backend.resume.domain.ResumeSkill;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.dto.ResumeEducationResponse;
import com.careerforge.backend.resume.dto.ResumeExperienceResponse;
import com.careerforge.backend.resume.dto.ResumeSkillResponse;
import com.careerforge.backend.resume.dto.ResumeVersionResponse;
import com.careerforge.backend.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final ResumeService resumeService;

    @PostMapping("/resumes/{resumeId}/versions/{versionId}/analyze")
    public ResponseEntity<JobAnalysisResponse> analyze(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody AIAnalyzeRequest request) {

        return ResponseEntity.ok(
                aiService.analyzeJobDescription(user, resumeId, versionId, request.jobDescription()));
    }

    @PostMapping("/resumes/{resumeId}/versions/{versionId}/tailor")
    public ResponseEntity<TailoringResponse> tailor(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody AITailorRequest request) {

        return ResponseEntity.ok(
                aiService.tailorResume(user, resumeId, versionId, request.jobDescription()));
    }

    /**
     * Accepts a set of AI tailoring suggestions and creates a new ResumeVersion.
     *
     * POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/accept-tailoring
     *
     * The source version is never modified. Returns 201 with the new ResumeVersionResponse.
     * Returns 404 if the resume/version does not exist or belongs to another user.
     * Returns 422 if any experienceId does not belong to the source version.
     */
    @PostMapping("/resumes/{resumeId}/versions/{versionId}/accept-tailoring")
    public ResponseEntity<ResumeVersionResponse> acceptTailoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody AcceptTailoringRequest request) {

        ResumeVersion newVersion = aiService.acceptTailoring(user, resumeId, versionId, request);
        int latestNum = resumeService.getLatestVersionNumber(resumeId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toVersionResponse(newVersion, newVersion.getVersionNumber() == latestNum));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ResumeVersionResponse toVersionResponse(ResumeVersion v, boolean isLatest) {
        List<ResumeExperienceResponse> exps = v.getExperiences().stream()
                .map(this::toExpResponse).toList();
        List<ResumeEducationResponse> edus = v.getEducations().stream()
                .map(this::toEduResponse).toList();
        List<ResumeSkillResponse> skills = v.getSkills().stream()
                .map(this::toSkillResponse).toList();
        return new ResumeVersionResponse(v.getId(), v.getVersionNumber(), v.getTitle(),
                v.getProfessionalSummary(), isLatest, exps, edus, skills, v.getCreatedAt());
    }

    private ResumeExperienceResponse toExpResponse(ResumeExperience e) {
        return new ResumeExperienceResponse(e.getId(), e.getCompanyName(), e.getJobTitle(),
                e.getLocation(), e.getEmploymentType(), e.getStartDate(), e.getEndDate(),
                e.isCurrentlyWorking(), e.getDescription(), e.getDisplayOrder());
    }

    private ResumeEducationResponse toEduResponse(ResumeEducation e) {
        return new ResumeEducationResponse(e.getId(), e.getInstitutionName(), e.getDegree(),
                e.getFieldOfStudy(), e.getLocation(), e.getStartDate(), e.getEndDate(),
                e.getGrade(), e.getDescription(), e.getDisplayOrder());
    }

    private ResumeSkillResponse toSkillResponse(ResumeSkill s) {
        return new ResumeSkillResponse(s.getId(), s.getName(), s.getCategory(),
                s.getProficiency(), s.getDisplayOrder());
    }
}
