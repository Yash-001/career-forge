-- V5__create_job_applications_table.sql
-- Job Application Tracker: tracks a user's job applications with optional resume version link.

CREATE TABLE job_applications (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL,
    company_name      VARCHAR(255) NOT NULL,
    job_title         VARCHAR(255) NOT NULL,
    application_date  DATE         NOT NULL,
    job_url           VARCHAR(2048),
    resume_version_id UUID,
    status            VARCHAR(20)  NOT NULL DEFAULT 'APPLIED',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_job_applications PRIMARY KEY (id),
    CONSTRAINT fk_job_applications_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_applications_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id) ON DELETE SET NULL,
    CONSTRAINT chk_job_applications_status
        CHECK (status IN ('APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'))
);

CREATE INDEX idx_job_applications_user_id        ON job_applications (user_id);
CREATE INDEX idx_job_applications_status         ON job_applications (status);
CREATE INDEX idx_job_applications_date           ON job_applications (application_date);
CREATE INDEX idx_job_applications_resume_version ON job_applications (resume_version_id);
CREATE INDEX idx_job_applications_user_status    ON job_applications (user_id, status);
CREATE INDEX idx_job_applications_user_date      ON job_applications (user_id, application_date DESC);
