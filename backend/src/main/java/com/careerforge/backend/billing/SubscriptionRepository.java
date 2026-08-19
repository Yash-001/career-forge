package com.careerforge.backend.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** Returns the active subscription for a user, if one exists. */
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE'")
    Optional<Subscription> findActiveByUserId(@Param("userId") UUID userId);

    /** Returns all subscriptions for a user (for lifecycle management). */
    List<Subscription> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);

    Optional<Subscription> findByProviderCustomerId(String providerCustomerId);
}
