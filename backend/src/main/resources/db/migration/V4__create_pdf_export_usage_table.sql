-- V4__create_pdf_export_usage_table.sql
-- Tracks per-user PDF export counts per calendar month for billing enforcement.

CREATE TABLE pdf_export_usage (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL,
    billing_period DATE        NOT NULL,   -- first day of the calendar month (YYYY-MM-01)
    export_count   INTEGER     NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_pdf_export_usage PRIMARY KEY (id),
    CONSTRAINT uq_pdf_export_usage_user_period UNIQUE (user_id, billing_period),
    CONSTRAINT fk_pdf_export_usage_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_pdf_export_usage_count CHECK (export_count >= 0)
);

CREATE INDEX idx_pdf_export_usage_user_period ON pdf_export_usage (user_id, billing_period);
