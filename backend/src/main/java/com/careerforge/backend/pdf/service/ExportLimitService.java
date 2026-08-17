package com.careerforge.backend.pdf.service;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.billing.SubscriptionService;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.shared.exception.DomainExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExportLimitService {

    static final int FREE_MONTHLY_LIMIT = 3;

    private final PdfExportUsageRepository usageRepository;
    private final SubscriptionService subscriptionService;

    /**
     * Checks the free-tier limit. Throws PDF_EXPORT_LIMIT_EXCEEDED if the user
     * has already consumed all free exports this calendar month.
     * Pro users are always allowed through.
     *
     * Must be called BEFORE PDF generation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkLimit(User user) {
        if (subscriptionService.isPro(user)) return;

        LocalDate period = currentPeriod();
        PdfExportUsage usage = usageRepository
                .findByUserIdAndBillingPeriodForUpdate(user.getId(), period)
                .orElse(null);

        int count = (usage == null) ? 0 : usage.getExportCount();
        if (count >= FREE_MONTHLY_LIMIT) {
            throw DomainExceptions.exportLimitExceeded();
        }
    }

    /**
     * Increments the export count for the current calendar month.
     * Must be called AFTER successful PDF generation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordExport(User user) {
        if (subscriptionService.isPro(user)) return;

        LocalDate period = currentPeriod();
        PdfExportUsage usage = usageRepository
                .findByUserIdAndBillingPeriodForUpdate(user.getId(), period)
                .orElse(null);

        if (usage == null) {
            usage = PdfExportUsage.builder()
                    .user(user)
                    .billingPeriod(period)
                    .exportCount(1)
                    .build();
        } else {
            usage.setExportCount(usage.getExportCount() + 1);
        }
        usageRepository.save(usage);
    }

    private LocalDate currentPeriod() {
        return LocalDate.now().withDayOfMonth(1);
    }
}
