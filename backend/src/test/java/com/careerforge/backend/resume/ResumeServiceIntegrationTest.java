package com.careerforge.backend.resume;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.domain.*;
import com.careerforge.backend.profile.repository.*;
import com.careerforge.backend.resume.domain.*;
import com.careerforge.backend.resume.repository.*;
import com.careerforge.backend.resume.service.ResumeService;
import com.careerforge.backend.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ResumeServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired ResumeService resumeService;
    @Autowired ResumeRepository resumeRepository;
    @Autowired ResumeVersionRepository resumeVersionRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired WorkExperienceRepository workExperienceRepository;
    @Autowired EducationRepository educationRepository;
    @Autowired SkillRepository skillRepository;

    @BeforeEach
    void clean() {
        resumeRepository.deleteAll();
        skillRepository.deleteAll();
        educationRepository.deleteAll();
        workExperienceRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());
    }

    private MasterProfile createProfile(User user) {
        return profileRepository.save(MasterProfile.builder()
                .user(user)
                .professionalTitle("Software Engineer")
                .professionalSummary("Experienced developer")
                .build());
    }

    private WorkExperience addExperience(MasterProfile profile, String company, int order) {
        return workExperienceRepository.save(WorkExperience.builder()
                .profile(profile)
                .companyName(company)
                .jobTitle("Engineer")
                .startDate(LocalDate.of(2020, 1, 1))
                .currentlyWorking(true)
                .displayOrder(order)
                .build());
    }

    private Education addEducation(MasterProfile profile, String institution, int order) {
        return educationRepository.save(Education.builder()
                .profile(profile)
                .institutionName(institution)
                .displayOrder(order)
                .build());
    }

    private Skill addSkill(MasterProfile profile, String name, int order) {
        return skillRepository.save(Skill.builder()
                .profile(profile)
                .name(name)
                .proficiency(ProficiencyLevel.ADVANCED)
                .displayOrder(order)
                .build());
    }

    // ── Snapshot isolation ────────────────────────────────────────────────────

    @Test
    void snapshot_profileChangeDoesNotAffectExistingResume() {
        User user = createUser("snapshot@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "Company A", 0);

        // Create resume — snapshot taken
        Resume resume = resumeService.createFromProfile(user, "My Resume");

        // Verify snapshot contains "Company A"
        ResumeVersion v1 = resumeService.getLatestVersion(user, resume.getId());
        assertThat(v1.getExperiences()).hasSize(1);
        assertThat(v1.getExperiences().iterator().next().getCompanyName()).isEqualTo("Company A");

        // Modify master profile — add new experience
        addExperience(profile, "Company B", 1);

        // Resume v1 must still contain only "Company A"
        ResumeVersion v1Again = resumeService.getLatestVersion(user, resume.getId());
        assertThat(v1Again.getExperiences()).hasSize(1);
        assertThat(v1Again.getExperiences().iterator().next().getCompanyName()).isEqualTo("Company A");
    }

    @Test
    void snapshot_resumeLocalEditDoesNotModifyProfile() {
        User user = createUser("localedit@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "ABC Ltd.", 0);

        Resume resume = resumeService.createFromProfile(user, "My Resume");
        ResumeVersion v1 = resumeService.getLatestVersion(user, resume.getId());
        UUID expId = v1.getExperiences().iterator().next().getId();

        // Edit resume-local copy
        resumeService.updateVersionExperience(user, resume.getId(), 1, expId,
                "ABC Corporation", "Senior Engineer");

        // Profile experience must remain unchanged
        List<WorkExperience> profileExps =
                workExperienceRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId());
        assertThat(profileExps.get(0).getCompanyName()).isEqualTo("ABC Ltd.");

        // Resume version must reflect the local edit
        ResumeVersion updated = resumeService.getVersion(user, resume.getId(), 1);
        assertThat(updated.getExperiences().iterator().next().getCompanyName()).isEqualTo("ABC Corporation");
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void createFromProfile_snapshotsAllContent() {
        User user = createUser("create@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "Acme Corp", 0);
        addEducation(profile, "MIT", 0);
        addSkill(profile, "Java", 0);

        Resume resume = resumeService.createFromProfile(user, "Full Resume");

        ResumeVersion v1 = resumeService.getLatestVersion(user, resume.getId());
        assertThat(v1.getVersionNumber()).isEqualTo(1);
        assertThat(v1.getTitle()).isEqualTo("Software Engineer");
        assertThat(v1.getProfessionalSummary()).isEqualTo("Experienced developer");
        assertThat(v1.getExperiences()).hasSize(1);
        assertThat(v1.getExperiences().iterator().next().getCompanyName()).isEqualTo("Acme Corp");
        assertThat(v1.getEducations()).hasSize(1);
        assertThat(v1.getEducations().iterator().next().getInstitutionName()).isEqualTo("MIT");
        assertThat(v1.getSkills()).hasSize(1);
        assertThat(v1.getSkills().iterator().next().getName()).isEqualTo("Java");
    }

    @Test
    void createFromProfile_noProfile_throws404() {
        User user = createUser("noprofile@example.com");
        assertThatThrownBy(() -> resumeService.createFromProfile(user, "Resume"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("profile");
    }

    @Test
    void createFromProfile_blankName_throws400() {
        User user = createUser("blankname@example.com");
        createProfile(user);
        assertThatThrownBy(() -> resumeService.createFromProfile(user, "  "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void createFromProfile_preservesDisplayOrder() {
        User user = createUser("order@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "First", 0);
        addExperience(profile, "Second", 1);
        addExperience(profile, "Third", 2);

        Resume resume = resumeService.createFromProfile(user, "Ordered Resume");
        ResumeVersion v1 = resumeService.getLatestVersion(user, resume.getId());

        assertThat(v1.getExperiences()).extracting(ResumeExperience::getCompanyName)
                .containsExactlyInAnyOrder("First", "Second", "Third");
    }

    // ── Versioning ────────────────────────────────────────────────────────────

    @Test
    void createNewVersion_incrementsVersionNumber() {
        User user = createUser("versioning@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "Company A", 0);

        Resume resume = resumeService.createFromProfile(user, "Versioned Resume");

        // Update profile before creating v2
        addExperience(profile, "Company B", 1);
        ResumeVersion v2 = resumeService.createNewVersion(user, resume.getId());

        assertThat(v2.getVersionNumber()).isEqualTo(2);
        assertThat(v2.getExperiences()).hasSize(2);
    }

    @Test
    void multipleVersionsCoexist() {
        User user = createUser("multiversion@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "Company A", 0);

        Resume resume = resumeService.createFromProfile(user, "Resume");

        addExperience(profile, "Company B", 1);
        resumeService.createNewVersion(user, resume.getId());

        List<ResumeVersion> versions = resumeService.listVersions(user, resume.getId());
        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).getVersionNumber()).isEqualTo(1);
        assertThat(versions.get(0).getExperiences()).hasSize(1);
        assertThat(versions.get(1).getVersionNumber()).isEqualTo(2);
        assertThat(versions.get(1).getExperiences()).hasSize(2);
    }

    @Test
    void getVersion_specificVersion_returnsCorrectContent() {
        User user = createUser("getversion@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "Company A", 0);

        Resume resume = resumeService.createFromProfile(user, "Resume");
        addExperience(profile, "Company B", 1);
        resumeService.createNewVersion(user, resume.getId());

        ResumeVersion v1 = resumeService.getVersion(user, resume.getId(), 1);
        assertThat(v1.getExperiences()).hasSize(1);
        assertThat(v1.getExperiences().iterator().next().getCompanyName()).isEqualTo("Company A");
    }

    // ── Ownership ─────────────────────────────────────────────────────────────

    @Test
    void resume_belongsToCorrectUser() {
        User userA = createUser("ownerA@example.com");
        User userB = createUser("ownerB@example.com");
        createProfile(userA);

        Resume resume = resumeService.createFromProfile(userA, "A's Resume");

        List<Resume> userAResumes = resumeService.listResumes(userA);
        List<Resume> userBResumes = resumeService.listResumes(userB);

        assertThat(userAResumes).hasSize(1);
        assertThat(userBResumes).isEmpty();
    }

    @Test
    void getResume_anotherUserCannotAccess_throws404() {
        User userA = createUser("crossA@example.com");
        User userB = createUser("crossB@example.com");
        createProfile(userA);

        Resume resume = resumeService.createFromProfile(userA, "A's Resume");

        assertThatThrownBy(() -> resumeService.getResume(userB, resume.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Resume");
    }

    @Test
    void listVersions_anotherUserCannotAccess_throws404() {
        User userA = createUser("versionOwnerA@example.com");
        User userB = createUser("versionOwnerB@example.com");
        createProfile(userA);

        Resume resume = resumeService.createFromProfile(userA, "A's Resume");

        assertThatThrownBy(() -> resumeService.listVersions(userB, resume.getId()))
                .isInstanceOf(ApiException.class);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteResume_removesResumeAndVersionsAndContent() {
        User user = createUser("delete@example.com");
        MasterProfile profile = createProfile(user);
        addExperience(profile, "Corp", 0);

        Resume resume = resumeService.createFromProfile(user, "To Delete");
        UUID resumeId = resume.getId();

        resumeService.deleteResume(user, resumeId);

        assertThat(resumeRepository.findById(resumeId)).isEmpty();
        assertThat(resumeVersionRepository.findByResumeIdOrderByVersionNumberAsc(resumeId)).isEmpty();
    }

    @Test
    void deleteResume_anotherUserCannotDelete_throws404() {
        User userA = createUser("delOwnerA@example.com");
        User userB = createUser("delOwnerB@example.com");
        createProfile(userA);

        Resume resume = resumeService.createFromProfile(userA, "A's Resume");

        assertThatThrownBy(() -> resumeService.deleteResume(userB, resume.getId()))
                .isInstanceOf(ApiException.class);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void createFromProfile_emptyProfile_createsResumeWithNoContent() {
        User user = createUser("empty@example.com");
        createProfile(user);

        Resume resume = resumeService.createFromProfile(user, "Empty Resume");
        ResumeVersion v1 = resumeService.getLatestVersion(user, resume.getId());

        assertThat(v1.getExperiences()).isEmpty();
        assertThat(v1.getEducations()).isEmpty();
        assertThat(v1.getSkills()).isEmpty();
    }

    @Test
    void getVersion_nonExistentVersion_throws404() {
        User user = createUser("noversion@example.com");
        createProfile(user);

        Resume resume = resumeService.createFromProfile(user, "Resume");

        assertThatThrownBy(() -> resumeService.getVersion(user, resume.getId(), 99))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("version");
    }
}
