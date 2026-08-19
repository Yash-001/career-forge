package com.careerforge.backend.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BillingConfig {

    @Value("${app.billing.provider:demo}")
    private String providerName;

    /**
     * Selects the active BillingProviderPort implementation based on
     * app.billing.provider property. Defaults to demo.
     * No code changes required to switch providers — only config.
     */
    @Bean
    public BillingProviderPort billingProviderPort(List<BillingProviderPort> providers) {
        return providers.stream()
                .filter(p -> p.getProvider().name().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No BillingProviderPort found for provider: " + providerName));
    }
}
