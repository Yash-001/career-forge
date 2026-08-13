-- V3__create_resume_tables.sql
-- Resume Builder domain: resumes, versions, and snapshot content.
--
-- Architecture: Resume (identity) → ResumeVersion (snapshot) → content tables.
-- Snapshot content has NO foreign keys to live profile tables.
-- Changes to MasterProfile never affect existing ResumeVersions.

-- ── Resumes ───────────────────────────────────────────────────────────────────
CREATE TABLE resumes (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_resumes PRIMARY KEY (id),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_resumes_user_id ON resumes (user_id);

-- ── Resume Versions ───────────────────────────────────────────────────────────
CREATE TABLE resume_versions (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    resume_id            UUID         NOT NULL,
    version_number       INTEGER      NOT NULL,
    title                VARCHAR(255),
    professional_summary TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_resume_versions PRIMARY KEY (id),
    CONSTRAINT fk_resume_versions_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT uq_resume_versions_number UNIQUE (resume_id, version_number)
);

CREATE INDEX idx_resume_versions_resume_id ON resume_versions (resume_id);

-- ── Resume Experience Snapshot ────────────────────────────────────────────────
CREATE TABLE resume_experiences (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    resume_version_id UUID         NOT NULL,
    company_name      VARCHAR(255) NOT NULL,
    job_title         VARCHAR(255) NOT NULL,
    location          VARCHAR(255),
    employment_type   VARCHAR(20),
    start_date        DATE         NOT NULL,
    end_date          DATE,
    currently_working BOOLEAN      NOT NULL DEFAULT FALSE,
    description       TEXT,
    display_order     INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT pk_resume_experiences PRIMARY KEY (id),
    CONSTRAINT fk_resume_experiences_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id) ON DELETE CASCADE,
    CONSTRAINT chk_resume_experiences_employment_type CHECK (
        employment_type IS NULL OR employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','FREELANCE')
    ),
    CONSTRAINT chk_resume_experiences_dates CHECK (
        end_date IS NULL OR end_date >= start_date
    ),
    CONSTRAINT chk_resume_experiences_current CHECK (
        NOT (currently_working = TRUE AND end_date IS NOT NULL)
    )
);

CREATE INDEX idx_resume_experiences_version_id ON resume_experiences (resume_version_id);
CREATE INDEX idx_resume_experiences_order      ON resume_experiences (resume_version_id, display_order);

-- ── Resume Education Snapshot ─────────────────────────────────────────────────
CREATE TABLE resume_educations (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    resume_version_id UUID         NOT NULL,
    institution_name  VARCHAR(255) NOT NULL,
    degree            VARCHAR(255),
    field_of_study    VARCHAR(255),
    location          VARCHAR(255),
    start_date        DATE,
    end_date          DATE,
    grade             VARCHAR(50),
    description       TEXT,
    display_order     INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT pk_resume_educations PRIMARY KEY (id),
    CONSTRAINT fk_resume_educations_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id) ON DELETE CASCADE,
    CONSTRAINT chk_resume_educations_dates CHECK (
        end_date IS NULL OR start_date IS NULL OR end_date >= start_date
    )
);

CREATE INDEX idx_resume_educations_version_id ON resume_educations (resume_version_id);
CREATE INDEX idx_resume_educations_order      ON resume_educations (resume_version_id, display_order);

-- ── Resume Skill Snapshot ─────────────────────────────────────────────────────
CREATE TABLE resume_skills (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    resume_version_id UUID         NOT NULL,
    name              VARCHAR(100) NOT NULL,
    category          VARCHAR(100),
    proficiency       VARCHAR(20),
    display_order     INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT pk_resume_skills PRIMARY KEY (id),
    CONSTRAINT fk_resume_skills_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id) ON DELETE CASCADE,
    CONSTRAINT chk_resume_skills_proficiency CHECK (
        proficiency IS NULL OR proficiency IN ('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT')
    )
);

CREATE INDEX idx_resume_skills_version_id ON resume_skills (resume_version_id);
CREATE INDEX idx_resume_skills_order      ON resume_skills (resume_version_id, display_order);
