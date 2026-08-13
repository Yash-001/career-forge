package com.careerforge.backend.profile.service;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.profile.domain.*;
import com.careerforge.backend.profile.dto.*;
import com.careerforge.backend.profile.repository.*;
import com.careerforge.backend.shared.exception.ApiException;
import com.careerforge.backend.shared.exception.DomainExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final MasterProfileRepository profileRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;

    // ── Profile ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(User user) {
        MasterProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(DomainExceptions::profileNotFound);
        return toProfileResponse(profile);
    }

    @Transactional
    public ProfileResponse upsertProfile(User user, UpdateProfileRequest request) {
        MasterProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> MasterProfile.builder().user(user).build());

        profile.setPhone(request.phone());
        profile.setLocation(request.location());
        profile.setProfessionalTitle(request.professionalTitle());
        profile.setProfessionalSummary(request.professionalSummary());
        profile.setLinkedinUrl(request.linkedinUrl());
        profile.setGithubUrl(request.githubUrl());
        profile.setPortfolioUrl(request.portfolioUrl());

        return toProfileResponse(profileRepository.save(profile));
    }

    // ── Work Experience ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorkExperienceResponse> getExperiences(User user) {
        MasterProfile profile = requireProfile(user);
        return workExperienceRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())
                .stream().map(this::toWorkExperienceResponse).toList();
    }

    @Transactional
    public WorkExperienceResponse createExperience(User user, CreateWorkExperienceRequest request) {
        MasterProfile profile = requireProfile(user);
        validateExperienceDates(request.currentlyWorking(), request.startDate(), request.endDate());

        WorkExperience exp = WorkExperience.builder()
                .profile(profile)
                .companyName(request.companyName())
                .jobTitle(request.jobTitle())
                .location(request.location())
                .employmentType(request.employmentType())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .currentlyWorking(request.currentlyWorking())
                .description(request.description())
                .displayOrder(request.displayOrder())
                .build();

        return toWorkExperienceResponse(workExperienceRepository.save(exp));
    }

    @Transactional
    public WorkExperienceResponse updateExperience(User user, UUID id, UpdateWorkExperienceRequest request) {
        WorkExperience exp = requireOwnedExperience(user, id);
        validateExperienceDates(request.currentlyWorking(), request.startDate(), request.endDate());

        exp.setCompanyName(request.companyName());
        exp.setJobTitle(request.jobTitle());
        exp.setLocation(request.location());
        exp.setEmploymentType(request.employmentType());
        exp.setStartDate(request.startDate());
        exp.setEndDate(request.endDate());
        exp.setCurrentlyWorking(request.currentlyWorking());
        exp.setDescription(request.description());
        exp.setDisplayOrder(request.displayOrder());

        return toWorkExperienceResponse(workExperienceRepository.save(exp));
    }

    @Transactional
    public void deleteExperience(User user, UUID id) {
        WorkExperience exp = requireOwnedExperience(user, id);
        workExperienceRepository.delete(exp);
    }

    // ── Education ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EducationResponse> getEducations(User user) {
        MasterProfile profile = requireProfile(user);
        return educationRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())
                .stream().map(this::toEducationResponse).toList();
    }

    @Transactional
    public EducationResponse createEducation(User user, CreateEducationRequest request) {
        MasterProfile profile = requireProfile(user);
        validateEducationDates(request.startDate(), request.endDate());

        Education edu = Education.builder()
                .profile(profile)
                .institutionName(request.institutionName())
                .degree(request.degree())
                .fieldOfStudy(request.fieldOfStudy())
                .location(request.location())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .grade(request.grade())
                .description(request.description())
                .displayOrder(request.displayOrder())
                .build();

        return toEducationResponse(educationRepository.save(edu));
    }

    @Transactional
    public EducationResponse updateEducation(User user, UUID id, UpdateEducationRequest request) {
        Education edu = requireOwnedEducation(user, id);
        validateEducationDates(request.startDate(), request.endDate());

        edu.setInstitutionName(request.institutionName());
        edu.setDegree(request.degree());
        edu.setFieldOfStudy(request.fieldOfStudy());
        edu.setLocation(request.location());
        edu.setStartDate(request.startDate());
        edu.setEndDate(request.endDate());
        edu.setGrade(request.grade());
        edu.setDescription(request.description());
        edu.setDisplayOrder(request.displayOrder());

        return toEducationResponse(educationRepository.save(edu));
    }

    @Transactional
    public void deleteEducation(User user, UUID id) {
        Education edu = requireOwnedEducation(user, id);
        educationRepository.delete(edu);
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills(User user) {
        MasterProfile profile = requireProfile(user);
        return skillRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())
                .stream().map(this::toSkillResponse).toList();
    }

    @Transactional
    public SkillResponse createSkill(User user, CreateSkillRequest request) {
        MasterProfile profile = requireProfile(user);

        Skill skill = Skill.builder()
                .profile(profile)
                .name(request.name())
                .category(request.category())
                .proficiency(request.proficiency())
                .displayOrder(request.displayOrder())
                .build();

        return toSkillResponse(skillRepository.save(skill));
    }

    @Transactional
    public SkillResponse updateSkill(User user, UUID id, UpdateSkillRequest request) {
        Skill skill = requireOwnedSkill(user, id);

        skill.setName(request.name());
        skill.setCategory(request.category());
        skill.setProficiency(request.proficiency());
        skill.setDisplayOrder(request.displayOrder());

        return toSkillResponse(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(User user, UUID id) {
        Skill skill = requireOwnedSkill(user, id);
        skillRepository.delete(skill);
    }

    // ── Ownership helpers ─────────────────────────────────────────────────────

    private MasterProfile requireProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(DomainExceptions::profileNotFound);
    }

    private WorkExperience requireOwnedExperience(User user, UUID id) {
        MasterProfile profile = requireProfile(user);
        WorkExperience exp = workExperienceRepository.findById(id)
                .orElseThrow(DomainExceptions::workExperienceNotFound);
        if (!exp.getProfile().getId().equals(profile.getId())) {
            throw DomainExceptions.workExperienceNotFound();
        }
        return exp;
    }

    private Education requireOwnedEducation(User user, UUID id) {
        MasterProfile profile = requireProfile(user);
        Education edu = educationRepository.findById(id)
                .orElseThrow(DomainExceptions::educationNotFound);
        if (!edu.getProfile().getId().equals(profile.getId())) {
            throw DomainExceptions.educationNotFound();
        }
        return edu;
    }

    private Skill requireOwnedSkill(User user, UUID id) {
        MasterProfile profile = requireProfile(user);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(DomainExceptions::skillNotFound);
        if (!skill.getProfile().getId().equals(profile.getId())) {
            throw DomainExceptions.skillNotFound();
        }
        return skill;
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

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ProfileResponse toProfileResponse(MasterProfile p) {
        return new ProfileResponse(p.getId(), p.getPhone(), p.getLocation(),
                p.getProfessionalTitle(), p.getProfessionalSummary(),
                p.getLinkedinUrl(), p.getGithubUrl(), p.getPortfolioUrl(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private WorkExperienceResponse toWorkExperienceResponse(WorkExperience e) {
        return new WorkExperienceResponse(e.getId(), e.getCompanyName(), e.getJobTitle(),
                e.getLocation(), e.getEmploymentType(), e.getStartDate(), e.getEndDate(),
                e.isCurrentlyWorking(), e.getDescription(), e.getDisplayOrder(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private EducationResponse toEducationResponse(Education e) {
        return new EducationResponse(e.getId(), e.getInstitutionName(), e.getDegree(),
                e.getFieldOfStudy(), e.getLocation(), e.getStartDate(), e.getEndDate(),
                e.getGrade(), e.getDescription(), e.getDisplayOrder(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private SkillResponse toSkillResponse(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.getCategory(),
                s.getProficiency(), s.getDisplayOrder(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
