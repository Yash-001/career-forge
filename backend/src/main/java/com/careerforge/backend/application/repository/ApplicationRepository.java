package com.careerforge.backend.application.repository;

import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.dashboard.dto.ApplicationTrendEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @EntityGraph(attributePaths = {"resumeVersion"})
    List<Application> findByUserIdOrderByApplicationDateDesc(UUID userId);

    List<Application> findTop5ByUserIdOrderByApplicationDateDesc(UUID userId);

    List<Application> findByUserIdAndStatusOrderByApplicationDateDesc(UUID userId, ApplicationStatus status);

    @EntityGraph(attributePaths = {"resumeVersion"})
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, ApplicationStatus status);

    @Query("""
            SELECT new com.careerforge.backend.dashboard.dto.ApplicationTrendEntry(
                YEAR(a.applicationDate), MONTH(a.applicationDate), COUNT(a))
            FROM Application a
            WHERE a.user.id = :userId
              AND a.applicationDate >= :since
            GROUP BY YEAR(a.applicationDate), MONTH(a.applicationDate)
            ORDER BY YEAR(a.applicationDate), MONTH(a.applicationDate)
            """)
    List<ApplicationTrendEntry> findMonthlyTrend(
            @Param("userId") UUID userId,
            @Param("since") LocalDate since);

    List<Application> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);
}
