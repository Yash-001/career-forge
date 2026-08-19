package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.billing.dto.CheckoutResponse;
import com.careerforge.backend.billing.dto.SubscriptionResponse;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.pdf.service.ExportLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final SubscriptionRepository subscriptionRepository;
    private final PdfExportUsageRepository pdfExportUsageRepository;

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionResponse> getSubscription(@AuthenticationPrincipal User user) {
        ProviderSubscriptionState state = billingService.getStatus(user);

        boolean isFree = SubscriptionTier.FREE.equals(state.tier());
        Integer used = null;
        Integer limit = null;
        if (isFree) {
            LocalDate period = LocalDate.now().withDayOfMonth(1);
            used = pdfExportUsageRepository
                    .findByUserIdAndBillingPeriod(user.getId(), period)
                    .map(u -> u.getExportCount())
                    .orElse(0);
            limit = ExportLimitService.FREE_MONTHLY_LIMIT;
        }

        Subscription sub = subscriptionRepository.findActiveByUserId(user.getId())
                .orElseThrow(com.careerforge.backend.shared.exception.DomainExceptions::noActiveSubscription);

        return ResponseEntity.ok(new SubscriptionResponse(
                state.tier(),
                state.status(),
                sub.getProvider(),
                state.currentPeriodStart(),
                state.currentPeriodEnd(),
                used,
                limit
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal User user) {
        Subscription sub = billingService.upgrade(user);
        return ResponseEntity.ok(new CheckoutResponse(
                "UPGRADED",
                sub.getTier(),
                sub.getStatus(),
                "Your subscription has been upgraded to Pro."
        ));
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(@AuthenticationPrincipal User user) {
        Subscription sub = billingService.cancel(user);
        return ResponseEntity.ok(new SubscriptionResponse(
                sub.getTier(),
                sub.getStatus(),
                sub.getProvider(),
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd(),
                null,
                null
        ));
    }
}
