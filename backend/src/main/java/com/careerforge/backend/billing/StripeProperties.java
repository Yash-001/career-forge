package com.careerforge.backend.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Stripe configuration properties.
 * All fields are optional — the application starts without Stripe credentials.
 * StripeBillingProvider validates presence at call time, not at startup.
 */
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /** Stripe secret key (sk_test_... for test mode). */
    private String secretKey;

    /** Stripe webhook signing secret (whsec_...). */
    private String webhookSecret;

    /** Stripe Price ID for the Pro monthly plan (price_...). */
    private String priceProMonthly;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getPriceProMonthly() { return priceProMonthly; }
    public void setPriceProMonthly(String priceProMonthly) { this.priceProMonthly = priceProMonthly; }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank()
                && priceProMonthly != null && !priceProMonthly.isBlank();
    }
}
