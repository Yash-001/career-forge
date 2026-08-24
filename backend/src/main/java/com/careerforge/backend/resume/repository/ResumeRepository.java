package com.careerforge.backend.resume.repository;

import com.careerforge.backend.resume.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Resume> findByIdAndUserId(UUID id, UUID userId);

    List<Resume> findTop5ByUserIdOrderByUpdatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    /**
     * Returns [Resume, maxVersionNumber] pairs for the 5 most recently updated resumes
     * of a user — eliminates the N+1 in DashboardService.buildResumeSummary.
     */
    @Query("""
            SELECT r, COALESCE(MAX(v.versionNumber), 0)
            FROM Resume r
            LEFT JOIN r.versions v
            WHERE r.user.id = :userId
            GROUP BY r
            ORDER BY r.updatedAt DESC
            LIMIT 5
            """)
    List<Object[]> findTop5WithMaxVersionByUserId(@Param("userId") UUID userId);
}
