package com.careerforge.backend.profile.repository;

import com.careerforge.backend.profile.domain.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EducationRepository extends JpaRepository<Education, UUID> {

    List<Education> findByProfileIdOrderByDisplayOrderAsc(UUID profileId);
}
