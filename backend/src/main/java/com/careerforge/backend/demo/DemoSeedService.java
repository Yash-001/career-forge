package com.careerforge.backend.demo;

import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.billing.BillingProvider;
import com.careerforge.backend.billing.Subscription;
import com.careerforge.backend.billing.SubscriptionRepository;
import com.careerforge.backend.billing.SubscriptionStatus;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.profile.domain.*;
import com.careerforge.backend.profile.repository.*;
import com.careerforge.backend.resume.domain.*;
import com.careerforge.backend.resume.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Idempotent demo seed service.
 * Only runs when app.demo.mode=true.
 * Guards: skips entirely if the demo user already exists (idempotent).
 * Never touches existing non-demo users.
 */
@Service
public class DemoSeedService {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedService.class);

    private final UserRepository userRepository;
    private final MasterProfileRepository profileRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeExperienceRepository resumeExperienceRepository;
    private final ResumeEducationRepository resumeEducationRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final ApplicationRepository applicationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PdfExportUsageRepository pdfExportUsageRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoProperties demoProperties;

    public DemoSeedService(
            UserRepository userRepository,
            MasterProfileRepository profileRepository,
            WorkExperienceRepository workExperienceRepository,
            EducationRepository educationRepository,
            SkillRepository skillRepository,
            ResumeRepository resumeRepository,
            ResumeVersionRepository resumeVersionRepository,
            ResumeExperienceRepository resumeExperienceRepository,
            ResumeEducationRepository resumeEducationRepository,
            ResumeSkillRepository resumeSkillRepository,
            ApplicationRepository applicationRepository,
            SubscriptionRepository subscriptionRepository,
            PdfExportUsageRepository pdfExportUsageRepository,
            PasswordEncoder passwordEncoder,
            DemoProperties demoProperties) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.educationRepository = educationRepository;
        this.skillRepository = skillRepository;
        this.resumeRepository = resumeRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.resumeExperienceRepository = resumeExperienceRepository;
        this.resumeEducationRepository = resumeEducationRepository;
        this.resumeSkillRepository = resumeSkillRepository;
        this.applicationRepository = applicationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pdfExportUsageRepository = pdfExportUsageRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoProperties = demoProperties;
    }

    /**
     * Seeds the demo user and all associated data.
     * Idempotent: if the demo user already exists, this method returns immediately.
     */
    @Transactional
    public void seed() {
        String email = demoProperties.userEmail();

        if (userRepository.existsByEmail(email)) {
            log.info("[Demo] Demo user '{}' already exists — skipping seed.", email);
            return;
        }

        log.info("[Demo] Seeding demo user '{}'...", email);

        User user = seedUser(email);
        MasterProfile profile = seedProfile(user);
        seedWorkExperiences(profile);
        seedEducation(profile);
        seedSkills(profile);
        List<Resume> resumes = seedResumes(user, profile);
        seedApplications(user, resumes);
        seedSubscription(user);
        seedPdfUsage(user);

        log.info("[Demo] Seed complete for '{}'.", email);
    }

    // ── User ─────────────────────────────────────────────────────────────────

    private User seedUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(demoProperties.userPassword()))
                .firstName("Alex")
                .lastName("Rivera")
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());
    }

    // ── Master Profile ────────────────────────────────────────────────────────

    private MasterProfile seedProfile(User user) {
        return profileRepository.save(MasterProfile.builder()
                .user(user)
                .phone("+1 (555) 204-8831")
                .location("Austin, TX")
                .professionalTitle("Software Engineer")
                .professionalSummary(
                        "Full-stack software engineer with 4 years of experience building " +
                        "scalable web applications. Proficient in Java, Spring Boot, Vue 3, " +
                        "and PostgreSQL. Passionate about clean architecture, automated testing, " +
                        "and developer experience.")
                .linkedinUrl("https://linkedin.com/in/alex-rivera-demo")
                .githubUrl("https://github.com/alex-rivera-demo")
                .build());
    }

    // ── Work Experiences ──────────────────────────────────────────────────────

    private void seedWorkExperiences(MasterProfile profile) {
        workExperienceRepository.saveAll(List.of(
                WorkExperience.builder()
                        .profile(profile)
                        .companyName("Meridian Software")
                        .jobTitle("Software Engineer II")
                        .location("Austin, TX")
                        .employmentType(EmploymentType.FULL_TIME)
                        .startDate(LocalDate.of(2022, 3, 1))
                        .currentlyWorking(true)
                        .description(
                                "Designed and implemented REST APIs serving 50k+ daily active users using Spring Boot and PostgreSQL.\n" +
                                "Reduced average API response time by 38% through query optimization and connection pool tuning.\n" +
                                "Led migration of legacy monolith modules to a layered service architecture, improving test coverage from 41% to 87%.\n" +
                                "Mentored two junior engineers through code reviews and pair programming sessions.")
                        .displayOrder(0)
                        .build(),

                WorkExperience.builder()
                        .profile(profile)
                        .companyName("Vantage Analytics")
                        .jobTitle("Junior Software Engineer")
                        .location("Remote")
                        .employmentType(EmploymentType.FULL_TIME)
                        .startDate(LocalDate.of(2020, 7, 1))
                        .endDate(LocalDate.of(2022, 2, 28))
                        .currentlyWorking(false)
                        .description(
                                "Built data pipeline dashboards using Vue 2 and a Python/FastAPI backend, processing 2M+ records daily.\n" +
                                "Implemented automated integration tests that caught 14 production regressions before release.\n" +
                                "Collaborated with product and design to ship 3 major feature releases on schedule.")
                        .displayOrder(1)
                        .build(),

                WorkExperience.builder()
                        .profile(profile)
                        .companyName("Stackline Labs")
                        .jobTitle("Software Engineering Intern")
                        .location("San Francisco, CA")
                        .employmentType(EmploymentType.INTERNSHIP)
                        .startDate(LocalDate.of(2019, 6, 1))
                        .endDate(LocalDate.of(2019, 8, 31))
                        .currentlyWorking(false)
                        .description(
                                "Developed internal tooling for CI/CD pipeline monitoring using React and Node.js.\n" +
                                "Fixed 22 open bugs in the developer portal, reducing support tickets by 30%.")
                        .displayOrder(2)
                        .build()
        ));
    }

    // ── Education ─────────────────────────────────────────────────────────────

    private void seedEducation(MasterProfile profile) {
        educationRepository.saveAll(List.of(
                Education.builder()
                        .profile(profile)
                        .institutionName("University of Texas at Austin")
                        .degree("Bachelor of Science")
                        .fieldOfStudy("Computer Science")
                        .location("Austin, TX")
                        .startDate(LocalDate.of(2016, 9, 1))
                        .endDate(LocalDate.of(2020, 5, 15))
                        .grade("3.7 GPA")
                        .description("Dean's List 2018–2020. Senior capstone: distributed task scheduler in Go.")
                        .displayOrder(0)
                        .build()
        ));
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    private void seedSkills(MasterProfile profile) {
        skillRepository.saveAll(List.of(
                skill(profile, "Java",          "Backend",  ProficiencyLevel.EXPERT,       0),
                skill(profile, "Spring Boot",   "Backend",  ProficiencyLevel.EXPERT,       1),
                skill(profile, "PostgreSQL",    "Database", ProficiencyLevel.ADVANCED,     2),
                skill(profile, "Vue 3",         "Frontend", ProficiencyLevel.ADVANCED,     3),
                skill(profile, "TypeScript",    "Frontend", ProficiencyLevel.ADVANCED,     4),
                skill(profile, "Docker",        "DevOps",   ProficiencyLevel.INTERMEDIATE, 5),
                skill(profile, "Python",        "Backend",  ProficiencyLevel.INTERMEDIATE, 6),
                skill(profile, "Git",           "Tools",    ProficiencyLevel.EXPERT,       7),
                skill(profile, "REST APIs",     "Backend",  ProficiencyLevel.EXPERT,       8),
                skill(profile, "JUnit 5",       "Testing",  ProficiencyLevel.ADVANCED,     9),
                skill(profile, "Flyway",        "Database", ProficiencyLevel.INTERMEDIATE, 10),
                skill(profile, "Linux",         "DevOps",   ProficiencyLevel.INTERMEDIATE, 11)
        ));
    }

    private Skill skill(MasterProfile profile, String name, String category,
                        ProficiencyLevel level, int order) {
        return Skill.builder()
                .profile(profile)
                .name(name)
                .category(category)
                .proficiency(level)
                .displayOrder(order)
                .build();
    }

    // ── Resumes ───────────────────────────────────────────────────────────────

    private List<Resume> seedResumes(User user, MasterProfile profile) {
        Resume r1 = seedResumeBackend(user, profile);
        Resume r2 = seedResumeFullStack(user, profile);
        return List.of(r1, r2);
    }

    private Resume seedResumeBackend(User user, MasterProfile profile) {
        Resume resume = resumeRepository.save(Resume.builder()
                .user(user)
                .name("Backend Engineer — General")
                .build());

        // Version 1 — base
        ResumeVersion v1 = buildVersion(resume, 1,
                "Backend Engineer",
                "Backend-focused engineer with expertise in Java, Spring Boot, and PostgreSQL. " +
                "Experienced in building high-throughput REST APIs and improving system reliability.");
        v1 = resumeVersionRepository.save(v1);
        addExperiences(v1, profile);
        addEducation(v1, profile);
        addSkills(v1, List.of("Java", "Spring Boot", "PostgreSQL", "REST APIs", "JUnit 5", "Flyway", "Docker", "Git"));

        // Version 2 — AI-tailored for a specific role
        ResumeVersion v2 = buildVersion(resume, 2,
                "Backend Engineer",
                "Results-driven backend engineer with 4 years delivering scalable Java/Spring Boot " +
                "services. Proven track record of performance optimization and test coverage improvement. " +
                "Seeking to bring production-grade engineering practices to a high-growth team.");
        v2 = resumeVersionRepository.save(v2);
        addExperiences(v2, profile);
        addEducation(v2, profile);
        addSkills(v2, List.of("Java", "Spring Boot", "PostgreSQL", "REST APIs", "JUnit 5", "Docker", "Git"));

        return resume;
    }

    private Resume seedResumeFullStack(User user, MasterProfile profile) {
        Resume resume = resumeRepository.save(Resume.builder()
                .user(user)
                .name("Full-Stack Engineer — Startup")
                .build());

        ResumeVersion v1 = buildVersion(resume, 1,
                "Full-Stack Software Engineer",
                "Full-stack engineer comfortable across the entire web stack. " +
                "Builds Vue 3 frontends and Spring Boot backends with a focus on clean APIs and " +
                "maintainable code. Thrives in small, fast-moving teams.");
        v1 = resumeVersionRepository.save(v1);
        addExperiences(v1, profile);
        addEducation(v1, profile);
        addSkills(v1, List.of("Java", "Spring Boot", "Vue 3", "TypeScript", "PostgreSQL", "Docker", "Git", "Python"));

        return resume;
    }

    private ResumeVersion buildVersion(Resume resume, int number, String title, String summary) {
        return ResumeVersion.builder()
                .resume(resume)
                .versionNumber(number)
                .title(title)
                .professionalSummary(summary)
                .build();
    }

    private void addExperiences(ResumeVersion version, MasterProfile profile) {
        List<WorkExperience> exps = workExperienceRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId());
        for (WorkExperience exp : exps) {
            resumeExperienceRepository.save(ResumeExperience.builder()
                    .resumeVersion(version)
                    .companyName(exp.getCompanyName())
                    .jobTitle(exp.getJobTitle())
                    .location(exp.getLocation())
                    .employmentType(exp.getEmploymentType())
                    .startDate(exp.getStartDate())
                    .endDate(exp.getEndDate())
                    .currentlyWorking(exp.isCurrentlyWorking())
                    .description(exp.getDescription())
                    .displayOrder(exp.getDisplayOrder())
                    .build());
        }
    }

    private void addEducation(ResumeVersion version, MasterProfile profile) {
        List<Education> edus = educationRepository.findByProfileIdOrderByDisplayOrderAsc(profile.getId());
        for (Education edu : edus) {
            resumeEducationRepository.save(ResumeEducation.builder()
                    .resumeVersion(version)
                    .institutionName(edu.getInstitutionName())
                    .degree(edu.getDegree())
                    .fieldOfStudy(edu.getFieldOfStudy())
                    .location(edu.getLocation())
                    .startDate(edu.getStartDate())
                    .endDate(edu.getEndDate())
                    .grade(edu.getGrade())
                    .description(edu.getDescription())
                    .displayOrder(edu.getDisplayOrder())
                    .build());
        }
    }

    private void addSkills(ResumeVersion version, List<String> skillNames) {
        int order = 0;
        for (String name : skillNames) {
            resumeSkillRepository.save(ResumeSkill.builder()
                    .resumeVersion(version)
                    .name(name)
                    .displayOrder(order++)
                    .build());
        }
    }

    // ── Applications ──────────────────────────────────────────────────────────

    private void seedApplications(User user, List<Resume> resumes) {
        // Get the latest version of the first resume to link
        ResumeVersion linkedVersion = resumeVersionRepository
                .findTopByResumeIdOrderByVersionNumberDesc(resumes.get(0).getId())
                .orElse(null);

        applicationRepository.saveAll(List.of(
                application(user, "Nexus Systems",      "Senior Backend Engineer",   LocalDate.of(2024, 10, 3),  ApplicationStatus.OFFER,     linkedVersion),
                application(user, "Brightpath Tech",   "Software Engineer II",      LocalDate.of(2024, 10, 8),  ApplicationStatus.INTERVIEW, linkedVersion),
                application(user, "Ironclad Data",     "Backend Engineer",          LocalDate.of(2024, 10, 14), ApplicationStatus.INTERVIEW, linkedVersion),
                application(user, "Cloudveil Inc",     "Full-Stack Engineer",       LocalDate.of(2024, 10, 20), ApplicationStatus.APPLIED,   null),
                application(user, "Orion Platforms",   "Java Engineer",             LocalDate.of(2024, 10, 25), ApplicationStatus.APPLIED,   null),
                application(user, "Helix Software",    "Software Engineer",         LocalDate.of(2024, 9, 5),   ApplicationStatus.REJECTED,  linkedVersion),
                application(user, "Cascade Labs",      "Backend Developer",         LocalDate.of(2024, 9, 12),  ApplicationStatus.REJECTED,  linkedVersion),
                application(user, "Stratum Digital",   "API Engineer",              LocalDate.of(2024, 9, 18),  ApplicationStatus.APPLIED,   null),
                application(user, "Vertex Engineering","Full-Stack Developer",      LocalDate.of(2024, 8, 22),  ApplicationStatus.REJECTED,  null),
                application(user, "Pinnacle Cloud",    "Software Engineer",         LocalDate.of(2024, 8, 30),  ApplicationStatus.REJECTED,  null)
        ));
    }

    private Application application(User user, String company, String role,
                                    LocalDate date, ApplicationStatus status,
                                    ResumeVersion version) {
        return Application.builder()
                .user(user)
                .companyName(company)
                .jobTitle(role)
                .applicationDate(date)
                .status(status)
                .resumeVersion(version)
                .build();
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    private void seedSubscription(User user) {
        subscriptionRepository.save(Subscription.builder()
                .user(user)
                .tier(SubscriptionTier.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .provider(BillingProvider.DEMO)
                .build());
    }

    // ── PDF Export Usage ──────────────────────────────────────────────────────

    private void seedPdfUsage(User user) {
        LocalDate currentPeriod = LocalDate.now().withDayOfMonth(1);
        pdfExportUsageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(currentPeriod)
                .exportCount(2)
                .build());
    }
}
