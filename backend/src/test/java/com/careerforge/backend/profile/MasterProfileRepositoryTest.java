package com.careerforge.backend.profile;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.domain.*;
import com.careerforge.backend.profile.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class MasterProfileRepositoryTest extends AbstractIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired WorkExperienceRepository workExperienceRepository;
    @Autowired EducationRepository educationRepository;
    @Autowired SkillRepository skillRepository;

    @BeforeEach
    void clean() {
        skillRepository.deleteAll();
        educationRepository.deleteAll();
        workExperienceRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── MasterProfile ─────────────────────────────────────────────────────────

    @Test
    void masterProfile_canBePersistedForUser() {
        User user = savedUser("profile@example.com");
        MasterProfile profile = profileRepository.save(MasterProfile.builder()
                .user(user)
                .professionalTitle("Software Engineer")
                .build());

        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getUpdatedAt()).isNotNull();
    }

    @Test
    void masterProfile_canBeRetrievedByUserId() {
        User user = savedUser("lookup@example.com");
        profileRepository.save(MasterProfile.builder().user(user).build());

        assertThat(profileRepository.findByUserId(user.getId())).isPresent();
        assertThat(profileRepository.existsByUserId(user.getId())).isTrue();
    }

    @Test
    void masterProfile_userIdIsUnique_secondProfileFails() {
        User user = savedUser("unique@example.com");
        profileRepository.save(MasterProfile.builder().user(user).build());

        assertThatThrownBy(() ->
                profileRepository.saveAndFlush(MasterProfile.builder().user(user).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void masterProfile_deletedWhenUserDeleted() {
        User user = savedUser("cascade@example.com");
        profileRepository.save(MasterProfile.builder().user(user).build());

        userRepository.delete(user);

        assertThat(profileRepository.existsByUserId(user.getId())).isFalse();
    }

    // ── WorkExperience ────────────────────────────────────────────────────────

    @Test
    void workExperience_canBePersistedUnderProfile() {
        MasterProfile profile = savedProfile("work@example.com");
        WorkExperience exp = workExperienceRepository.save(WorkExperience.builder()
                .profile(profile)
                .companyName("Acme Corp")
                .jobTitle("Engineer")
                .startDate(LocalDate.of(2020, 1, 1))
                .currentlyWorking(true)
                .build());

        assertThat(exp.getId()).isNotNull();
        assertThat(exp.getCreatedAt()).isNotNull();
    }

    @Test
    void workExperience_displayOrderIsPreserved() {
        MasterProfile profile = savedProfile("order@example.com");
        workExperienceRepository.save(WorkExperience.builder()
                .profile(profile).companyName("B").jobTitle("Dev")
                .startDate(LocalDate.of(2019, 1, 1)).displayOrder(2).build());
        workExperienceRepository.save(WorkExperience.builder()
                .profile(profile).companyName("A").jobTitle("Dev")
                .startDate(LocalDate.of(2021, 1, 1)).displayOrder(1).build());

        List<WorkExperience> results =
                workExperienceRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId());

        assertThat(results).extracting(WorkExperience::getCompanyName)
                .containsExactly("A", "B");
    }

    @Test
    void workExperience_currentlyWorkingWithEndDate_throwsOnPersist() {
        MasterProfile profile = savedProfile("invalid@example.com");

        assertThatThrownBy(() ->
                workExperienceRepository.saveAndFlush(WorkExperience.builder()
                        .profile(profile)
                        .companyName("Corp")
                        .jobTitle("Dev")
                        .startDate(LocalDate.of(2020, 1, 1))
                        .endDate(LocalDate.of(2022, 1, 1))
                        .currentlyWorking(true)
                        .build()))
                .hasMessageContaining("end date");
    }

    @Test
    void workExperience_endDateBeforeStartDate_throwsOnPersist() {
        MasterProfile profile = savedProfile("dates@example.com");

        assertThatThrownBy(() ->
                workExperienceRepository.saveAndFlush(WorkExperience.builder()
                        .profile(profile)
                        .companyName("Corp")
                        .jobTitle("Dev")
                        .startDate(LocalDate.of(2022, 1, 1))
                        .endDate(LocalDate.of(2020, 1, 1))
                        .build()))
                .hasMessageContaining("start date");
    }

    @Test
    void workExperience_deletedWhenProfileDeleted() {
        MasterProfile profile = savedProfile("wexp-cascade@example.com");
        workExperienceRepository.save(WorkExperience.builder()
                .profile(profile).companyName("Corp").jobTitle("Dev")
                .startDate(LocalDate.of(2020, 1, 1)).build());

        profileRepository.delete(profile);

        assertThat(workExperienceRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())).isEmpty();
    }

    // ── Education ─────────────────────────────────────────────────────────────

    @Test
    void education_canBePersistedUnderProfile() {
        MasterProfile profile = savedProfile("edu@example.com");
        Education edu = educationRepository.save(Education.builder()
                .profile(profile)
                .institutionName("MIT")
                .degree("B.Sc. Computer Science")
                .startDate(LocalDate.of(2016, 9, 1))
                .endDate(LocalDate.of(2020, 6, 1))
                .build());

        assertThat(edu.getId()).isNotNull();
        assertThat(edu.getCreatedAt()).isNotNull();
    }

    @Test
    void education_displayOrderIsPreserved() {
        MasterProfile profile = savedProfile("edu-order@example.com");
        educationRepository.save(Education.builder()
                .profile(profile).institutionName("Harvard").displayOrder(2).build());
        educationRepository.save(Education.builder()
                .profile(profile).institutionName("MIT").displayOrder(1).build());

        List<Education> results =
                educationRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId());

        assertThat(results).extracting(Education::getInstitutionName)
                .containsExactly("MIT", "Harvard");
    }

    @Test
    void education_endDateBeforeStartDate_throwsOnPersist() {
        MasterProfile profile = savedProfile("edu-dates@example.com");

        assertThatThrownBy(() ->
                educationRepository.saveAndFlush(Education.builder()
                        .profile(profile)
                        .institutionName("MIT")
                        .startDate(LocalDate.of(2022, 1, 1))
                        .endDate(LocalDate.of(2020, 1, 1))
                        .build()))
                .hasMessageContaining("start date");
    }

    // ── Skill ─────────────────────────────────────────────────────────────────

    @Test
    void skill_canBePersistedUnderProfile() {
        MasterProfile profile = savedProfile("skill@example.com");
        Skill skill = skillRepository.save(Skill.builder()
                .profile(profile)
                .name("Java")
                .proficiency(ProficiencyLevel.ADVANCED)
                .category("Backend")
                .build());

        assertThat(skill.getId()).isNotNull();
        assertThat(skill.getCreatedAt()).isNotNull();
    }

    @Test
    void skill_displayOrderIsPreserved() {
        MasterProfile profile = savedProfile("skill-order@example.com");
        skillRepository.save(Skill.builder().profile(profile).name("Docker").displayOrder(2).build());
        skillRepository.save(Skill.builder().profile(profile).name("Java").displayOrder(1).build());

        List<Skill> results = skillRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId());

        assertThat(results).extracting(Skill::getName)
                .containsExactly("Java", "Docker");
    }

    @Test
    void skill_deletedWhenProfileDeleted() {
        MasterProfile profile = savedProfile("skill-cascade@example.com");
        skillRepository.save(Skill.builder().profile(profile).name("Java").build());

        profileRepository.delete(profile);

        assertThat(skillRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User savedUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("$2a$10$irrelevant")
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());
    }

    private MasterProfile savedProfile(String email) {
        return profileRepository.save(
                MasterProfile.builder().user(savedUser(email)).build());
    }
}
