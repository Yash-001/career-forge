package com.careerforge.backend.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, UUID> {

    boolean existsByProviderEventId(String providerEventId);
}
