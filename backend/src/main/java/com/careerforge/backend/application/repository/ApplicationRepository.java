package com.careerforge.backend.application.repository;

import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @EntityGraph(attributePaths = {"resumeVersion"})
    List<Application> findByUserIdOrderByApplicationDateDesc(UUID userId);

    List<Application> findByUserIdAndStatusOrderByApplicationDateDesc(UUID userId, ApplicationStatus status);

    @EntityGraph(attributePaths = {"resumeVersion"})
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, ApplicationStatus status);
}
