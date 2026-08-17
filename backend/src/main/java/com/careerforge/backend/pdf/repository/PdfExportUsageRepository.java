package com.careerforge.backend.pdf.repository;

import com.careerforge.backend.pdf.domain.PdfExportUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PdfExportUsageRepository extends JpaRepository<PdfExportUsage, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM PdfExportUsage u WHERE u.user.id = :userId AND u.billingPeriod = :period")
    Optional<PdfExportUsage> findByUserIdAndBillingPeriodForUpdate(
            @Param("userId") UUID userId,
            @Param("period") LocalDate period);

    @Query("SELECT u FROM PdfExportUsage u WHERE u.user.id = :userId AND u.billingPeriod = :period")
    Optional<PdfExportUsage> findByUserIdAndBillingPeriod(
            @Param("userId") UUID userId,
            @Param("period") LocalDate period);
}
