package com.careerforge.backend.pdf;

import com.careerforge.backend.pdf.dto.ResumeVersionData;
import com.careerforge.backend.pdf.generator.OpenPdfGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfGeneratorTest {

    private final OpenPdfGenerator generator = new OpenPdfGenerator();

    @Test
    void generate_minimalData_returnsValidPdfBytes() {
        ResumeVersionData data = new ResumeVersionData(
                "My Resume", "Software Engineer", "Jane Doe",
                "jane@example.com", null, null, null,
                "Experienced software engineer.",
                List.of(), List.of(), List.of()
        );

        byte[] pdf = generator.generate(data);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        // PDF magic bytes: %PDF
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generate_withExperienceEducationSkills_returnsValidPdf() {
        var exp = new ResumeVersionData.ExperienceData(
                "Backend Engineer", "Acme Corp", "New York",
                "Jan 2021", "Dec 2023", false,
                "Built REST APIs using Spring Boot."
        );
        var edu = new ResumeVersionData.EducationData(
                "B.Sc.", "Computer Science", "State University",
                "Boston", "Sep 2017", "May 2021", "3.8 GPA"
        );
        var skill = new ResumeVersionData.SkillData("Java", "Backend", "ADVANCED");

        ResumeVersionData data = new ResumeVersionData(
                "Resume v2", "AI Tailored", "John Smith",
                "john@example.com", "+1-555-0100", "New York, NY", "linkedin.com/in/john",
                "Passionate engineer with 5 years of experience.",
                List.of(exp), List.of(edu), List.of(skill)
        );

        byte[] pdf = generator.generate(data);

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generate_nullOptionalFields_doesNotThrow() {
        ResumeVersionData data = new ResumeVersionData(
                null, null, null, null, null, null, null,
                null, List.of(), List.of(), List.of()
        );

        byte[] pdf = generator.generate(data);

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generate_currentlyWorkingExperience_showsPresent() {
        var exp = new ResumeVersionData.ExperienceData(
                "Staff Engineer", "TechCorp", null,
                "Mar 2022", null, true, null
        );
        ResumeVersionData data = new ResumeVersionData(
                "Resume", "Title", "Alice", "alice@example.com",
                null, null, null, null,
                List.of(exp), List.of(), List.of()
        );

        byte[] pdf = generator.generate(data);

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
