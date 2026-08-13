package com.careerforge.backend.resume.repository;

import com.careerforge.backend.resume.domain.ResumeEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeEducationRepository extends JpaRepository<ResumeEducation, UUID> {

    Optional<ResumeEducation> findByIdAndResumeVersionId(UUID id, UUID resumeVersionId);
}
