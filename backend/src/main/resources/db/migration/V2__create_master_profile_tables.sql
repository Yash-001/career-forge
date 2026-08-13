-- V2__create_master_profile_tables.sql
-- Master Profile domain: profiles, work experience, education, skills.

-- ── Master Profiles ───────────────────────────────────────────────────────────
CREATE TABLE master_profiles (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL,
    phone               VARCHAR(50),
    location            VARCHAR(255),
    professional_title  VARCHAR(255),
    professional_summary TEXT,
    linkedin_url        VARCHAR(500),
    github_url          VARCHAR(500),
    portfolio_url       VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_master_profiles PRIMARY KEY (id),
    CONSTRAINT uq_master_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_master_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_master_profiles_user_id ON master_profiles (user_id);

-- ── Work Experiences ──────────────────────────────────────────────────────────
CREATE TABLE work_experiences (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id       UUID         NOT NULL,
    company_name     VARCHAR(255) NOT NULL,
    job_title        VARCHAR(255) NOT NULL,
    location         VARCHAR(255),
    employment_type  VARCHAR(20),
    start_date       DATE         NOT NULL,
    end_date         DATE,
    currently_working BOOLEAN     NOT NULL DEFAULT FALSE,
    description      TEXT,
    display_order    INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_work_experiences PRIMARY KEY (id),
    CONSTRAINT fk_work_experiences_profile FOREIGN KEY (profile_id) REFERENCES master_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_work_experiences_employment_type CHECK (
        employment_type IS NULL OR employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','FREELANCE')
    ),
    CONSTRAINT chk_work_experiences_dates CHECK (
        end_date IS NULL OR end_date >= start_date
    ),
    CONSTRAINT chk_work_experiences_current CHECK (
        NOT (currently_working = TRUE AND end_date IS NOT NULL)
    )
);

CREATE INDEX idx_work_experiences_profile_id ON work_experiences (profile_id);
CREATE INDEX idx_work_experiences_order      ON work_experiences (profile_id, display_order);

-- ── Education ─────────────────────────────────────────────────────────────────
CREATE TABLE educations (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id       UUID         NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    degree           VARCHAR(255),
    field_of_study   VARCHAR(255),
    location         VARCHAR(255),
    start_date       DATE,
    end_date         DATE,
    grade            VARCHAR(50),
    description      TEXT,
    display_order    INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_educations PRIMARY KEY (id),
    CONSTRAINT fk_educations_profile FOREIGN KEY (profile_id) REFERENCES master_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_educations_dates CHECK (
        end_date IS NULL OR start_date IS NULL OR end_date >= start_date
    )
);

CREATE INDEX idx_educations_profile_id ON educations (profile_id);
CREATE INDEX idx_educations_order      ON educations (profile_id, display_order);

-- ── Skills ────────────────────────────────────────────────────────────────────
CREATE TABLE skills (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id    UUID         NOT NULL,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(100),
    proficiency   VARCHAR(20),
    display_order INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_skills PRIMARY KEY (id),
    CONSTRAINT fk_skills_profile FOREIGN KEY (profile_id) REFERENCES master_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_skills_proficiency CHECK (
        proficiency IS NULL OR proficiency IN ('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT')
    )
);

CREATE INDEX idx_skills_profile_id ON skills (profile_id);
CREATE INDEX idx_skills_order      ON skills (profile_id, display_order);
