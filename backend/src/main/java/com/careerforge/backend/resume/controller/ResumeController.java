package com.careerforge.backend.resume.controller;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.resume.domain.*;
import com.careerforge.backend.resume.dto.*;
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
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    // ── Resume CRUD ───────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ResumeSummaryResponse>> listResumes(@AuthenticationPrincipal User user) {
        List<Resume> resumes = resumeService.listResumes(user);
        List<ResumeSummaryResponse> body = resumes.stream()
                .map(r -> toSummary(r, resumeService.getLatestVersionNumber(r.getId())))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateResumeRequest request) {
        Resume resume = resumeService.createFromProfile(user, request.name());
        ResumeVersion latest = resumeService.getLatestVersion(user, resume.getId());
        int latestNum = latest.getVersionNumber();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResumeResponse(resume, toVersionSummary(latest, latestNum, true)));
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<ResumeResponse> getResume(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId) {
        Resume resume = resumeService.getResume(user, resumeId);
        ResumeVersion latest = resumeService.getLatestVersion(user, resumeId);
        return ResponseEntity.ok(toResumeResponse(resume,
                toVersionSummary(latest, latest.getVersionNumber(), true)));
    }

    @PutMapping("/{resumeId}")
    public ResponseEntity<ResumeResponse> updateResume(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @Valid @RequestBody UpdateResumeRequest request) {
        Resume resume = resumeService.renameResume(user, resumeId, request.name());
        ResumeVersion latest = resumeService.getLatestVersion(user, resumeId);
        return ResponseEntity.ok(toResumeResponse(resume,
                toVersionSummary(latest, latest.getVersionNumber(), true)));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId) {
        resumeService.deleteResume(user, resumeId);
        return ResponseEntity.noContent().build();
    }

    // ── Version management ────────────────────────────────────────────────────

    @GetMapping("/{resumeId}/versions")
    public ResponseEntity<List<ResumeVersionSummaryResponse>> listVersions(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId) {
        List<ResumeVersion> versions = resumeService.listVersions(user, resumeId);
        int latestNum = versions.isEmpty() ? 0 : versions.get(versions.size() - 1).getVersionNumber();
        List<ResumeVersionSummaryResponse> body = versions.stream()
                .map(v -> toVersionSummary(v, latestNum, v.getVersionNumber() == latestNum))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{resumeId}/versions")
    public ResponseEntity<ResumeVersionResponse> createVersion(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId) {
        ResumeVersion version = resumeService.createNewVersion(user, resumeId);
        int latestNum = resumeService.getLatestVersionNumber(resumeId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toVersionResponse(version, version.getVersionNumber() == latestNum));
    }

    @GetMapping("/{resumeId}/versions/{versionId}")
    public ResponseEntity<ResumeVersionResponse> getVersion(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId) {
        ResumeVersion version = resumeService.getVersionById(user, resumeId, versionId);
        int latestNum = resumeService.getLatestVersionNumber(resumeId);
        return ResponseEntity.ok(toVersionResponse(version, version.getVersionNumber() == latestNum));
    }

    @PutMapping("/{resumeId}/versions/{versionId}")
    public ResponseEntity<ResumeVersionResponse> updateVersionMeta(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody UpdateResumeVersionRequest request) {
        ResumeVersion version = resumeService.updateVersionMeta(
                user, resumeId, versionId, request.title(), request.professionalSummary());
        int latestNum = resumeService.getLatestVersionNumber(resumeId);
        return ResponseEntity.ok(toVersionResponse(version, version.getVersionNumber() == latestNum));
    }

    // ── Version Experience ────────────────────────────────────────────────────

    @PostMapping("/{resumeId}/versions/{versionId}/experiences")
    public ResponseEntity<ResumeExperienceResponse> addExperience(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody ResumeExperienceRequest request) {
        ResumeExperience exp = resumeService.addExperience(user, resumeId, versionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toExpResponse(exp));
    }

    @PutMapping("/{resumeId}/versions/{versionId}/experiences/{experienceId}")
    public ResponseEntity<ResumeExperienceResponse> updateExperience(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @PathVariable UUID experienceId,
            @Valid @RequestBody ResumeExperienceRequest request) {
        ResumeExperience exp = resumeService.updateExperience(user, resumeId, versionId, experienceId, request);
        return ResponseEntity.ok(toExpResponse(exp));
    }

    @DeleteMapping("/{resumeId}/versions/{versionId}/experiences/{experienceId}")
    public ResponseEntity<Void> deleteExperience(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @PathVariable UUID experienceId) {
        resumeService.deleteExperience(user, resumeId, versionId, experienceId);
        return ResponseEntity.noContent().build();
    }

    // ── Version Education ─────────────────────────────────────────────────────

    @PostMapping("/{resumeId}/versions/{versionId}/education")
    public ResponseEntity<ResumeEducationResponse> addEducation(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody ResumeEducationRequest request) {
        ResumeEducation edu = resumeService.addEducation(user, resumeId, versionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toEduResponse(edu));
    }

    @PutMapping("/{resumeId}/versions/{versionId}/education/{educationId}")
    public ResponseEntity<ResumeEducationResponse> updateEducation(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @PathVariable UUID educationId,
            @Valid @RequestBody ResumeEducationRequest request) {
        ResumeEducation edu = resumeService.updateEducation(user, resumeId, versionId, educationId, request);
        return ResponseEntity.ok(toEduResponse(edu));
    }

    @DeleteMapping("/{resumeId}/versions/{versionId}/education/{educationId}")
    public ResponseEntity<Void> deleteEducation(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @PathVariable UUID educationId) {
        resumeService.deleteEducation(user, resumeId, versionId, educationId);
        return ResponseEntity.noContent().build();
    }

    // ── Version Skills ────────────────────────────────────────────────────────

    @PostMapping("/{resumeId}/versions/{versionId}/skills")
    public ResponseEntity<ResumeSkillResponse> addSkill(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @Valid @RequestBody ResumeSkillRequest request) {
        ResumeSkill skill = resumeService.addSkill(user, resumeId, versionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSkillResponse(skill));
    }

    @PutMapping("/{resumeId}/versions/{versionId}/skills/{skillId}")
    public ResponseEntity<ResumeSkillResponse> updateSkill(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @PathVariable UUID skillId,
            @Valid @RequestBody ResumeSkillRequest request) {
        ResumeSkill skill = resumeService.updateSkill(user, resumeId, versionId, skillId, request);
        return ResponseEntity.ok(toSkillResponse(skill));
    }

    @DeleteMapping("/{resumeId}/versions/{versionId}/skills/{skillId}")
    public ResponseEntity<Void> deleteSkill(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resumeId,
            @PathVariable UUID versionId,
            @PathVariable UUID skillId) {
        resumeService.deleteSkill(user, resumeId, versionId, skillId);
        return ResponseEntity.noContent().build();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ResumeSummaryResponse toSummary(Resume r, int latestVersionNumber) {
        return new ResumeSummaryResponse(r.getId(), r.getName(), latestVersionNumber,
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private ResumeResponse toResumeResponse(Resume r, ResumeVersionSummaryResponse latestVersion) {
        return new ResumeResponse(r.getId(), r.getName(), latestVersion,
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private ResumeVersionSummaryResponse toVersionSummary(ResumeVersion v, int latestNum, boolean isLatest) {
        return new ResumeVersionSummaryResponse(v.getId(), v.getVersionNumber(), v.getTitle(),
                isLatest, v.getCreatedAt());
    }

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
