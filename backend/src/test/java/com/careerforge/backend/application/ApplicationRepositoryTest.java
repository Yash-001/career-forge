package com.careerforge.backend.application;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.domain.MasterProfile;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.resume.domain.Resume;
import com.careerforge.backend.resume.repository.ResumeRepository;
import com.careerforge.backend.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationRepositoryTest extends AbstractIntegrationTest {

    @Autowired ApplicationRepository applicationRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired ResumeRepository resumeRepository;
    @Autowired ResumeService resumeService;

    @BeforeEach
    void clean() {
        applicationRepository.deleteAll();
        resumeRepository.deleteAll();
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

    private Application buildApplication(User user, String company, String role) {
        return Application.builder()
                .user(user)
                .companyName(company)
                .jobTitle(role)
                .applicationDate(LocalDate.of(2024, 6, 1))
                .status(ApplicationStatus.APPLIED)
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void createApplication_persistsAllFields() {
        User user = createUser("create@example.com");

        Application saved = applicationRepository.save(Application.builder()
                .user(user)
                .companyName("Acme Corp")
                .jobTitle("Backend Engineer")
                .applicationDate(LocalDate.of(2024, 6, 15))
                .jobUrl("https://acme.com/jobs/123")
                .status(ApplicationStatus.APPLIED)
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(saved.getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(saved.getApplicationDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(saved.getJobUrl()).isEqualTo("https://acme.com/jobs/123");
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void application_belongsToCorrectUser() {
        User user = createUser("owner@example.com");
        applicationRepository.save(buildApplication(user, "Corp A", "Engineer"));

        List<Application> results = applicationRepository.findByUserIdOrderByApplicationDateDesc(user.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void application_canReferenceResumeVersion() {
        User user = createUser("withresume@example.com");
        profileRepository.save(MasterProfile.builder().user(user).build());
        Resume resume = resumeService.createFromProfile(user, "My Resume");
        var version = resumeService.getLatestVersion(user, resume.getId());

        Application saved = applicationRepository.save(Application.builder()
                .user(user)
                .companyName("Tech Co")
                .jobTitle("Developer")
                .applicationDate(LocalDate.of(2024, 7, 1))
                .resumeVersion(version)
                .status(ApplicationStatus.APPLIED)
                .build());

        Optional<Application> found = applicationRepository.findByIdAndUserId(saved.getId(), user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getResumeVersion().getId()).isEqualTo(version.getId());
    }

    @Test
    void applicationStatus_persistsCorrectly() {
        User user = createUser("status@example.com");

        Application app = applicationRepository.save(buildApplication(user, "Corp", "Role"));
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPLIED);

        app.setStatus(ApplicationStatus.INTERVIEW);
        applicationRepository.save(app);

        Application updated = applicationRepository.findByIdAndUserId(app.getId(), user.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void differentUsers_haveIsolatedApplications() {
        User userA = createUser("userA@example.com");
        User userB = createUser("userB@example.com");

        applicationRepository.save(buildApplication(userA, "Corp A", "Engineer"));
        applicationRepository.save(buildApplication(userB, "Corp B", "Designer"));

        List<Application> aApps = applicationRepository.findByUserIdOrderByApplicationDateDesc(userA.getId());
        List<Application> bApps = applicationRepository.findByUserIdOrderByApplicationDateDesc(userB.getId());

        assertThat(aApps).hasSize(1);
        assertThat(aApps.get(0).getCompanyName()).isEqualTo("Corp A");
        assertThat(bApps).hasSize(1);
        assertThat(bApps.get(0).getCompanyName()).isEqualTo("Corp B");
    }

    @Test
    void application_canExistWithoutResumeVersion() {
        User user = createUser("noversion@example.com");

        Application saved = applicationRepository.save(Application.builder()
                .user(user)
                .companyName("Startup")
                .jobTitle("Generalist")
                .applicationDate(LocalDate.of(2024, 8, 1))
                .status(ApplicationStatus.APPLIED)
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getResumeVersion()).isNull();
    }

    @Test
    void foreignKey_userDelete_cascadesApplications() {
        User user = createUser("cascade@example.com");
        Application saved = applicationRepository.save(buildApplication(user, "Corp", "Role"));
        UUID appId = saved.getId();

        userRepository.delete(user);

        assertThat(applicationRepository.findById(appId)).isEmpty();
    }

    @Test
    void findByIdAndUserId_crossUserAccess_returnsEmpty() {
        User userA = createUser("crossA@example.com");
        User userB = createUser("crossB@example.com");

        Application app = applicationRepository.save(buildApplication(userA, "Corp", "Role"));

        Optional<Application> result = applicationRepository.findByIdAndUserId(app.getId(), userB.getId());
        assertThat(result).isEmpty();
    }
}
