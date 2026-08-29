# CareerForge — Architecture Overview

---

## System Architecture

CareerForge is a modular monolith. The frontend and backend are independently deployable but developed in the same repository.

```
┌─────────────────────────────────────┐
│           Browser (Vue 3)           │
│  Vue Router · Pinia · PrimeVue      │
└──────────────┬──────────────────────┘
               │ HTTPS / REST (JSON)
               ▼
┌─────────────────────────────────────┐
│       Spring Boot REST API          │
│                                     │
│  ┌──────────┐   ┌────────────────┐  │
│  │Controller│   │  Security      │  │
│  └────┬─────┘   │  (JWT Filter)  │  │
│       │         └────────────────┘  │
│  ┌────▼─────┐                       │
│  │ Service  │                       │
│  └────┬─────┘                       │
│       │                             │
│  ┌────▼──────────┐                  │
│  │  Repository   │                  │
│  │  (Spring JPA) │                  │
│  └────┬──────────┘                  │
└───────┼─────────────────────────────┘
        │
        ▼
┌───────────────┐     ┌──────────────────────┐
│  PostgreSQL   │     │  External Providers  │
│               │     │  (via interfaces)    │
│               │     │  · AIService         │
│               │     │  · EmailService      │
│               │     │  · BillingService    │
└───────────────┘     └──────────────────────┘
```

---

## Backend Package Structure

Packages are organised by business capability, not technical layer.

```
com.careerforge.backend
├── auth/
├── profile/
├── resume/
├── ai/
├── pdf/
│   ├── dto/
│   ├── generator/
│   └── service/
├── application/
├── billing/
├── dashboard/
└── shared/
    ├── config/
    ├── exception/
    ├── dto/
    └── security/
```

Each capability package contains its own controller, service, repository, and DTOs.

---

## Frontend Structure

```
src/
├── api/          Axios client and per-feature API modules
├── assets/       Global styles
├── components/   Shared UI components
├── router/       Route definitions
├── stores/       Pinia stores (one per capability)
└── views/        Page-level components
```

---

## AI Provider Architecture

AI operations are accessed through a provider abstraction. No feature code references a specific AI vendor directly.

```
AIService
   ↓
AIProvider (interface)
   ↓
DemoAIProvider          ← default, no API key, no network
ExternalAIProvider      ← stub, wired in a future phase
```

**DemoAIProvider** is the default provider. It is deterministic and rule-based — it is NOT an LLM. It uses keyword matching against a curated vocabulary to extract technologies, detect roles, and generate tailored bullet suggestions. It requires no API key, no external network call, and no subscription.

**ExternalAIProvider** is a stub class (not a Spring bean). It will be registered and wired in a future phase when an external API key is available.

**Provider selection** is controlled by the `ai.provider` property (default: `demo`). The active bean is resolved in `AIConfig` — controllers and services depend only on the `AIProvider` interface.

**Security contract:** `AIService` always verifies resume ownership via `ResumeService` before invoking the provider. The authenticated user's identity is never overridden by client-supplied values. AI results are suggestions only — no resume or profile entities are mutated.

**AI REST boundary:**

```
POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/analyze
        ↓
AIController  (@AuthenticationPrincipal, @Valid, delegates only)
        ↓
AIService     (ownership check via ResumeService, builds provider request)
        ↓
AIProvider    (interface)
        ↓
DemoAIProvider  (rule-based, no API key, no network)

POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/tailor
        ↓
AIController  (@AuthenticationPrincipal, @Valid, delegates only)
        ↓
AIService     (ownership check via ResumeService, extracts bullets + skills)
        ↓
AIProvider    (interface)
        ↓
DemoAIProvider  (rule-based, deterministic bullet suggestions)

POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/accept-tailoring
        ↓
AIController  (@AuthenticationPrincipal, @Valid, returns 201 ResumeVersionResponse)
        ↓
AIService     (ownership check, validates all experienceIds against source version)
        ↓
ResumeService.cloneVersionWithTailoring  (clones source, applies accepted overrides)
        ↓
New ResumeVersion persisted (source version unchanged)
```

**Accept-tailoring contract:**
- `POST .../accept-tailoring` accepts a list of `AcceptedSuggestion` (experienceId + suggestedText).
- Returns `201 Created` with the new `ResumeVersionResponse`.
- Source version is never modified — all child entities are new instances.
- Clones from the source `ResumeVersion` directly (not MasterProfile) — preserves resume-local edits.
- Version title gets `" — AI Tailored"` suffix; no double-suffix if source already ends with it.
- Returns `422 INVALID_SUGGESTION` if any `experienceId` does not belong to the source version.
- Returns `404` if resume/version not found or belongs to another user.

**Tailoring contract:**
- Requires authentication; unauthenticated requests return 401.
- Ownership is enforced via `AIService` → `ResumeService`; cross-user access returns 404 with no existence leakage.
- `DemoAIProvider` is deterministic: identical resume version + job description always produce identical output.
- Suggestions are not persisted; no resume, version, experience, education, skill, or profile entity is mutated.
- The HTTP DTO (`AITailorRequest`) accepts only `jobDescription`; bullets and skills are populated server-side.

---

## PDF Export Architecture

PDF generation is server-side, stateless, and uses an open-source library. No external PDF API is used.

```
PdfExportService
   ↓
PdfGenerator (interface)
   ↓
OpenPdfGenerator          ← default, OpenPDF (com.github.librepdf:openpdf), no network
```

**Responsibility boundaries:**

| Layer | Responsibility |
|---|---|
| `PdfExportService` | Ownership enforcement (via `ResumeService`), data assembly, billing limit enforcement |
| `PdfGenerator` | Interface — decouples service from the PDF library |
| `OpenPdfGenerator` | Converts `ResumeVersionData` to PDF bytes — no DB, no ownership, no billing |
| `ResumeVersionData` | Flat DTO carrying all render data; fully resolved before the generator is called |

**PDF export contract:**
- `PdfExportService.exportVersion(user, resumeId, versionId)` — verifies ownership via `ResumeService`, assembles `ResumeVersionData`, delegates to `PdfGenerator`, returns `byte[]`.
- `OpenPdfGenerator` must not access the database, determine ownership, enforce billing limits, or modify any entity.
- Output is a single-column, ATS-friendly PDF (no graphics, machine-readable text, standard fonts).
- Free users: 3 exports per calendar month (enforced in `ExportLimitService`, tracked in `pdf_export_usage` table, pessimistic lock for concurrency safety).
- Pro users: unlimited exports.
- `DomainExceptions.exportLimitExceeded()` returns `402 PDF_EXPORT_LIMIT_EXCEEDED` so the frontend can trigger the upgrade prompt.

**PDF REST boundary:**

```
GET /api/v1/resumes/{resumeId}/versions/{versionId}/pdf
        ↓
PdfController  (@AuthenticationPrincipal, delegates only)
        ↓
PdfExportService  (ownership check, export limit check, data assembly)
        ↓
ExportLimitService  (checkLimit / recordExport, REQUIRES_NEW, pessimistic lock)
        ↓
PdfGenerator  (interface)
        ↓
OpenPdfGenerator  (OpenPDF, in-memory byte[], no DB)
```

---

## Job Application Tracker Architecture

Applications are a user-owned resource with an optional soft link to a `ResumeVersion`. The link is nullable by design — deleting a resume preserves application history.

```
ApplicationController  (@AuthenticationPrincipal, delegates only)
        ↓
ApplicationService     (ownership enforcement, resolveOwnedVersion)
        ↓
ApplicationRepository  (@EntityGraph on read queries)
        ↓
job_applications table (FK user_id CASCADE, FK resume_version_id SET NULL)
```

**Ownership pattern:**
- `findByIdAndUserId` returns `Optional.empty()` for both not-found and cross-user — no existence leakage.
- `resolveOwnedVersion(user, versionId)` uses JPQL traversal `v.resume.user.id = :userId` for atomic ownership check without extra round-trips. Returns `null` when `versionId` is null (link is optional).

**Resume version link contract:**
- `resume_version_id` FK uses `ON DELETE SET NULL` — deleting a resume/version nullifies the link but preserves the application record and all its other fields.
- `ApplicationResponse` includes `resumeVersionId`, `resumeVersionTitle`, and `resumeVersionNumber` so the frontend can display version info without a second request.
- `@EntityGraph(attributePaths={"resumeVersion"})` on both read queries (`findByUserIdOrderByApplicationDateDesc`, `findByIdAndUserId`) prevents `LazyInitializationException` when the controller mapper accesses `rv.getTitle()` / `rv.getVersionNumber()` outside the transaction boundary.
- No reverse `@OneToMany applications` collection on `ResumeVersion` or `User` — avoids loading application data when fetching resumes.

**Application REST boundary:**

```
POST   /api/v1/applications              → 201 ApplicationResponse
GET    /api/v1/applications              → 200 List<ApplicationResponse>
GET    /api/v1/applications/{id}         → 200 ApplicationResponse
PUT    /api/v1/applications/{id}         → 200 ApplicationResponse
DELETE /api/v1/applications/{id}         → 204
```

**Status lifecycle:**

```
APPLIED → INTERVIEW → OFFER
                    → REJECTED
```

All four statuses are valid at any point — there is no enforced state machine. Status is stored as `VARCHAR(20)` with a CHECK constraint.

**Frontend architecture:**
- `src/api/application.ts` — typed API module mirroring backend DTOs
- `src/stores/application.ts` — Pinia store; optimistic local updates (unshift on create, splice on edit, filter on delete)
- `src/components/application/StatusBadge.vue` — WCAG-accessible: colored dot + text label (not color alone)
- `src/components/application/ApplicationForm.vue` — create/edit dialog; resume version selector loads user's own versions only; saving state owned by parent via `setSaving`/`setError` expose
- `src/views/ApplicationListView.vue` — client-side filtering (search by company/role, filter by status); backend `findByUserIdAndStatusOrderByApplicationDateDesc` available for future server-side filtering

---

## Billing & Subscription Architecture

Subscription state is stored in a dedicated `subscriptions` table, separate from the `users` table. `User.subscriptionTier` is retained as a denormalized fast-read field and is kept in sync whenever subscription state changes.

```
AuthService.register
        ↓
SubscriptionService.provisionFreeSubscription
        ↓
SubscriptionRepository  (persists FREE/DEMO Subscription record)
        ↓
subscriptions table  (partial unique index: one ACTIVE per user)
```

**Responsibility boundaries:**

| Layer | Responsibility |
|---|---|
| `Subscription` entity | Authoritative subscription lifecycle record |
| `User.subscriptionTier` | Denormalized fast-read field; updated on tier change |
| `SubscriptionService.isPro` | Fast-path check via `User.subscriptionTier` — no extra DB query |
| `DefaultSubscriptionService` | Implements interface; reads from `SubscriptionRepository` |
| `ExportLimitService` | Calls `subscriptionService.isPro(user)` — unchanged |

**Subscription domain model:**
- `tier`: `FREE` or `PRO`
- `status`: `ACTIVE`, `INACTIVE`, `CANCELED`, `PAST_DUE`
- `provider`: `DEMO` or `STRIPE`
- `providerCustomerId` / `providerSubscriptionId`: nullable — only set for external providers
- `currentPeriodStart` / `currentPeriodEnd`: nullable — set by billing provider on activation

**Database constraints:**
- `WHERE status = 'ACTIVE'` partial unique index prevents two active subscriptions per user
- Nullable provider ID columns handle FREE/DEMO users correctly
- `ON DELETE CASCADE` from `users` — subscription deleted when user is deleted
- CHECK constraints on `tier`, `status`, `provider` columns

**Billing provider abstraction:**

```
BillingService  (interface)
        ↓
DefaultBillingService  (owns business logic, state validation, User.subscriptionTier sync)
        ↓
BillingProviderPort  (interface — no SDK types leak into domain)
        ↓
DemoBillingProvider   ← default, no external calls, deterministic
StripeBillingProvider ← Stripe test mode (app.billing.provider=stripe)
```

**Provider selection:** controlled by `app.billing.provider` property (default: `demo`). `BillingConfig` resolves the active `BillingProviderPort` bean at startup. No code changes required to switch providers. `StripeBillingProvider` validates credentials at call time — the application starts without Stripe credentials regardless of which provider is active.

**BillingService contract:**
- `upgrade(User)` — validates active subscription exists, validates not already PRO, delegates to provider, syncs `User.subscriptionTier`
- `cancel(User)` — validates active subscription exists, delegates to provider, syncs `User.subscriptionTier`
- `getStatus(User)` — validates active subscription exists, delegates to provider for current state
- All operations throw `NO_ACTIVE_SUBSCRIPTION` (400) if no active subscription record exists
- `upgrade` throws `ALREADY_PRO` (409) if user is already on PRO tier
- Provider failures surface as `BILLING_PROVIDER_ERROR` (502)

**DemoBillingProvider behavior:**
- `initiateUpgrade` — returns PRO/ACTIVE state with 30-day period, no external call
- `cancelSubscription` — returns FREE/CANCELED state, no external call
- `getSubscriptionState` — reflects current local Subscription record
- Fully deterministic; exercises the same code path as Stripe will

**StripeBillingProvider behavior:**
- All Stripe SDK types (`com.stripe.*`) confined to this class — nothing leaks into the domain
- `resolveOrCreateCustomer` — uses stored `providerCustomerId` → Stripe search by email → create new (idempotent)
- `toState()` is the only Stripe→domain mapping point; status mapping: `active/trialing→ACTIVE`, `canceled→CANCELED`, `past_due/unpaid→PAST_DUE`
- All `StripeException` caught and wrapped as `BILLING_PROVIDER_ERROR` (502)
- Credentials validated at call time via `StripeProperties.isConfigured()` — app starts without them

**Stripe webhook architecture:**

```
POST /api/v1/webhooks/stripe  (permit-all — Stripe authenticates via HMAC-SHA256 signature)
        ↓
StripeWebhookController  (verifies signature, returns 400 on failure)
        ↓
StripeWebhookService.verifyAndParse()  (Webhook.constructEvent with webhookSecret)
        ↓
StripeWebhookService.process()  (@Transactional, idempotency check, event dispatch)
        ↓
StripeWebhookEventRepository  (existsByProviderEventId — deduplication)
        ↓
SubscriptionRepository + UserRepository  (sync local state)
```

**Webhook events handled:**

| Event | Action |
|---|---|
| `customer.subscription.created` | Sync tier, status, period; update `User.subscriptionTier` |
| `customer.subscription.updated` | Sync tier, status, period; update `User.subscriptionTier` |
| `customer.subscription.deleted` | Set CANCELED + FREE; update `User.subscriptionTier` |
| `invoice.payment_failed` | Set PAST_DUE |
| anything else | Recorded in `stripe_webhook_events`, ignored |

**Webhook idempotency:** `stripe_webhook_events` table stores `provider_event_id` with a `UNIQUE` constraint. `existsByProviderEventId` is checked before any processing — the same event ID is never processed twice. The event is recorded after processing, inside the same transaction.

**Billing REST boundary:**

```
GET  /api/v1/billing/subscription   → 200 SubscriptionResponse (tier, status, provider, period, usage)
POST /api/v1/billing/checkout       → 200 CheckoutResponse (upgrade FREE→PRO)
POST /api/v1/billing/cancel         → 200 SubscriptionResponse (cancel active subscription)
POST /api/v1/webhooks/stripe        → 200 / 400 (Stripe webhook, permit-all)
```

All `/api/v1/billing/**` endpoints require JWT authentication. User identity is always taken from `@AuthenticationPrincipal` — never from the request body.

---

## External Provider Summary

All external integrations are accessed through internal service interfaces. The active implementation is selected via environment variable. No feature code references a provider SDK directly.

```
AIService
├── DemoAIProvider        (default — no API key, rule-based)
└── ExternalAIProvider    (stub — wired in a future phase)

EmailService
├── ConsoleEmailProvider  (default — logs to stdout)
└── SmtpEmailProvider     (optional)

BillingProviderPort
├── DemoBillingProvider   (default — no credentials, deterministic)
└── StripeBillingProvider (optional — Stripe test mode)
```

---

## API Conventions

All responses use a consistent envelope:

```json
{ "status": "success", "data": {}, "error": null }
{ "status": "error", "data": null, "error": { "code": "...", "message": "...", "fieldErrors": [] } }
```

Base path: `/api/v1`

---

## Security Principles

- JWT access tokens (short-lived) + refresh tokens (long-lived, hashed in DB)
- All secrets via environment variables — never in source code
- Ownership checks on every user-owned resource (`findByIdAndUserId` pattern — no existence leakage)
- CORS restricted to known frontend origin (comma-separated, whitespace-trimmed)
- Stripe webhook signature verified via HMAC-SHA256 (`Webhook.constructEvent`) — invalid signatures return 400
- Stripe credentials validated at call time, not at startup — app runs without them
- No provider credentials logged at any level
- JWT secret validated at startup: `@PostConstruct` in `SecurityConfig` fails fast in production if secret is < 32 bytes or matches known placeholder prefixes
- `RateLimitFilter` applied before JWT filter on auth endpoints (login, register, forgot-password, reset-password): 20 requests / 60-second window per IP; returns `429 RATE_LIMIT_EXCEEDED`
- Security response headers on all responses: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`
- `ConsoleEmailProvider` blocked at startup when `app.env=production` (SEC-15)
- `CAREERFORGE_DEMO_MODE=true` blocked at startup when `app.env=production`
- Input size limits (`@Size`) on all free-text fields (email ≤ 255, description/summary ≤ 5000)
- `Content-Disposition` header uses RFC 5987 `filename*=UTF-8''<encoded>` to prevent header injection
- `findValidTokens()` JPQL query replaces `findAll()` in password reset — indexed, bounded result set

---

## Dashboard Architecture

The dashboard aggregates data from 6 backend modules into a single authenticated response. All data is scoped to the authenticated user — no cross-user data is ever returned.

**Single-request design:**

```
GET /api/v1/dashboard
        ↓
DashboardController  (@AuthenticationPrincipal, delegates only)
        ↓
DashboardService  (@Transactional(readOnly=true))
        ├── buildProfileSummary     → MasterProfileRepository
        ├── buildResumeSummary      → ResumeRepository (N+1-free JPQL join)
        │                             ResumeVersionRepository (count only)
        ├── buildApplicationSummary → ApplicationRepository (5 bounded queries)
        ├── buildSubscriptionSummary→ SubscriptionService
        ├── buildUsageSummary       → PdfExportUsageRepository
        ├── buildQuickActions       → ResumeRepository (count), SubscriptionService
        ├── AnalyticsService        → ApplicationRepository (4 counts + GROUP BY trend)
        └── ActivityService         → 4 × Top-5 queries, merged + sorted in Java
```

**DashboardSummary response shape:**

| Field | Description |
|---|---|
| `profile` | Completion percent (4-point scoring), field presence flags |
| `resumes` | Count, version count, top-5 recent (name, latest version number, updatedAt) |
| `applications` | Total + per-status counts, top-5 recent (with jobUrl) |
| `subscription` | Tier, status, provider, billing period — no provider secrets |
| `usage` | PDF exports used/limit/atLimit (FREE); zeros (PRO) |
| `quickActions` | `canCreateResume`, `canLogApplication`, `canUpgrade` |
| `analytics` | Pipeline counts + 12-month trend (JPQL GROUP BY year/month) |
| `activity` | Top-10 recent events from 4 sources, sorted descending |

**Performance decisions:**
- `findTop5WithMaxVersionByUserId` — single JPQL `SELECT r, MAX(v.versionNumber) … GROUP BY r LIMIT 5` eliminates the N+1 that would result from calling `findMaxVersionNumber` per resume
- Analytics and activity are embedded in the main response — eliminates 2 extra round-trips on page load
- Separate `GET /api/v1/dashboard/analytics` and `GET /api/v1/dashboard/activity` endpoints are retained for targeted retry on partial failure
- All queries are `readOnly=true` — no write locks acquired during dashboard load

**Activity feed sources:**

| Type | Source | Query |
|---|---|---|
| `RESUME_UPDATED` | `ResumeRepository` | `findTop5ByUserIdOrderByUpdatedAtDesc` |
| `VERSION_CREATED` | `ResumeVersionRepository` | `findTop5ByResumeUserIdOrderByCreatedAtDesc` |
| `APPLICATION_ADDED` | `ApplicationRepository` | `findTop5ByUserIdOrderByCreatedAtDesc` |
| `PDF_EXPORTED` | `PdfExportUsageRepository` | `findTop5ByUserIdOrderByUpdatedAtDesc` |

All 4 queries are bounded (Top-5). Results are merged in Java, sorted by `occurredAt` descending, and limited to 10.

**Security contract:**
- Every sub-query is scoped to `user.getId()` — no cross-user data is possible
- `SubscriptionSummary` omits `providerCustomerId` and `providerSubscriptionId` — internal provider IDs are never exposed to the frontend
- `jobUrl` values are validated client-side with `safeJobUrl()` — only `https:` and `http:` schemes are rendered as links; `javascript:`, `data:`, and malformed URLs are silently dropped

**Frontend architecture:**
- `src/api/dashboard.ts` — typed interfaces mirroring all 8 response sections
- `src/stores/dashboard.ts` — Pinia store; `loadDashboard()` populates `summary`, `analytics`, and `activity` from the single response; `loadAnalytics()` and `loadActivity()` retained for retry buttons
- `src/views/DashboardView.vue` — single `onMounted` call; loading skeleton; error+retry; KPI grid; pipeline bars; analytics section; activity feed; quick actions; subscription card
- `src/components/dashboard/AnalyticsSection.vue` — SVG donut chart (pipeline) + CSS bar chart (trend); zero chart library dependencies
- `src/components/dashboard/RecentActivitySection.vue` — activity feed with relative timestamps (`<time datetime>`), RouterLink labels, icon per event type

**Responsive breakpoints:**

| Breakpoint | Layout change |
|---|---|
| ≤ 1024px | Right column narrows to `minmax(0, 280px)` |
| ≤ 768px | Single-column layout; KPI grid 2-column; analytics charts stack |
| ≤ 600px | Welcome header stacks; profile nudge full-width; pipeline labels narrow |

---

## Rate Limiting Architecture

In-process fixed-window rate limiter applied to authentication endpoints.

```
RateLimitFilter  (OncePerRequestFilter, registered before JwtAuthenticationFilter)
        ↓
RateLimitService  (singleton Spring bean)
        ↓
ConcurrentHashMap<String, AtomicInteger>  (key = "<path>:<client-ip>")
```

**Endpoints protected:** `/api/v1/auth/login`, `/register`, `/forgot-password`, `/reset-password`

**Limits:** 20 requests per 60-second window per IP. Returns `429 RATE_LIMIT_EXCEEDED` JSON when exceeded.

**Client IP resolution:** `X-Forwarded-For` header (first value) → fallback to `remoteAddr`.

**Scope:** Single-instance in-process only. Acceptable for MVP single-instance target (PRD NFR-11). Multi-instance deployments would require Redis-backed counters.

**Test isolation:** `AbstractIntegrationTest` auto-clears `RateLimitService` in `@BeforeEach` so rate-limit counters do not bleed across integration test classes.

---

## CI/CD Architecture

GitHub Actions pipeline at `.github/workflows/ci.yml`. Runs on push to `main` and on all pull requests.

```
backend ──────────────────┐
                          ├── docker-validate
backend-build (needs: backend) ─┘
frontend ─────────────────┘
```

| Job | What it does |
|---|---|
| `backend` | Spins up `postgres:16-alpine` service container, runs `./gradlew test` with `DATABASE_URL` injected |
| `backend-build` | Runs `./gradlew bootJar -x test` to verify production JAR assembles |
| `frontend` | `npm ci` → `vitest --run` → `oxlint` → `vite build` |
| `docker-validate` | Builds both Docker images (no push), validates both compose configs |

**Caching:** Gradle dependencies (`actions/setup-java cache: gradle`), npm modules (`actions/setup-node cache: npm`), Docker layers (`type=gha` via Buildx).

**No external credentials required:** test profile uses `DemoBillingProvider`, `ConsoleEmailProvider`, `DemoAIProvider`, empty Stripe keys.

See `docs/ci-architecture.md` for full details.

---

## Production Docker Architecture

```
docker-compose.yml       ← local development (postgres only, port 5433)
docker-compose.prod.yml  ← production stack (postgres + backend + frontend)
```

**Backend image** (`backend/Dockerfile`): two-stage build.
- Stage 1: `eclipse-temurin:21-jdk-alpine` — Gradle resolves dependencies (cached layer), compiles, produces `bootJar`
- Stage 2: `eclipse-temurin:21-jre-alpine` — copies JAR only; non-root `careerforge` user; no source, no compiler

**Frontend image** (`frontend/Dockerfile`): two-stage build.
- Stage 1: `node:22-alpine` — `npm ci` + `vite build` produces `dist/`
- Stage 2: `nginx:1.27-alpine` — serves static assets; custom `nginx.conf` with SPA fallback and `/api/` proxy to backend

**Production compose security:**
- PostgreSQL has no `ports:` block — not reachable from outside the Docker network
- `CAREERFORGE_ENV: production` and `CAREERFORGE_DEMO_MODE: "false"` are hardcoded (not env-var overridable)
- `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET` have no defaults — stack fails fast if missing
- All services: `restart: unless-stopped`, healthchecks on all three containers

A Resume is NOT a live view of MasterProfile. When a resume is created, all relevant profile content is copied into an immutable snapshot. Subsequent changes to MasterProfile do not affect existing resumes.

```
MasterProfile  (source of truth for profile data)
     │
     │  copied at creation / new-version time
     ▼
  Resume        (identity: name, owner, timestamps)
     │
     ├── ResumeVersion 1   (snapshot: title, summary + content)
     │    ├── ResumeExperience[]   ← copy of WorkExperience at snapshot time
     │    ├── ResumeEducation[]    ← copy of Education at snapshot time
     │    └── ResumeSkill[]        ← copy of Skill at snapshot time
     │
     └── ResumeVersion 2   (new snapshot taken later)
          ├── ResumeExperience[]
          ├── ResumeEducation[]
          └── ResumeSkill[]
```

**Why snapshot content is independent from MasterProfile:**

- Resume content has NO foreign keys to `work_experiences`, `educations`, or `skills` tables.
- `resume_experiences`, `resume_educations`, and `resume_skills` are owned entirely by `resume_versions`.
- Editing a resume entry (e.g. changing a company name) modifies only the resume snapshot — the MasterProfile record is untouched.
- This guarantees historical stability: a resume sent to an employer in January still shows January's content in February, even if the profile was updated.
- A new `ResumeVersion` can be created at any time to re-snapshot the current profile state.
