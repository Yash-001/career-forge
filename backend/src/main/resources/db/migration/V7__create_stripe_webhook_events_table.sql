-- V7__create_stripe_webhook_events_table.sql
-- Deduplication log for processed Stripe webhook events.

CREATE TABLE stripe_webhook_events (
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    provider_event_id VARCHAR(255) NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    processed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_stripe_webhook_events PRIMARY KEY (id),
    CONSTRAINT uq_stripe_webhook_events_provider_event_id UNIQUE (provider_event_id)
);
