package com.careerforge.backend.profile.repository;

import com.careerforge.backend.profile.domain.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkExperienceRepository extends JpaRepository<WorkExperience, UUID> {

    List<WorkExperience> findByProfileIdOrderByDisplayOrderAsc(UUID profileId);
}
