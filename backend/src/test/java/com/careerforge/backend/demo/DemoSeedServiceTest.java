package com.careerforge.backend.demo;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.billing.SubscriptionRepository;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.profile.repository.*;
import com.careerforge.backend.resume.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "app.demo.mode=true",
        "app.demo.user-email=seed-test@careerforge.dev",
        "app.demo.user-password=SeedTest1!",
        "app.env="
})
class DemoSeedServiceTest extends AbstractIntegrationTest {

    @Autowired DemoSeedService seedService;
    @Autowired UserRepository userRepository;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired WorkExperienceRepository workExperienceRepository;
    @Autowired EducationRepository educationRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired ResumeRepository resumeRepository;
    @Autowired ResumeVersionRepository resumeVersionRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired PdfExportUsageRepository pdfExportUsageRepository;

    private static final String DEMO_EMAIL = "seed-test@careerforge.dev";

    @BeforeEach
    void cleanDemoUser() {
        userRepository.findByEmail(DEMO_EMAIL).ifPresent(user -> {
            applicationRepository.findByUserIdOrderByApplicationDateDesc(user.getId())
                    .forEach(a -> applicationRepository.delete(a));
            pdfExportUsageRepository.findTop5ByUserIdOrderByUpdatedAtDesc(user.getId())
                    .forEach(u -> pdfExportUsageRepository.delete(u));
            subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .forEach(s -> subscriptionRepository.delete(s));
            resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .forEach(r -> resumeRepository.delete(r));
            profileRepository.findByUserId(user.getId())
                    .ifPresent(profileRepository::delete);
            userRepository.delete(user);
        });
    }

    @Test
    void seed_createsUserWithCorrectEmail() {
        seedService.seed();
        assertThat(userRepository.existsByEmail(DEMO_EMAIL)).isTrue();
    }

    @Test
    void seed_createsMasterProfile() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        assertThat(profileRepository.existsByUserId(user.getId())).isTrue();
    }

    @Test
    void seed_createsThreeWorkExperiences() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        var profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(workExperienceRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())).hasSize(3);
    }

    @Test
    void seed_createsOneEducationEntry() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        var profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(educationRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())).hasSize(1);
    }

    @Test
    void seed_createsTwelveSkills() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        var profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(skillRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId())).hasSize(12);
    }

    @Test
    void seed_createsTwoResumes() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        assertThat(resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(2);
    }

    @Test
    void seed_firstResumeHasTwoVersions() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        var resumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        // "Backend Engineer — General" has 2 versions
        var backendResume = resumes.stream()
                .filter(r -> r.getName().contains("Backend"))
                .findFirst().orElseThrow();
        assertThat(resumeVersionRepository.findByResumeIdOrderByVersionNumberAsc(backendResume.getId())).hasSize(2);
    }

    @Test
    void seed_createsTenApplications() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        assertThat(applicationRepository.countByUserId(user.getId())).isEqualTo(10);
    }

    @Test
    void seed_applicationsHaveDiverseStatuses() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        var apps = applicationRepository.findByUserIdOrderByApplicationDateDesc(user.getId());
        var statuses = apps.stream().map(a -> a.getStatus().name()).distinct().toList();
        assertThat(statuses).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void seed_createsActiveSubscription() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        assertThat(subscriptionRepository.findActiveByUserId(user.getId())).isPresent();
    }

    @Test
    void seed_createsPdfUsageRecord() {
        seedService.seed();
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        assertThat(pdfExportUsageRepository.findTop5ByUserIdOrderByUpdatedAtDesc(user.getId())).hasSize(1);
    }

    @Test
    void seed_isIdempotent_runTwiceDoesNotDuplicate() {
        seedService.seed();
        seedService.seed(); // second call must be a no-op
        assertThat(userRepository.findAll().stream()
                .filter(u -> DEMO_EMAIL.equals(u.getEmail()))
                .count()).isEqualTo(1);
        var user = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
        assertThat(resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(2);
        assertThat(applicationRepository.countByUserId(user.getId())).isEqualTo(10);
    }

    @Test
    void seed_doesNotAffectOtherUsers() {
        // Count users before
        long before = userRepository.count();
        seedService.seed();
        // Only one new user added
        assertThat(userRepository.count()).isEqualTo(before + 1);
    }
}
