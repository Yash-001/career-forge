package com.careerforge.backend.resume.repository;

import com.careerforge.backend.resume.domain.ResumeExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeExperienceRepository extends JpaRepository<ResumeExperience, UUID> {

    Optional<ResumeExperience> findByIdAndResumeVersionId(UUID id, UUID resumeVersionId);
}
