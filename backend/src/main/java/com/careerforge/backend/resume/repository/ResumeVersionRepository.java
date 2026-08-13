package com.careerforge.backend.resume.repository;

import com.careerforge.backend.resume.domain.ResumeVersion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {

    @EntityGraph(attributePaths = {"experiences", "educations", "skills"})
    List<ResumeVersion> findByResumeIdOrderByVersionNumberAsc(UUID resumeId);

    @EntityGraph(attributePaths = {"experiences", "educations", "skills"})
    Optional<ResumeVersion> findByResumeIdAndVersionNumber(
            @Param("resumeId") UUID resumeId,
            @Param("versionNumber") int versionNumber);

    @EntityGraph(attributePaths = {"experiences", "educations", "skills"})
    Optional<ResumeVersion> findByIdAndResumeId(UUID id, UUID resumeId);

    @Query("SELECT MAX(v.versionNumber) FROM ResumeVersion v WHERE v.resume.id = :resumeId")
    Optional<Integer> findMaxVersionNumber(@Param("resumeId") UUID resumeId);
}
