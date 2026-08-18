# CareerForge — Product Requirements Document

---

## 1. Product Vision

CareerForge is a web-based SaaS platform that helps job seekers create, tailor, and manage professional resumes and job applications using AI assistance. The product reduces the friction between a candidate's raw experience and a polished, role-specific application package — without requiring design skills, writing expertise, or hours of manual effort.

The long-term vision is a single workspace where a job seeker manages their entire application lifecycle: from building a master profile, to generating tailored resumes, to tracking application outcomes.

---

## 2. Target Users

- Recent graduates entering the job market for the first time
- Mid-career professionals actively switching roles or industries
- Laid-off workers who need to re-enter the market quickly
- Freelancers building a professional presence for contract work
- Career changers who need to reframe existing experience for a new domain

---

## 3. User Personas

**Persona 1 — The Recent Graduate**
Name: Priya, 23
Situation: Finishing a CS degree, applying to her first full-time roles. Has internship experience but doesn't know how to present it. Overwhelmed by resume formatting and tailoring advice online.
Goals: Create a clean, professional resume fast. Get callbacks.
Pain points: No template looks right. Doesn't know what to emphasize. Applies to 20 jobs with the same resume and hears nothing back.

**Persona 2 — The Mid-Career Switcher**
Name: Marcus, 34
Situation: Five years in operations, wants to move into product management. Has relevant experience but doesn't know how to reframe it.
Goals: Reposition his resume narrative. Apply to PM roles without looking like an outsider.
Pain points: His current resume reads as operations-focused. Tailoring it manually for each job takes 45 minutes per application.

**Persona 3 — The Laid-Off Professional**
Name: Sandra, 41
Situation: Recently laid off from a senior marketing role. Needs to move fast. Last job search was 8 years ago.
Goals: Get back into the market quickly. Apply to many roles efficiently.
Pain points: Her resume is outdated. She doesn't know what modern resumes look like. She needs volume and speed.

---

## 4. Problems Being Solved

1. Job seekers submit generic resumes that don't reflect the specific language of a job posting, reducing ATS pass-through rates.
2. Tailoring a resume manually for each application is time-consuming and inconsistent.
3. Most resume builders produce visually-focused output that doesn't survive ATS parsing.
4. Job seekers have no structured way to track what they applied to, with which resume version, and what happened.
5. There is no single place to maintain a master profile of experience that can be selectively surfaced per application.

---

## 5. Core User Journeys

**Journey 1 — First-Time Setup**
User registers → completes profile (work history, education, skills) → system creates a master profile → user is taken to the dashboard.

**Journey 2 — Resume Creation**
User creates a new resume → selects sections to include from master profile → optionally pastes a job description → AI tailors the content → user reviews and edits → user exports as PDF.

**Journey 3 — Job Application Tracking**
User logs a job application → links a resume version to it → records status (applied, interview, offer, rejected) → updates status as the process progresses → views a summary of all active applications.

**Journey 4 — Subscription Upgrade**
Free user hits resume export limit → prompted to upgrade → selects a plan → completes Stripe checkout (test mode) → limit is lifted → user continues.

**Journey 5 — Returning User**
User logs in → sees dashboard with resume drafts and application pipeline → resumes a draft or creates a new tailored resume.

---

## 6. Zero-Mandatory-Cost Principle

CareerForge is designed so that the complete application can be developed, tested, demonstrated, and deployed without requiring a paid subscription to any third-party service. This is a hard architectural constraint, not a preference.

### Rules

- No MVP feature may permanently depend on a commercial API that requires payment to function.
- Every external integration must have either a free/local implementation or a graceful fallback/demo implementation.
- The application must remain fully usable for core workflows when external providers are unavailable.
- API keys for external services must never be required for the application to boot.
- Free-tier availability of any third-party provider must not be treated as a guaranteed permanent dependency. Provider pricing can change; the architecture must not.
- The application must support a fully local development and demonstration mode with no cloud account required.

### Provider Abstraction Model

All external integrations are accessed exclusively through internal service interfaces. No feature layer code may reference a specific provider implementation directly.

```
AIService
├── DemoAIProvider          (default — deterministic, no API key required)
├── LocalAIProvider         (optional — self-hosted model, e.g. Ollama)
└── ExternalAIProvider      (optional — OpenAI, Anthropic, AWS Bedrock, etc.)

EmailService
├── ConsoleEmailProvider    (default — prints to application log)
└── ExternalEmailProvider   (optional — any SMTP or transactional email provider)

BillingService
├── DemoBillingProvider     (default — simulates Free/Pro upgrade without payment)
└── StripeBillingProvider   (optional — Stripe test mode)
```

The active implementation for each service is selected through environment variable configuration. Changing providers requires no code changes.

---

## 7. Demo Mode

Demo mode is an explicitly enabled runtime configuration that allows CareerForge to be fully demonstrated without any external paid services, cloud accounts, or API credentials.

### What Demo Mode Provides

- DemoAIProvider: deterministic, rule-based bullet point rewriting that exercises the full tailoring workflow
- ConsoleEmailProvider: password reset tokens are printed to the application log instead of sent via email
- DemoBillingProvider: allows simulating a Free-to-Pro upgrade through a UI action without a real payment
- Local PostgreSQL via Docker Compose
- Server-side PDF generation (no external dependency)
- Optional seed data for demonstrating a populated dashboard

### Demo Mode Rules

- Demo mode must be explicitly enabled by setting `CAREERFORGE_DEMO_MODE=true` in the environment. It must never activate automatically.
- Demo mode must never be enabled in a production environment. The application must log a prominent warning and refuse to start if `CAREERFORGE_DEMO_MODE=true` is combined with `CAREERFORGE_ENV=production`.
- Demo mode must never bypass authentication, authorization, or ownership checks. All security rules apply identically.
- Demo mode must never expose real secrets or credentials.
- Demo provider implementations must not contain production credentials.
- The UI must display a visible but unobtrusive indicator when AI tailoring or billing is operating in demo/simulated mode.
- Core business logic must be exercised identically in demo mode and production mode. Demo providers replace only the external communication layer.

---

## 8. MVP Features

### Authentication
- Email and password registration and login
- JWT-based session management
- Password reset via token (email delivery is provider-dependent; token is always generated and stored)
- Basic input validation and error messaging

### Master Profile
- Work experience entries (title, company, dates, description bullets)
- Education entries (institution, degree, dates)
- Skills list (free-form tags)
- Contact information (name, email, phone, location, LinkedIn URL)

### Resume Builder
- Create a named resume from master profile data
- Select which profile sections and entries to include
- Edit content inline within the resume context (does not modify master profile)
- Paste a job description to trigger AI tailoring of bullet points
- AI rewrites selected bullet points to better match the job description language
- Save resume as a versioned draft

### PDF Export
- Export any saved resume to a clean, ATS-friendly single-column PDF
- PDF generation is server-side using an open-source library (no external API required)
- One PDF template in MVP
- Export count tracked per user per billing period

### Job Application Tracker
- Log an application (company, role, date applied, URL optional)
- Link a resume version to the application
- Set and update application status (applied, phone screen, interview, offer, rejected, withdrawn)
- View all applications in a list with status indicators

### Subscription and Billing
- Free tier: 3 PDF exports per month, 2 saved resumes
- Paid tier (Pro): unlimited exports, unlimited saved resumes
- Stripe test mode checkout (when Stripe is configured)
- Demo billing upgrade (when Stripe is not configured)
- Subscription status stored and enforced server-side
- Upgrade prompt shown when free limits are reached

### Dashboard
- Summary of saved resumes (count, last modified)
- Summary of active applications by status
- Quick actions: create resume, log application

---

## 9. Phase-2 Features

- Multiple PDF export templates (modern, creative, minimal)
- Cover letter generation from resume and job description
- AI-generated interview preparation questions based on job description
- LinkedIn profile import via URL parsing or manual paste
- Application notes and contact tracking per application
- Email notifications for application status reminders
- Team or referral accounts
- Resume performance analytics (export count, application outcomes per resume version)
- Public shareable resume link (hosted HTML version)
- Chrome extension to capture job postings directly from job boards

---

## 10. Features Explicitly Excluded from MVP

- OAuth login (Google, LinkedIn, GitHub)
- Multiple resume templates
- Cover letter generation
- Interview prep features
- Browser extension
- Public resume sharing
- Email notifications or reminders
- Team accounts or multi-user workspaces
- Resume import from LinkedIn or existing file
- Mobile-native application
- Real-time collaboration
- Admin dashboard or internal tooling UI
- Redis, Kafka, or any message broker
- Microservices or distributed architecture
- GraphQL

---

## 11. User Roles

**Guest** — Unauthenticated visitor. Can view marketing/landing page only. No access to application features.

**Free User** — Authenticated user on the free tier. Subject to export and resume save limits. Full access to all MVP features within those limits.

**Pro User** — Authenticated user on a paid subscription. No feature limits within MVP scope. Same feature set as Free User with limits removed.

**System (Internal)** — Backend service identity used for scheduled jobs and internal operations. Not a human-facing role.

> Admin role is deferred to Phase 2. No admin UI is in scope for MVP.

---

## 12. Functional Requirements

### Authentication
- FR-AUTH-01: Users must register with a valid email address and a password meeting minimum complexity requirements (8+ characters, at least one number).
- FR-AUTH-02: Passwords must be stored as bcrypt hashes. Plaintext passwords must never be persisted or logged.
- FR-AUTH-03: Login must return a short-lived JWT access token and a longer-lived refresh token.
- FR-AUTH-04: Password reset must generate a hashed, single-use token stored in the database that expires after 1 hour. Token delivery is handled by the active EmailService implementation. In development/demo mode, the ConsoleEmailProvider prints the reset link to the application log.
- FR-AUTH-05: All authenticated API endpoints must reject requests with missing or expired tokens with a 401 response.
- FR-AUTH-06: The password reset flow must function correctly regardless of which EmailService implementation is active. Token generation, storage, validation, and expiry are independent of email delivery.

### Master Profile
- FR-PROF-01: A user may have exactly one master profile.
- FR-PROF-02: Work experience entries must support multiple bullet points per entry.
- FR-PROF-03: All profile fields are optional except the user's display name.
- FR-PROF-04: Profile updates must not affect previously saved resume drafts.

### Resume Builder
- FR-RES-01: A resume is a named snapshot derived from master profile data at creation time.
- FR-RES-02: Edits made within a resume context are local to that resume and do not propagate back to the master profile.
- FR-RES-03: A resume must track which version of each bullet point is active (original vs. AI-tailored).
- FR-RES-04: AI tailoring requires a job description input of at least 50 characters.
- FR-RES-05: The AI tailoring request must be routed through the backend AIService abstraction layer. The frontend must never communicate with an AI provider directly.
- FR-RES-06: Free users may have at most 2 saved resumes. Attempting to create a third must return a clear limit error.
- FR-RES-07: Resume names must be unique per user.
- FR-RES-08: When the active AIService implementation is DemoAIProvider, the tailored output must be clearly labelled as demo/simulated in the API response and in the UI.
- FR-RES-09: AI tailoring must degrade gracefully. If the AIService returns an error or is unavailable, the resume builder must remain functional and the user must receive a clear, specific error message.

### PDF Export
- FR-PDF-01: PDF export must be generated server-side using an open-source Java library (e.g. OpenPDF, iText AGPL, Flying Saucer). No external PDF API may be used.
- FR-PDF-02: The exported PDF must be ATS-compatible (single column, no graphics, machine-readable text).
- FR-PDF-03: Free users are limited to 3 PDF exports per calendar month. The count resets on the first of each month.
- FR-PDF-04: Export count must be enforced server-side. Frontend limit indicators are informational only.

### Job Application Tracker
- FR-TRACK-01: An application entry requires at minimum: company name, role title, and date applied.
- FR-TRACK-02: A resume version may be linked to an application but is not required.
- FR-TRACK-03: Status transitions are not enforced (any status can be set at any time).
- FR-TRACK-04: Deleting a resume version that is linked to an application must not delete the application record. The link must be nullified with a warning shown to the user.

### Billing
- FR-BILL-01: Subscription state (free vs. pro) must be stored and enforced on the backend regardless of which BillingService implementation is active.
- FR-BILL-02: When StripeBillingProvider is active, Stripe webhooks must update subscription state. The backend must not rely solely on frontend-reported payment status.
- FR-BILL-03: When DemoBillingProvider is active, a dedicated API endpoint must allow simulating a Free-to-Pro upgrade. This endpoint must only be accessible when demo mode is enabled.
- FR-BILL-04: Downgrading or canceling a subscription must not immediately delete user data. Limits are re-applied at the next billing cycle boundary.
- FR-BILL-05: The Stripe secret key must never be exposed to the frontend or included in any client-side code.
- FR-BILL-06: The application must start and operate normally when no Stripe credentials are configured. Billing features fall back to DemoBillingProvider.

---

## 13. Non-Functional Requirements

- NFR-01: The backend API must be stateless. Session state must not be stored server-side (JWT-based).
- NFR-02: All API responses must follow a consistent envelope structure (status, data, error).
- NFR-03: The application must be deployable via Docker Compose for local development with a single command.
- NFR-04: Environment-specific configuration (database credentials, API keys, JWT secrets) must be injected via environment variables and never hardcoded.
- NFR-05: The frontend must not contain any secrets, API keys, or credentials of any kind.
- NFR-06: The active AI provider, email provider, and billing provider must each be selectable through environment variable configuration without code changes.
- NFR-07: Database schema changes must be managed through Flyway migrations.
- NFR-08: The application must function correctly without any paid third-party service dependency. AI, email, and billing features fall back to their demo/development implementations when external providers are not configured.
- NFR-09: The application must boot successfully without external AI, email, payment, analytics, or other third-party API credentials present.
- NFR-10: All external integrations must be hidden behind application-level service interfaces. No feature-layer code may reference a provider implementation directly.
- NFR-11: The MVP must support a fully local Docker Compose environment containing the backend, frontend development server, and PostgreSQL. No cloud account is required to run the application locally.
- NFR-12: External service outages must not prevent unrelated CareerForge functionality from operating. An AI provider outage must not affect PDF export, application tracking, or authentication.
- NFR-13: The project must not rely on temporary free trials as architectural dependencies.
- NFR-14: All secrets must be supplied through environment variables or equivalent secure configuration. No secret may be committed to source control.

---

## 14. Security Requirements

- SEC-01: All HTTP traffic must be served over HTTPS in production. HTTP must redirect to HTTPS.
- SEC-02: JWT tokens must be signed with a secret of at least 256 bits. Secrets must be rotatable without requiring a full deployment.
- SEC-03: Refresh tokens must be stored as hashed values in the database, not in plaintext.
- SEC-04: All user-supplied input must be validated server-side before processing. Client-side validation is supplementary only.
- SEC-05: SQL queries must use parameterized statements exclusively. Raw string concatenation in queries is not permitted.
- SEC-06: API endpoints must enforce ownership checks. A user must not be able to read or modify another user's resumes, profile, or applications.
- SEC-07: Stripe webhook endpoints must validate the Stripe signature header before processing any payload.
- SEC-08: CORS policy must be explicitly configured to allow only the known frontend origin(s).
- SEC-09: Sensitive fields (passwords, tokens, API keys) must never appear in application logs.
- SEC-10: Rate limiting must be applied to authentication endpoints (login, register, password reset) to mitigate brute-force attacks.
- SEC-11: Demo mode must never be enabled automatically. It must require explicit opt-in via environment variable and must be blocked in production environments.
- SEC-12: Demo provider implementations must not contain, reference, or log any production credentials.
- SEC-13: External API credentials must never be stored in source control. `.env` files containing secrets must be listed in `.gitignore`.
- SEC-14: Provider abstraction implementations must not leak credentials into logs or API responses.
- SEC-15: The password reset development mechanism (ConsoleEmailProvider) must only be available when the application is not running in production mode.

---

## 15. Performance Requirements

- PERF-01: API responses for standard CRUD operations must complete within 500ms under normal load on the target infrastructure.
- PERF-02: PDF generation must complete within 10 seconds. Requests exceeding this must return a timeout error, not hang indefinitely.
- PERF-03: AI tailoring requests must complete within 30 seconds. The frontend must show a loading state for the duration. This applies to both real and demo AI providers.
- PERF-04: The frontend initial load (first contentful paint) must complete within 3 seconds on a standard broadband connection.
- PERF-05: The system must handle at least 50 concurrent users on the reference deployment infrastructure without degradation. This is the MVP infrastructure ceiling, not a long-term target.

---

## 16. Accessibility Requirements

- ACC-01: The frontend must meet WCAG 2.1 Level AA compliance as a baseline target.
- ACC-02: All interactive elements must be keyboard navigable.
- ACC-03: Form fields must have associated labels. Placeholder text alone is not sufficient.
- ACC-04: Color must not be the sole means of conveying information (e.g., status indicators must include text or icons).
- ACC-05: The application must be usable with a screen reader for core journeys (login, profile edit, resume creation).

---

## 17. Error Handling Requirements

- ERR-01: All API errors must return a structured JSON response with a machine-readable error code and a human-readable message.
- ERR-02: Validation errors must identify the specific field(s) that failed and the reason.
- ERR-03: The frontend must display user-facing error messages for all failed API calls. Silent failures are not acceptable.
- ERR-04: Unhandled server exceptions must return a generic 500 response. Internal stack traces must never be returned to the client.
- ERR-05: AI provider failures must return error code `AI_PROVIDER_UNAVAILABLE`. The frontend must display a meaningful degraded-state message (e.g., "AI tailoring is temporarily unavailable. You can still edit your resume manually.").
- ERR-06: Limit-exceeded errors (resume count, export count) must return error code `LIMIT_EXCEEDED` with a sub-code identifying the specific limit, distinct from generic validation errors, so the frontend can trigger the upgrade prompt.
- ERR-07: Email delivery failures must not surface as user-facing errors during password reset initiation. The API must return a success response regardless of delivery outcome to prevent email enumeration. Delivery failures must be logged server-side.

---

## 18. Analytics Requirements

Analytics are minimal in MVP. No third-party analytics SDK is required.

- ANA-01: The backend must log the following events to the application log: user registration, login, resume created, PDF exported, AI tailoring requested, subscription upgraded.
- ANA-02: Log entries must include a timestamp, user ID (not email), and event type. No PII beyond user ID in event logs.
- ANA-03: Export counts per user per month must be stored in the database for limit enforcement. This data doubles as basic usage analytics.

Phase-2 will introduce a proper analytics pipeline if warranted by user growth.

---

## 19. Testing Requirements

### Provider Abstraction Tests

The test suite must prove that the application remains functional when external providers are unavailable, misconfigured, or replaced.

**AI Provider**
- TEST-AI-01: AI tailoring succeeds when ExternalAIProvider is active and reachable.
- TEST-AI-02: AI tailoring returns `AI_PROVIDER_UNAVAILABLE` when ExternalAIProvider is unreachable.
- TEST-AI-03: Application boots and all non-AI features function when no AI provider is configured.
- TEST-AI-04: DemoAIProvider returns deterministic output and correctly labels results as simulated.

**Email Provider**
- TEST-EMAIL-01: Password reset token is generated and stored correctly regardless of email provider.
- TEST-EMAIL-02: ConsoleEmailProvider logs the reset link without throwing an exception.
- TEST-EMAIL-03: Application boots and authentication functions when no external email provider is configured.

**Billing Provider**
- TEST-BILL-01: Free-to-Pro upgrade succeeds via StripeBillingProvider in test mode.
- TEST-BILL-02: Application boots and billing limits are enforced when Stripe is not configured.
- TEST-BILL-03: DemoBillingProvider correctly transitions a user from Free to Pro.
- TEST-BILL-04: Free user limits (2 resumes, 3 exports/month) are enforced server-side.
- TEST-BILL-05: Pro user limits are not enforced after a successful upgrade.
- TEST-BILL-06: Downgrade re-applies Free limits at the next billing cycle boundary without deleting data.

### General
- TEST-GEN-01: All provider implementations must have unit tests covering the happy path and primary failure modes.
- TEST-GEN-02: Integration tests must cover the full tailoring workflow using DemoAIProvider so the CI pipeline requires no external credentials.

---

## 20. Monetization Model

**Free Tier (default on registration)**
- 2 saved resumes
- 3 PDF exports per calendar month
- Full access to AI tailoring (subject to AI provider availability)
- Full access to job application tracker

**Pro Tier — $9/month (Stripe test mode in MVP)**
- Unlimited saved resumes
- Unlimited PDF exports
- All free tier features

Rationale: The free tier is generous enough to demonstrate value and complete a real job search at low volume. The Pro tier is priced to be an easy decision for an active job seeker. No annual plan in MVP.

**Future monetization options (Phase 2+)**
- Annual billing with discount
- Team plans for career coaches managing multiple clients
- One-time resume review add-on
- White-label licensing for career centers or universities

---

## 21. Future Scalability Considerations

The reference deployment target for MVP is a container-friendly Linux host (Oracle Cloud Always Free is one example; any VPS or container platform is equally valid). The architecture must not create structural blockers to migration.

- The backend is a single Spring Boot JAR deployable as a Docker container. Horizontal scaling requires only adding a load balancer and additional container instances — no architectural changes.
- The database is PostgreSQL. Migration to any managed PostgreSQL service (AWS RDS, Supabase, Neon, Railway) requires only a connection string change.
- The AI abstraction layer means the provider (DemoAIProvider, a self-hosted model via Ollama, OpenAI, Anthropic, AWS Bedrock) can be swapped by changing an environment variable and, if needed, adding a new implementation class.
- PDF generation is server-side, stateless, and uses an open-source library. It can be extracted to a separate service if it becomes a bottleneck.
- Object storage for PDF files (if added in Phase 2) must use an S3-compatible interface so the provider (Oracle Object Storage, AWS S3, Cloudflare R2, MinIO) is interchangeable.
- The frontend is a static build deployable to any CDN or static host. Moving from Cloudflare Pages to another host requires no code changes.
- The BillingService abstraction means switching payment processors requires only a new implementation of that interface.
- The architecture is a modular monolith. Internal boundaries are clean enough to extract individual services in the future if warranted, but no distributed architecture is introduced in MVP.

---

## 22. Infrastructure and Deployment

### Local Development
- Docker Compose brings up the full stack: Spring Boot backend, Vue frontend dev server, and PostgreSQL.
- No cloud account, API key, or external service is required to run the application locally.
- A single `docker compose up` command starts the complete development environment.

### Production Deployment
- The backend is packaged as a Docker image and deployed to any container-capable host.
- The frontend is built as a static artifact and deployed to any CDN or static hosting provider.
- PostgreSQL is the only required infrastructure dependency.
- Oracle Cloud Always Free is the reference deployment target for the portfolio demo but is not an architectural dependency. Any equivalent VPS or container platform is supported.
- The architecture must not use any Oracle-specific APIs, SDKs, or services.
- All environment-specific configuration is injected through environment variables. No configuration is baked into the Docker image.

### Provider-Independent Configuration

| Concern | Default (no config) | Optional override |
|---|---|---|
| AI tailoring | DemoAIProvider | ExternalAIProvider via `AI_PROVIDER_*` env vars |
| Email delivery | ConsoleEmailProvider | ExternalEmailProvider via `EMAIL_PROVIDER_*` env vars |
| Billing | DemoBillingProvider | StripeBillingProvider via `STRIPE_*` env vars |
| Database | Local PostgreSQL (Docker Compose) | Any PostgreSQL via `DATABASE_URL` |
| PDF generation | OpenPDF (bundled) | No override needed |

---

## 23. Portfolio Demonstration Requirements

CareerForge is also a portfolio project intended to demonstrate production-style full-stack SaaS engineering to potential freelance clients. The following capabilities must be demonstrable without requiring the reviewer to configure any paid service or cloud account.

### Capabilities to Demonstrate

| Capability | How Demonstrated |
|---|---|
| Modern responsive UI | Vue 3 + PrimeVue frontend |
| Authentication | JWT login, registration, password reset |
| Secure REST APIs | Spring Security, ownership checks, input validation |
| PostgreSQL | Relational schema with proper normalization |
| Database migrations | Flyway versioned migrations |
| Resume builder | Full create/edit/save workflow |
| AI-assisted tailoring | DemoAIProvider exercises the full workflow without an API key |
| Server-side PDF generation | OpenPDF, ATS-friendly output |
| Job application tracking | Full CRUD with status management |
| Subscription/billing architecture | BillingService abstraction, Free/Pro limits |
| Usage limits | Server-side enforcement, upgrade prompts |
| Error handling | Structured error envelope, graceful degradation |
| Security | HTTPS, CORS, rate limiting, no secrets in frontend |
| Automated tests | Unit and integration tests covering provider failure scenarios |
| Docker | Docker Compose local environment, Dockerized backend |
| Production deployment | Deployed instance on reference infrastructure |
| Clean architecture | Layered monolith: controller → service → domain → persistence |
| Provider abstraction | Swappable AI, email, and billing implementations |
| Graceful degradation | Application remains functional when external providers are unavailable |

### Portfolio Demo Standards

- The deployed demo must be fully functional using only demo/free providers.
- The demo must not require a reviewer to create an account with any paid service.
- The codebase must be publicly readable on GitHub and reflect production-quality standards: no commented-out code, no hardcoded secrets, no tutorial-style shortcuts.
- The README must document how to run the project locally in under five minutes.
- The project must feel like a realistic SaaS product, not a tutorial CRUD application.

---

## 24. MVP Scope — 10 to 14 Day Estimate

This is the minimum buildable product that demonstrates the full value loop: profile → resume → AI tailoring → PDF export → application tracking → billing gate — entirely without paid external services.

### What is in scope

- User registration, login, JWT auth, password reset (ConsoleEmailProvider in dev)
- Master profile CRUD (work experience, education, skills, contact info)
- Resume builder: create from profile, inline editing, save as draft
- AI tailoring: paste job description, rewrite bullet points via AIService (DemoAIProvider default)
- PDF export: server-side via open-source library, single template, ATS-friendly
- Job application tracker: log applications, link resume, update status
- Free tier limits enforced server-side (2 resumes, 3 exports/month)
- Stripe test mode checkout for Pro upgrade (StripeBillingProvider, optional)
- Demo billing upgrade (DemoBillingProvider, active when Stripe not configured)
- Stripe webhook handler for test mode
- Dashboard with resume and application summaries
- Demo mode (CAREERFORGE_DEMO_MODE=true)
- Provider abstraction layer (AIService, EmailService, BillingService)
- Docker Compose for local development (no cloud account required)
- Flyway database migrations
- Consistent API error envelope
- Unit and integration tests covering provider failure scenarios

### What is explicitly deferred

Everything in sections 9 and 10 of this document, plus admin tooling, email notifications, and multiple PDF templates.

### Suggested day-by-day allocation

| Days | Focus |
|------|-------|
| 1–2 | Project scaffolding, Docker Compose, database schema, Flyway migrations, auth endpoints, provider abstraction interfaces |
| 3–4 | Master profile API and frontend profile editor |
| 5–7 | Resume builder API and frontend (create, edit, save, version tracking) |
| 8 | AIService abstraction + DemoAIProvider + optional ExternalAIProvider wiring |
| 9 | PDF export (OpenPDF, server-side, export count enforcement) |
| 10 | Job application tracker (API + frontend list and form) |
| 11 | BillingService abstraction + DemoBillingProvider + optional Stripe wiring + webhook handler |
| 12 | Dashboard, limit UI, upgrade prompts, demo mode flag and UI indicators |
| 13 | Provider failure integration tests, error handling polish |
| 14 | Deployment to reference infrastructure, README, final review |
