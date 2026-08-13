package com.careerforge.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TailoringRequest(
        @NotBlank String jobDescription,
        /** Bullets from the resume version, each paired with their ResumeExperience ID. */
        List<BulletWithId> bullets,
        /** Skill names already on the resume version. */
        List<String> resumeSkills
) {
    public TailoringRequest {
        if (bullets == null) bullets = List.of();
        if (resumeSkills == null) resumeSkills = List.of();
    }
}
