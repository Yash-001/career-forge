-- V6__create_subscriptions_table.sql
-- Billing domain: stores subscription state per user.
-- User.subscription_tier remains a denormalized fast-read field on the users table.
-- This table is the authoritative subscription lifecycle record.

CREATE TABLE subscriptions (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL,
    tier                     VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    provider                 VARCHAR(20)  NOT NULL,
    provider_customer_id     VARCHAR(255),
    provider_subscription_id VARCHAR(255),
    current_period_start     TIMESTAMPTZ,
    current_period_end       TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_subscriptions_tier
        CHECK (tier IN ('FREE', 'PRO')),
    CONSTRAINT chk_subscriptions_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'CANCELED', 'PAST_DUE')),
    CONSTRAINT chk_subscriptions_provider
        CHECK (provider IN ('DEMO', 'STRIPE')),
    -- Provider IDs are only unique when non-null (partial unique indexes below)
    CONSTRAINT uq_subscriptions_provider_customer_id
        UNIQUE (provider, provider_customer_id),
    CONSTRAINT uq_subscriptions_provider_subscription_id
        UNIQUE (provider, provider_subscription_id)
);

-- Fast lookup: find active subscription for a user
CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_user_status ON subscriptions (user_id, status);

-- Enforce at most one ACTIVE subscription per user via partial unique index
CREATE UNIQUE INDEX uq_subscriptions_one_active_per_user
    ON subscriptions (user_id)
    WHERE status = 'ACTIVE';
