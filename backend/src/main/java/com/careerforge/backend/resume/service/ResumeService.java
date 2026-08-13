package com.careerforge.backend.resume.service;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.profile.domain.*;
import com.careerforge.backend.profile.repository.*;
import com.careerforge.backend.resume.domain.*;
import com.careerforge.backend.resume.dto.*;
import com.careerforge.backend.resume.repository.*;
import com.careerforge.backend.shared.exception.ApiException;
import com.careerforge.backend.shared.exception.DomainExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeExperienceRepository resumeExperienceRepository;
    private final ResumeEducationRepository resumeEducationRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final MasterProfileRepository profileRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;

    // ── Resume CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public Resume createFromProfile(User user, String name) {
        if (name == null || name.isBlank()) {
            throw DomainExceptions.resumeNameBlank();
        }
        MasterProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(DomainExceptions::profileNotFound);

        Resume resume = Resume.builder().user(user).name(name).build();
        resumeRepository.save(resume);
        createVersion(resume, profile, 1);
        return resume;
    }

    @Transactional(readOnly = true)
    public List<Resume> listResumes(User user) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public Resume getResume(User user, UUID resumeId) {
        return requireOwnedResume(user, resumeId);
    }

    @Transactional
    public Resume renameResume(User user, UUID resumeId, String name) {
        if (name == null || name.isBlank()) {
            throw DomainExceptions.resumeNameBlank();
        }
        Resume resume = requireOwnedResume(user, resumeId);
        resume.setName(name);
        return resumeRepository.save(resume);
    }

    @Transactional
    public void deleteResume(User user, UUID resumeId) {
        Resume resume = requireOwnedResume(user, resumeId);
        resumeRepository.delete(resume);
    }

    // ── Version management ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResumeVersion> listVersions(User user, UUID resumeId) {
        requireOwnedResume(user, resumeId);
        return resumeVersionRepository.findByResumeIdOrderByVersionNumberAsc(resumeId);
    }

    @Transactional(readOnly = true)
    public ResumeVersion getVersionById(User user, UUID resumeId, UUID versionId) {
        requireOwnedResume(user, resumeId);
        return resumeVersionRepository.findByIdAndResumeId(versionId, resumeId)
                .orElseThrow(DomainExceptions::resumeVersionNotFound);
    }

    @Transactional(readOnly = true)
    public ResumeVersion getVersion(User user, UUID resumeId, int versionNumber) {
        requireOwnedResume(user, resumeId);
        return resumeVersionRepository.findByResumeIdAndVersionNumber(resumeId, versionNumber)
                .orElseThrow(DomainExceptions::resumeVersionNotFound);
    }

    @Transactional(readOnly = true)
    public ResumeVersion getLatestVersion(User user, UUID resumeId) {
        requireOwnedResume(user, resumeId);
        int latest = resumeVersionRepository.findMaxVersionNumber(resumeId)
                .orElseThrow(DomainExceptions::resumeVersionNotFound);
        return resumeVersionRepository.findByResumeIdAndVersionNumber(resumeId, latest)
                .orElseThrow(DomainExceptions::resumeVersionNotFound);
    }

    @Transactional
    public ResumeVersion createNewVersion(User user, UUID resumeId) {
        Resume resume = requireOwnedResume(user, resumeId);
        MasterProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(DomainExceptions::profileNotFound);
        int nextVersion = resumeVersionRepository.findMaxVersionNumber(resumeId)
                .map(max -> max + 1)
                .orElse(1);
        return createVersion(resume, profile, nextVersion);
    }

    @Transactional
    public ResumeVersion updateVersionMeta(User user, UUID resumeId, UUID versionId,
                                           String title, String professionalSummary) {
        ResumeVersion version = requireOwnedVersion(user, resumeId, versionId);
        version.setTitle(title);
        version.setProfessionalSummary(professionalSummary);
        return resumeVersionRepository.save(version);
    }

    // ── Version Experience CRUD ───────────────────────────────────────────────

    @Transactional
    public ResumeExperience addExperience(User user, UUID resumeId, UUID versionId,
                                          ResumeExperienceRequest req) {
        ResumeVersion version = requireOwnedVersion(user, resumeId, versionId);
        validateExperienceDates(req.currentlyWorking(), req.startDate(), req.endDate());
        ResumeExperience exp = ResumeExperience.builder()
                .resumeVersion(version)
                .companyName(req.companyName())
                .jobTitle(req.jobTitle())
                .location(req.location())
                .employmentType(req.employmentType())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .currentlyWorking(req.currentlyWorking())
                .description(req.description())
                .displayOrder(req.displayOrder())
                .build();
        return resumeExperienceRepository.save(exp);
    }

    /** Convenience overload used by integration tests (version-number based). */
    @Transactional
    public ResumeExperience updateVersionExperience(User user, UUID resumeId, int versionNumber,
                                                    UUID experienceId, String companyName, String jobTitle) {
        ResumeVersion version = getVersion(user, resumeId, versionNumber);
        ResumeExperience exp = resumeExperienceRepository
                .findByIdAndResumeVersionId(experienceId, version.getId())
                .orElseThrow(DomainExceptions::resumeExperienceNotFound);
        exp.setCompanyName(companyName);
        exp.setJobTitle(jobTitle);
        return resumeExperienceRepository.save(exp);
    }

    @Transactional
    public ResumeExperience updateExperience(User user, UUID resumeId, UUID versionId,
                                             UUID experienceId, ResumeExperienceRequest req) {
        requireOwnedVersion(user, resumeId, versionId);
        validateExperienceDates(req.currentlyWorking(), req.startDate(), req.endDate());
        ResumeExperience exp = resumeExperienceRepository
                .findByIdAndResumeVersionId(experienceId, versionId)
                .orElseThrow(DomainExceptions::resumeExperienceNotFound);
        exp.setCompanyName(req.companyName());
        exp.setJobTitle(req.jobTitle());
        exp.setLocation(req.location());
        exp.setEmploymentType(req.employmentType());
        exp.setStartDate(req.startDate());
        exp.setEndDate(req.endDate());
        exp.setCurrentlyWorking(req.currentlyWorking());
        exp.setDescription(req.description());
        exp.setDisplayOrder(req.displayOrder());
        return resumeExperienceRepository.save(exp);
    }

    @Transactional
    public void deleteExperience(User user, UUID resumeId, UUID versionId, UUID experienceId) {
        requireOwnedVersion(user, resumeId, versionId);
        ResumeExperience exp = resumeExperienceRepository
                .findByIdAndResumeVersionId(experienceId, versionId)
                .orElseThrow(DomainExceptions::resumeExperienceNotFound);
        resumeExperienceRepository.delete(exp);
    }

    // ── Version Education CRUD ────────────────────────────────────────────────

    @Transactional
    public ResumeEducation addEducation(User user, UUID resumeId, UUID versionId,
                                        ResumeEducationRequest req) {
        ResumeVersion version = requireOwnedVersion(user, resumeId, versionId);
        validateEducationDates(req.startDate(), req.endDate());
        ResumeEducation edu = ResumeEducation.builder()
                .resumeVersion(version)
                .institutionName(req.institutionName())
                .degree(req.degree())
                .fieldOfStudy(req.fieldOfStudy())
                .location(req.location())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .grade(req.grade())
                .description(req.description())
                .displayOrder(req.displayOrder())
                .build();
        return resumeEducationRepository.save(edu);
    }

    @Transactional
    public ResumeEducation updateEducation(User user, UUID resumeId, UUID versionId,
                                           UUID educationId, ResumeEducationRequest req) {
        requireOwnedVersion(user, resumeId, versionId);
        validateEducationDates(req.startDate(), req.endDate());
        ResumeEducation edu = resumeEducationRepository
                .findByIdAndResumeVersionId(educationId, versionId)
                .orElseThrow(DomainExceptions::resumeEducationNotFound);
        edu.setInstitutionName(req.institutionName());
        edu.setDegree(req.degree());
        edu.setFieldOfStudy(req.fieldOfStudy());
        edu.setLocation(req.location());
        edu.setStartDate(req.startDate());
        edu.setEndDate(req.endDate());
        edu.setGrade(req.grade());
        edu.setDescription(req.description());
        edu.setDisplayOrder(req.displayOrder());
        return resumeEducationRepository.save(edu);
    }

    @Transactional
    public void deleteEducation(User user, UUID resumeId, UUID versionId, UUID educationId) {
        requireOwnedVersion(user, resumeId, versionId);
        ResumeEducation edu = resumeEducationRepository
                .findByIdAndResumeVersionId(educationId, versionId)
                .orElseThrow(DomainExceptions::resumeEducationNotFound);
        resumeEducationRepository.delete(edu);
    }

    // ── Version Skill CRUD ────────────────────────────────────────────────────

    @Transactional
    public ResumeSkill addSkill(User user, UUID resumeId, UUID versionId,
                                ResumeSkillRequest req) {
        ResumeVersion version = requireOwnedVersion(user, resumeId, versionId);
        ResumeSkill skill = ResumeSkill.builder()
                .resumeVersion(version)
                .name(req.name())
                .category(req.category())
                .proficiency(req.proficiency())
                .displayOrder(req.displayOrder())
                .build();
        return resumeSkillRepository.save(skill);
    }

    @Transactional
    public ResumeSkill updateSkill(User user, UUID resumeId, UUID versionId,
                                   UUID skillId, ResumeSkillRequest req) {
        requireOwnedVersion(user, resumeId, versionId);
        ResumeSkill skill = resumeSkillRepository
                .findByIdAndResumeVersionId(skillId, versionId)
                .orElseThrow(DomainExceptions::resumeSkillNotFound);
        skill.setName(req.name());
        skill.setCategory(req.category());
        skill.setProficiency(req.proficiency());
        skill.setDisplayOrder(req.displayOrder());
        return resumeSkillRepository.save(skill);
    }

    @Transactional
    public void deleteSkill(User user, UUID resumeId, UUID versionId, UUID skillId) {
        requireOwnedVersion(user, resumeId, versionId);
        ResumeSkill skill = resumeSkillRepository
                .findByIdAndResumeVersionId(skillId, versionId)
                .orElseThrow(DomainExceptions::resumeSkillNotFound);
        resumeSkillRepository.delete(skill);
    }

    // ── Latest version number helper (used by controller for list response) ───

    @Transactional(readOnly = true)
    public int getLatestVersionNumber(UUID resumeId) {
        return resumeVersionRepository.findMaxVersionNumber(resumeId).orElse(0);
    }

    // ── AI tailoring clone ────────────────────────────────────────────────────

    /**
     * Creates a new ResumeVersion by cloning the source version and applying
     * accepted AI suggestion overrides to the relevant experiences.
     *
     * The source version is NEVER modified. All child entities are new instances.
     * Does NOT read from MasterProfile — preserves resume-local edits.
     *
     * @param resumeId       the owning resume ID
     * @param source         the source version to clone (already ownership-verified)
     * @param acceptedMap    map of experienceId → suggestedText for accepted suggestions
     * @return the newly persisted ResumeVersion
     */
    @Transactional
    public ResumeVersion cloneVersionWithTailoring(UUID resumeId, ResumeVersion source,
                                                   Map<UUID, String> acceptedMap) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(DomainExceptions::resumeNotFound);

        int nextVersion = resumeVersionRepository.findMaxVersionNumber(resumeId)
                .map(max -> max + 1)
                .orElse(1);

        String newTitle = buildAiTailoredTitle(source.getTitle());

        ResumeVersion newVersion = ResumeVersion.builder()
                .resume(resume)
                .versionNumber(nextVersion)
                .title(newTitle)
                .professionalSummary(source.getProfessionalSummary())
                .build();

        for (ResumeExperience src : source.getExperiences()) {
            String description = acceptedMap.getOrDefault(src.getId(), src.getDescription());
            newVersion.getExperiences().add(ResumeExperience.builder()
                    .resumeVersion(newVersion)
                    .companyName(src.getCompanyName())
                    .jobTitle(src.getJobTitle())
                    .location(src.getLocation())
                    .employmentType(src.getEmploymentType())
                    .startDate(src.getStartDate())
                    .endDate(src.getEndDate())
                    .currentlyWorking(src.isCurrentlyWorking())
                    .description(description)
                    .displayOrder(src.getDisplayOrder())
                    .build());
        }

        for (ResumeEducation src : source.getEducations()) {
            newVersion.getEducations().add(ResumeEducation.builder()
                    .resumeVersion(newVersion)
                    .institutionName(src.getInstitutionName())
                    .degree(src.getDegree())
                    .fieldOfStudy(src.getFieldOfStudy())
                    .location(src.getLocation())
                    .startDate(src.getStartDate())
                    .endDate(src.getEndDate())
                    .grade(src.getGrade())
                    .description(src.getDescription())
                    .displayOrder(src.getDisplayOrder())
                    .build());
        }

        for (ResumeSkill src : source.getSkills()) {
            newVersion.getSkills().add(ResumeSkill.builder()
                    .resumeVersion(newVersion)
                    .name(src.getName())
                    .category(src.getCategory())
                    .proficiency(src.getProficiency())
                    .displayOrder(src.getDisplayOrder())
                    .build());
        }

        return resumeVersionRepository.save(newVersion);
    }

    private String buildAiTailoredTitle(String sourceTitle) {
        String suffix = " — AI Tailored";
        if (sourceTitle == null || sourceTitle.isBlank()) {
            return "AI Tailored";
        }
        if (sourceTitle.endsWith(suffix)) {
            return sourceTitle;
        }
        return sourceTitle + suffix;
    }

    // ── Ownership helpers ─────────────────────────────────────────────────────

    private Resume requireOwnedResume(User user, UUID resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(DomainExceptions::resumeNotFound);
    }

    private ResumeVersion requireOwnedVersion(User user, UUID resumeId, UUID versionId) {
        requireOwnedResume(user, resumeId);
        return resumeVersionRepository.findByIdAndResumeId(versionId, resumeId)
                .orElseThrow(DomainExceptions::resumeVersionNotFound);
    }

    // ── Validation helpers ────────────────────────────────────────────────────

    private void validateExperienceDates(boolean currentlyWorking, LocalDate startDate, LocalDate endDate) {
        if (currentlyWorking && endDate != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "A current position cannot have an end date.");
        }
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "End date cannot be before start date.");
        }
    }

    private void validateEducationDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "End date cannot be before start date.");
        }
    }

    // ── Snapshot helper ───────────────────────────────────────────────────────

    private ResumeVersion createVersion(Resume resume, MasterProfile profile, int versionNumber) {
        ResumeVersion version = ResumeVersion.builder()
                .resume(resume)
                .versionNumber(versionNumber)
                .title(profile.getProfessionalTitle())
                .professionalSummary(profile.getProfessionalSummary())
                .build();

        for (WorkExperience src : workExperienceRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())) {
            version.getExperiences().add(ResumeExperience.builder()
                    .resumeVersion(version)
                    .companyName(src.getCompanyName())
                    .jobTitle(src.getJobTitle())
                    .location(src.getLocation())
                    .employmentType(src.getEmploymentType())
                    .startDate(src.getStartDate())
                    .endDate(src.getEndDate())
                    .currentlyWorking(src.isCurrentlyWorking())
                    .description(src.getDescription())
                    .displayOrder(src.getDisplayOrder())
                    .build());
        }

        for (Education src : educationRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())) {
            version.getEducations().add(ResumeEducation.builder()
                    .resumeVersion(version)
                    .institutionName(src.getInstitutionName())
                    .degree(src.getDegree())
                    .fieldOfStudy(src.getFieldOfStudy())
                    .location(src.getLocation())
                    .startDate(src.getStartDate())
                    .endDate(src.getEndDate())
                    .grade(src.getGrade())
                    .description(src.getDescription())
                    .displayOrder(src.getDisplayOrder())
                    .build());
        }

        for (Skill src : skillRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())) {
            version.getSkills().add(ResumeSkill.builder()
                    .resumeVersion(version)
                    .name(src.getName())
                    .category(src.getCategory())
                    .proficiency(src.getProficiency())
                    .displayOrder(src.getDisplayOrder())
                    .build());
        }

        return resumeVersionRepository.save(version);
    }
}
