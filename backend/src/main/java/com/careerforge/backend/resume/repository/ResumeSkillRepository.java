package com.careerforge.backend.resume.repository;

import com.careerforge.backend.resume.domain.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, UUID> {

    Optional<ResumeSkill> findByIdAndResumeVersionId(UUID id, UUID resumeVersionId);
}
