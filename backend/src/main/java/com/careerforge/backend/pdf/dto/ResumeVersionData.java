package com.careerforge.backend.pdf.dto;

import java.util.List;

/**
 * Fully resolved representation of a resume version passed to the PDF generator.
 * The generator must not access the database — all data is pre-loaded here.
 */
public record ResumeVersionData(
        String resumeName,
        String versionTitle,
        String fullName,
        String email,
        String phone,
        String location,
        String linkedInUrl,
        String professionalSummary,
        List<ExperienceData> experiences,
        List<EducationData> educations,
        List<SkillData> skills
) {

    public record ExperienceData(
            String jobTitle,
            String companyName,
            String location,
            String startDate,
            String endDate,
            boolean currentlyWorking,
            String description
    ) {}

    public record EducationData(
            String degree,
            String fieldOfStudy,
            String institutionName,
            String location,
            String startDate,
            String endDate,
            String grade
    ) {}

    public record SkillData(
            String name,
            String category,
            String proficiency
    ) {}
}
