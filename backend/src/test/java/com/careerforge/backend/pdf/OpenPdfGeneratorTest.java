package com.careerforge.backend.pdf;

import com.careerforge.backend.pdf.dto.ResumeVersionData;
import com.careerforge.backend.pdf.generator.OpenPdfGenerator;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OpenPdfGeneratorTest {

    private final OpenPdfGenerator generator = new OpenPdfGenerator();

    // ── Helper: extract all text from all pages ───────────────────────────────

    private String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            sb.append(extractor.getTextFromPage(i));
        }
        reader.close();
        return sb.toString();
    }

    private ResumeVersionData.ExperienceData exp(String title, String company, String start, String end,
                                                  boolean current, String desc) {
        return new ResumeVersionData.ExperienceData(title, company, "New York", start, end, current, desc);
    }

    private ResumeVersionData.EducationData edu(String degree, String field, String institution,
                                                 String start, String end, String grade) {
        return new ResumeVersionData.EducationData(degree, field, institution, "Boston", start, end, grade);
    }

    private ResumeVersionData.SkillData skill(String name) {
        return new ResumeVersionData.SkillData(name, "General", null);
    }

    // ── 1. Basic resume ───────────────────────────────────────────────────────

    @Test
    void basicResume_containsNameContactAndSummary() throws Exception {
        ResumeVersionData data = new ResumeVersionData(
                "My Resume", "Software Engineer", "Jane Doe",
                "jane@example.com", "+1-555-0100", "Austin, TX", "linkedin.com/in/jane",
                "Experienced software engineer with a passion for clean code.",
                List.of(), List.of(), List.of()
        );

        byte[] pdf = generator.generate(data);
        String text = extractText(pdf);

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(text).contains("Jane Doe");
        assertThat(text).contains("jane@example.com");
        assertThat(text).contains("+1-555-0100");
        assertThat(text).contains("Austin, TX");
        assertThat(text).contains("SUMMARY");
        assertThat(text).contains("Experienced software engineer");
    }

    // ── 2. Multiple experiences ───────────────────────────────────────────────

    @Test
    void multipleExperiences_allEntriesRendered() throws Exception {
        ResumeVersionData data = new ResumeVersionData(
                "Resume", "Title", "John Smith", "john@example.com",
                null, null, null, null,
                List.of(
                        exp("Senior Engineer", "Acme Corp", "Jan 2022", "Dec 2023", false,
                                "Led backend platform migration to microservices."),
                        exp("Software Engineer", "Beta Inc", "Jun 2019", "Dec 2021", false,
                                "Built REST APIs and improved test coverage to 90%."),
                        exp("Junior Developer", "Gamma Ltd", "Jan 2018", "May 2019", false,
                                "Maintained legacy Java applications.")
                ),
                List.of(), List.of()
        );

        byte[] pdf = generator.generate(data);
        String text = extractText(pdf);

        assertThat(text).contains("EXPERIENCE");
        assertThat(text).contains("Senior Engineer");
        assertThat(text).contains("Acme Corp");
        assertThat(text).contains("Software Engineer");
        assertThat(text).contains("Beta Inc");
        assertThat(text).contains("Junior Developer");
        assertThat(text).contains("Gamma Ltd");
        assertThat(text).contains("Led backend platform migration");
        assertThat(text).contains("improved test coverage");
    }

    // ── 3. Multiple education entries ─────────────────────────────────────────

    @Test
    void multipleEducationEntries_allEntriesRendered() throws Exception {
        ResumeVersionData data = new ResumeVersionData(
                "Resume", "Title", "Alice Brown", "alice@example.com",
                null, null, null, null,
                List.of(),
                List.of(
                        edu("M.Sc.", "Computer Science", "MIT", "Sep 2019", "May 2021", "4.0 GPA"),
                        edu("B.Sc.", "Mathematics", "State University", "Sep 2015", "May 2019", "3.8 GPA")
                ),
                List.of()
        );

        byte[] pdf = generator.generate(data);
        String text = extractText(pdf);

        assertThat(text).contains("EDUCATION");
        assertThat(text).contains("M.Sc.");
        assertThat(text).contains("Computer Science");
        assertThat(text).contains("MIT");
        assertThat(text).contains("B.Sc.");
        assertThat(text).contains("Mathematics");
        assertThat(text).contains("State University");
    }

    // ── 4. Multiple skills ────────────────────────────────────────────────────

    @Test
    void multipleSkills_allSkillsRendered() throws Exception {
        ResumeVersionData data = new ResumeVersionData(
                "Resume", "Title", "Bob Lee", "bob@example.com",
                null, null, null, null,
                List.of(), List.of(),
                List.of(skill("Java"), skill("Spring Boot"), skill("PostgreSQL"),
                        skill("Docker"), skill("Kubernetes"), skill("TypeScript"))
        );

        byte[] pdf = generator.generate(data);
        String text = extractText(pdf);

        assertThat(text).contains("SKILLS");
        assertThat(text).contains("Java");
        assertThat(text).contains("Spring Boot");
        assertThat(text).contains("PostgreSQL");
        assertThat(text).contains("Docker");
        assertThat(text).contains("Kubernetes");
        assertThat(text).contains("TypeScript");
    }

    // ── 5. Long text wrapping ─────────────────────────────────────────────────

    @Test
    void longDescription_wrapsWithoutThrowingAndTextIsPresent() throws Exception {
        String longDesc = "Architected and delivered a distributed event-driven data pipeline processing "
                + "over 50 million events per day using Apache Kafka, Apache Flink, and AWS S3. "
                + "Reduced end-to-end latency from 4 hours to under 90 seconds by redesigning the "
                + "ingestion layer and introducing parallel consumer groups. Collaborated with data "
                + "science teams to define schema contracts and implemented Avro serialisation with "
                + "a schema registry. Mentored three junior engineers and conducted weekly architecture "
                + "reviews. Drove adoption of infrastructure-as-code using Terraform across the team.";

        ResumeVersionData data = new ResumeVersionData(
                "Resume", "Title", "Carol White", "carol@example.com",
                null, null, null, null,
                List.of(exp("Staff Engineer", "DataCo", "Mar 2020", null, true, longDesc)),
                List.of(), List.of()
        );

        assertThatCode(() -> {
            byte[] pdf = generator.generate(data);
            String text = extractText(pdf);
            assertThat(text).contains("Staff Engineer");
            assertThat(text).contains("Architected and delivered");
            assertThat(text).contains("Terraform");
        }).doesNotThrowAnyException();
    }

    // ── 6. Empty optional fields ──────────────────────────────────────────────

    @Test
    void emptyOptionalFields_noExceptionAndValidPdf() {
        // All nullable fields null — no name, no contact, no summary, no sections
        ResumeVersionData data = new ResumeVersionData(
                null, null, null, null, null, null, null,
                null, List.of(), List.of(), List.of()
        );

        assertThatCode(() -> {
            byte[] pdf = generator.generate(data);
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }).doesNotThrowAnyException();
    }

    @Test
    void emptySections_noOrphanedSectionHeadings() throws Exception {
        // Has name but no experience/education/skills — headings must not appear
        ResumeVersionData data = new ResumeVersionData(
                "Resume", "Title", "Dan Green", "dan@example.com",
                null, null, null, "A brief summary.",
                List.of(), List.of(), List.of()
        );

        byte[] pdf = generator.generate(data);
        String text = extractText(pdf);

        assertThat(text).contains("Dan Green");
        assertThat(text).contains("SUMMARY");
        assertThat(text).doesNotContain("EXPERIENCE");
        assertThat(text).doesNotContain("EDUCATION");
        assertThat(text).doesNotContain("SKILLS");
    }

    // ── 7. Unicode characters ─────────────────────────────────────────────────

    @Test
    void unicodeCharacters_doesNotThrow() {
        // Accented Latin characters within Helvetica's supported range
        ResumeVersionData data = new ResumeVersionData(
                "Resume", "Title", "Ren\u00e9e Dupont", "renee@example.com",
                null, "Paris, France", null,
                "Ing\u00e9nieure logicielle avec exp\u00e9rience en d\u00e9veloppement backend.",
                List.of(exp("D\u00e9veloppeur Senior", "Soci\u00e9t\u00e9 G\u00e9n\u00e9rale",
                        "Jan 2020", "Dec 2023", false,
                        "D\u00e9velopp\u00e9 des APIs RESTful avec Spring Boot et PostgreSQL.")),
                List.of(edu("Dipl\u00f4me d'Ing\u00e9nieur", "Informatique",
                        "\u00c9cole Polytechnique", "Sep 2015", "Jun 2020", null)),
                List.of(skill("Java"), skill("Spring Boot"))
        );

        assertThatCode(() -> {
            byte[] pdf = generator.generate(data);
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }).doesNotThrowAnyException();
    }

    // ── 8. Multiple pages ─────────────────────────────────────────────────────

    @Test
    void multiplePages_pdfSpansMoreThanOnePage() throws Exception {
        String desc = "Designed, implemented, and maintained large-scale distributed systems. "
                + "Worked closely with product and infrastructure teams to deliver high-availability "
                + "services. Conducted code reviews, wrote technical design documents, and drove "
                + "cross-team alignment on API contracts and data models.";

        List<ResumeVersionData.ExperienceData> experiences = List.of(
                exp("Principal Engineer",  "Alpha Corp",   "Jan 2023", null,       true,  desc),
                exp("Staff Engineer",      "Beta Systems", "Jan 2021", "Dec 2022", false, desc),
                exp("Senior Engineer",     "Gamma Tech",   "Jan 2019", "Dec 2020", false, desc),
                exp("Software Engineer",   "Delta Inc",    "Jan 2017", "Dec 2018", false, desc),
                exp("Junior Developer",    "Epsilon Ltd",  "Jun 2015", "Dec 2016", false, desc),
                exp("Intern",              "Zeta Co",      "Jan 2015", "May 2015", false, desc)
        );

        List<ResumeVersionData.EducationData> educations = List.of(
                edu("M.Sc.", "Computer Science",  "MIT",              "Sep 2013", "May 2015", "4.0 GPA"),
                edu("B.Sc.", "Software Engineering", "State University", "Sep 2009", "May 2013", "3.9 GPA")
        );

        List<ResumeVersionData.SkillData> skills = List.of(
                skill("Java"), skill("Kotlin"), skill("Spring Boot"), skill("PostgreSQL"),
                skill("Kafka"), skill("Docker"), skill("Kubernetes"), skill("Terraform"),
                skill("AWS"), skill("TypeScript"), skill("Vue.js"), skill("Redis")
        );

        ResumeVersionData data = new ResumeVersionData(
                "Full Resume", "Principal Engineer", "Eve Johnson",
                "eve@example.com", "+1-555-9999", "San Francisco, CA", "linkedin.com/in/eve",
                "Principal engineer with 10 years of experience building distributed systems at scale.",
                experiences, educations, skills
        );

        byte[] pdf = generator.generate(data);

        PdfReader reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();

        assertThat(pages).isGreaterThan(1);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
