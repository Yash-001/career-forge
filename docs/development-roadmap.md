# CareerForge — Development Roadmap

---

## Phase 0 — Project Foundation ✅

- Monorepo structure
- Vue 3 + TypeScript + Vite frontend scaffold
- Spring Boot + Gradle backend scaffold
- PrimeVue, Pinia, Vue Router configured
- Axios API client abstraction
- Health endpoint (`GET /api/v1/health`)
- Root `.gitignore`, `README.md`
- Architecture and roadmap documentation

---

## Phase 1 — Authentication ✅

- User registration and login (email + password)
- JWT access token + refresh token
- Refresh token endpoint (`POST /api/v1/auth/refresh`)
- Password reset (token-based, ConsoleEmailProvider)
- Spring Security configuration
- BCrypt password hashing
- Structured error envelope
- 43 backend tests passing

> Frontend auth (login/register views, auth store, route guards) is deferred to Phase 2C-pre or can be done as a standalone prompt before Phase 2C.

---

## Phase 2 — Master Profile

### Phase 2A — Domain Model ✅

- `MasterProfile`, `WorkExperience`, `Education`, `Skill` JPA entities
- `EmploymentType`, `ProficiencyLevel` enums
- 4 repositories
- Flyway V2 migration (`V2__create_master_profile_tables.sql`)
- 15 repository integration tests
- 58 total tests passing

### Phase 2B — REST API ✅

- 14 endpoints under `/api/v1/profile`
- 11 DTOs (all Java records)
- `ProfileService` with ownership enforcement and date validation
- `ProfileController` (thin, `@AuthenticationPrincipal`)
- 4 new `DomainExceptions` factory methods
- 28 new integration tests
- 86 total tests passing

### Phase 2C — Frontend ✅

#### Phase 2C-1 — API Client + Auth Store + Profile Store + Router ✅

- `src/api/client.ts` — Bearer token injection, 401 redirect
- `src/api/profile.ts` — typed service for all 14 endpoints
- `src/stores/auth.ts` — `isAuthenticated`, `setTokens`, `clearTokens`
- `src/stores/profile.ts` — full CRUD actions for profile/experience/education/skills
- `src/stores/index.ts` — re-exports both stores
- `src/router/index.ts` — `/profile` route with `requiresAuth` guard

#### Phase 2C-2 — Reusable Form Components ✅

- `src/components/profile/ConfirmDialog.vue` — delete confirmation modal
- `src/components/profile/ExperienceForm.vue` — add/edit work experience
- `src/components/profile/EducationForm.vue` — add/edit education
- `src/components/profile/SkillForm.vue` — add/edit skill

#### Phase 2C-3 — ProfileView ✅

- `src/views/ProfileView.vue` — 6 sections (personal info, summary, online profiles, experience, education, skills), loading skeleton, empty states, 3s save feedback, delete confirmation

#### Phase 2C-4 — Shared CSS / Design Tokens ✅

- `src/assets/main.css` — full design token system, button/form/card/badge/spinner/skeleton/empty-state styles, responsive breakpoints at 600px and 480px

#### Phase 2C-5 — Frontend Tests ✅

- `src/components/__tests__/ProfileView.spec.ts` — 17 tests (sections render, loading, empty states, add/edit/delete for all entity types, validation, API error)
- `src/components/__tests__/HomeView.spec.ts` — 2 tests
- `src/test-setup.ts`, `vitest.config.ts` configured with happy-dom
- 20/20 tests passing, build clean, lint 0 errors

### Phase 2D — Master Profile Hardening ✅

- Fixed `ProfileService.java` — replaced inline fully-qualified class names with proper imports
- Fixed `ExperienceForm.vue` — removed unused import
- Fixed `ConfirmDialog.vue` — corrected aria attribute bindings
- Fixed `vite.config.ts` — disabled vueDevTools in test environment
- Fixed `vitest.config.ts` — switched to `mergeConfig` pattern to resolve Vite 8 / Vitest v3 plugin type mismatch
- Fixed `ProfileView.spec.ts` — form submit via `form.trigger('submit')` (happy-dom limitation), `.btn-danger` selector for confirm dialog
- Downgraded Vitest to `3.2.4` (v4.1.10 crashes on Node 22.12.0)
- Added `happy-dom` (jsdom@29 requires Node ≥22.13, incompatible with Node 22.12.0)
- Final: 86/86 backend tests, 20/20 frontend tests, build ✅, lint ✅

**Known Technical Debt:**
1. Three-save pattern in ProfileView (personal/summary/online each send full payload) — needs PATCH endpoint
2. Password reset token lookup uses `findAll()` + BCrypt iteration — deferred pre-scale
3. Refresh tokens stateless, no rotation/revocation — deferred
4. `minimal.spec.ts` diagnostic file left in test suite — can be deleted
5. Node 22.12 / jsdom incompatibility — upgrading to Node ≥22.13 allows switching back to jsdom

---

## Phase 3 — Resume Builder

### Phase 3A — Domain + Database Foundation ✅

- `Resume`, `ResumeVersion`, `ResumeExperience`, `ResumeEducation`, `ResumeSkill` JPA entities
- Snapshot architecture: resume content has NO FK to live profile tables
- `Resume + ResumeVersion` versioning model (identity separate from snapshot)
- `V3__create_resume_tables.sql` Flyway migration
- `ResumeRepository`, `ResumeVersionRepository` with `@EntityGraph` fetch (avoids `MultipleBagFetchException`)
- `ResumeService`: `createFromProfile`, `createNewVersion`, `listResumes`, `getResume`, `listVersions`, `getVersion`, `getLatestVersion`, `updateVersionExperience`, `deleteResume`
- 3 new `DomainExceptions` factory methods (`resumeNotFound`, `resumeVersionNotFound`, `resumeNameBlank`)
- 16 integration tests covering: snapshot isolation, resume-local edit, versioning, ownership, cascade delete, edge cases
- 102/102 total tests passing, BUILD SUCCESSFUL

**Architecture decision:** `Resume + ResumeVersion` chosen over flat versioned Resume because it cleanly separates resume identity from snapshot content, allows multiple versions to coexist, and makes future version restoration straightforward without data duplication at the Resume level.

### Phase 3B — REST API ✅

- 20 endpoints under `/api/v1/resumes`
- 13 DTOs (all Java records with Jakarta validation)
- `ResumeService` expanded: full CRUD for resumes, versions, experiences, educations, skills
- `ResumeController` (thin, `@AuthenticationPrincipal`, inline mappers)
- `ResumeExperienceRepository`, `ResumeEducationRepository`, `ResumeSkillRepository` — ownership-safe child lookups via `findByIdAndResumeVersionId`
- 3 new `DomainExceptions` factory methods (`resumeExperienceNotFound`, `resumeEducationNotFound`, `resumeSkillNotFound`)
- Ownership enforced: `requireOwnedResume` → `requireOwnedVersion` → child lookup (no existence leakage)
- 102/102 total tests passing, BUILD SUCCESSFUL

### Phase 3C — Frontend ✅

- `src/api/resume.ts` — typed API client for all 20 resume endpoints
- `src/stores/resume.ts` — Pinia store for resume list (load, rename, delete)
- `src/stores/index.ts` — re-exports resume store
- `src/router/index.ts` — 4 resume routes (`/resumes`, `/resumes/new`, `/resumes/:resumeId`, `/resumes/:resumeId/versions`) with `requiresAuth` guards
- `src/components/resume/ResumeExperienceForm.vue` — resume-local experience form
- `src/components/resume/ResumeEducationForm.vue` — resume-local education form
- `src/components/resume/ResumeSkillForm.vue` — resume-local skill form
- `src/components/resume/ExperienceSection.vue` — inline add/edit/delete list for experiences
- `src/components/resume/EducationSection.vue` — inline add/edit/delete list for education
- `src/components/resume/SkillsSection.vue` — inline add/edit/delete list for skills
- `src/views/ResumeListView.vue` — resume cards, empty state, loading skeleton, inline rename, delete confirmation
- `src/views/CreateResumeView.vue` — 2-step creation flow with snapshot explanation
- `src/views/ResumeEditorView.vue` — sidebar nav, summary/meta editor, experience/education/skills sections, new version branching, `?version=<id>` query param support
- `src/views/ResumeVersionHistoryView.vue` — version list sorted descending, Latest badge, Open links to editor
- `src/components/__tests__/ResumeListView.spec.ts` — 7 tests
- `src/components/__tests__/CreateResumeView.spec.ts` — 5 tests
- `src/components/__tests__/ResumeEditorView.spec.ts` — 9 tests
- `src/components/__tests__/ResumeVersionHistoryView.spec.ts` — 8 tests
- 49/49 frontend tests passing, type-check ✅, build ✅, lint ✅

### Phase 3D — Resume API Integration Tests ✅

- `ResumeApiIntegrationTest.java` — HTTP-level tests for all 20 endpoints
- 54 tests: full CRUD, ownership (cross-user 404), not-found, validation, snapshot isolation, auth (401)
- 54/54 tests passing, BUILD SUCCESSFUL

---

## Phase 4 — AI Tailoring

### Phase 4A — AI Provider Foundation ✅

- `AIProvider` interface — abstraction for all AI providers
- `DemoAIProvider` — deterministic rule-based provider (no API key, no network)
- `ExternalAIProvider` — stub for future external LLM integration (not a Spring bean)
- `AIService` — business layer with ownership enforcement via `ResumeService`
- `AIConfig` — selects active provider via `ai.provider` property (default: `demo`)
- DTOs: `JobAnalysisRequest`, `JobAnalysisResponse`, `TailoringRequest`, `TailoringResponse`, `BulletSuggestion`
- `ai.provider=demo` added to `application.properties` and `application-test.properties`
- 32 new unit tests (`DemoAIProviderTest` + `AIServiceTest`), no DB/network required
- All existing tests continue passing, BUILD SUCCESSFUL

### Phase 4B-1 — AI REST API Foundation ✅

- `AIController` — thin controller at `/api/v1/ai`, delegates to `AIService`
- `POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/analyze` — job description analysis endpoint
- `AIAnalyzeRequest` — HTTP request DTO (`jobDescription` only; `resumeSkills` populated server-side)
- Authentication required — unauthenticated requests return 401
- Ownership enforced via `AIService` — cross-user access returns 404 (no existence leakage)
- Jakarta validation: `@NotBlank`, `@Size(max=10000)` on job description
- 12 controller integration tests (HTTP method, status codes, validation, ownership, demo provider, field assertions)
- All existing tests continue passing, BUILD SUCCESSFUL

### Phase 4B-2 — AI Tailoring API ✅

- `POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/tailor` — bullet tailoring endpoint
- `AITailorRequest` — HTTP request DTO (`jobDescription` only; bullets and skills populated server-side)
- `DemoAIProvider.tailorResume` — deterministic rule-based bullet suggestions (keyword matching, no LLM, no network)
- `BulletWithId` provider-layer DTO — pairs `experienceId` with bullet text so the ID survives the provider round-trip
- `BulletSuggestion` carries `experienceId` — frontend sends it back in `AcceptedSuggestion` without relying on array position
- Authentication required — unauthenticated requests return 401
- Ownership enforced via `AIService` — cross-user access returns 404 (no existence leakage)
- Non-mutating — no resume, version, experience, education, skill, or profile entity is modified
- Suggestions not persisted — response only
- Jakarta validation: `@NotBlank`, `@Size(max=10000)` on job description
- 15 tailoring integration tests (success, 401, 400 blank, 400 oversized, 404 resume, 404 version, cross-user resume, cross-user version, response fields, original bullet, tailored suggestion, matched keywords, non-mutation of resume, non-mutation of profile, determinism)
- All existing tests continue passing, BUILD SUCCESSFUL

### Phase 4C — Frontend AI Experience ✅

- `src/api/ai.ts` — typed API client for analyze and tailor endpoints (`JobAnalysisResponse`, `BulletSuggestion`, `TailoringResponse`)
- `src/stores/ai.ts` — Pinia store (`jobDescription`, `analysisResult`, `tailoringResult`, `analyzingLoading`, `tailoringLoading`, `error`, `analyzeResume`, `tailorResume`, `clearResults`)
- `src/components/ai/JobDescriptionInput.vue` — textarea with label, character count, validation, disabled state, Analyze + Tailor Resume + Clear actions, accessible
- `src/components/ai/JobAnalysisResult.vue` — displays detected role, technologies, keywords, matched skills, missing skills; Demo AI badge; empty states per field
- `src/components/ai/TailoringSuggestions.vue` — per-bullet original/suggestion/matched keywords/rationale; Demo AI badge; empty state; read-only (no persistence at this stage)
- `ResumeEditorView.vue` — AI Tailoring nav item added; AI panel integrated; loading states (Analyzing…, Tailoring…); error display; empty states; stale result clearing on new analysis
- Demo AI indicator on analysis and tailoring results — clearly labelled as rule-based demo, not an LLM
- No resume data mutated — tailoring is suggestions-only at this stage
- 11 new `JobDescriptionInput` tests, 10 new `JobAnalysisResult` tests, 8 new `TailoringSuggestions` tests, 8 new `ResumeEditorView` AI tests
- All existing tests continue passing, BUILD SUCCESSFUL

### Phase 4D — Accept Tailoring (Original vs. Tailored Version Tracking) ✅

- `POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/accept-tailoring` — accepts a list of AI suggestions and creates a new `ResumeVersion`
- `AcceptTailoringRequest` / `AcceptedSuggestion` DTOs (Jakarta validation: `@NotEmpty`, `@NotNull`, `@NotBlank`)
- `AIService.acceptTailoring` — ownership check, validates all `experienceId`s against source version (422 `INVALID_SUGGESTION` on mismatch), delegates to `ResumeService.cloneVersionWithTailoring`
- `ResumeService.cloneVersionWithTailoring` — `@Transactional`, clones source version (not MasterProfile), applies accepted overrides, copies educations/skills verbatim, title gets `" — AI Tailored"` suffix with deduplication
- `AIController` — new `accept-tailoring` endpoint returning `201 ResumeVersionResponse`
- `DomainExceptions.invalidSuggestion()` — `422 INVALID_SUGGESTION`
- 16 new acceptance integration tests in `AIControllerIntegrationTest` (201, 401, 400 empty list, 404 resume, 404 version, cross-user 404, 422 invalid expId, 422 expId from other version, source unchanged, new version content, title suffix, no double suffix, version number increments, educations/skills copied, repeated acceptance creates separate versions, tailor response includes experienceId)
- Frontend: `BulletSuggestion` gains `experienceId`; `AcceptedSuggestion` / `AcceptTailoringPayload` types; `aiApi.acceptTailoring`
- Frontend: `useAiStore` gains `acceptTailoring` action, `acceptingLoading`, `acceptedVersion` state
- Frontend: `TailoringSuggestions.vue` — per-suggestion Accept/Reject toggles, Apply N Accepted Suggestions button, emits `apply` event
- Frontend: `ResumeEditorView.vue` — `handleAcceptTailoring`, success message, navigates to new version
- 16 new `TailoringSuggestions` tests (toggle, apply button, emit), 12 new `ResumeEditorView` acceptance/AI tests
- 246/246 backend tests passing, 100/100 frontend tests passing, BUILD SUCCESSFUL, lint ✅

### Phase 4E — Onboarding Lifecycle Integration Tests ✅

- `OnboardingLifecycleIntegrationTest` — full new-user onboarding regression test
- Covers: registration → MasterProfile creation → login → GET all sub-resources → create education/skill/experience → verify ownership at DB level → cross-user isolation (DELETE + PUT blocked with 404)
- 246/246 backend tests passing, 100/100 frontend tests passing, BUILD SUCCESSFUL, lint ✅

---

## Phase 5 — PDF Export

### Phase 5A — PDF Export Foundation ✅

- `com.github.librepdf:openpdf:1.3.43` added to `build.gradle`
- `pdf/dto/ResumeVersionData` — flat data carrier passed to generator (no DB access)
- `pdf/generator/PdfGenerator` — interface decoupling service from OpenPDF
- `pdf/generator/OpenPdfGenerator` — single-column ATS-friendly PDF via OpenPDF; no DB, no ownership, no billing
- `pdf/service/PdfExportService` — owns ownership enforcement (via `ResumeService`), data assembly, delegates to `PdfGenerator`; billing limit enforcement deferred to Phase 5C
- `DomainExceptions.exportLimitExceeded()` — `402 LIMIT_EXCEEDED` for Phase 5B enforcement
- `OpenPdfGeneratorTest` — 4 unit tests: minimal data, full data, null optional fields, currently-working experience; no DB/Spring context
- 250/250 backend tests passing, BUILD SUCCESSFUL

### Phase 5B — ATS-Friendly PDF Template ✅

- `OpenPdfGenerator` fully implemented: single-column ATS template, correct section order (Name → Contact → Summary → Experience → Education → Skills)
- Standard Helvetica fonts (machine-readable, universally supported by ATS parsers)
- Uppercase section headings with thin horizontal rule
- `SpacingBefore`/`SpacingAfter` on every paragraph — no bare `Chunk.NEWLINE` gaps
- Empty sections produce no heading (guard on every `render*` method)
- Long descriptions wrap naturally via iText paragraph flow
- `buildDateRange` handles start-only, start–end, and currently-working (“Present”) cases
- Invisible placeholder paragraph prevents OpenPDF from throwing on an empty document
- `OpenPdfGeneratorTest` — 8 tests using `PdfReader` + `PdfTextExtractor` to inspect actual rendered text:
  1. Basic resume — name, contact, summary present in extracted text
  2. Multiple experiences — all three entries rendered
  3. Multiple education entries — both entries rendered
  4. Multiple skills — all six skills in output
  5. Long text wrapping — full description present, no exception
  6. Empty optional fields — all-null input, no exception, valid PDF
  7. Empty sections — no orphaned EXPERIENCE/EDUCATION/SKILLS headings
  8. Unicode characters — accented Latin characters, no exception
  9. Multiple pages — `PdfReader.getNumberOfPages()` asserts > 1
- 255/255 backend tests passing, BUILD SUCCESSFUL

### Phase 5C — PDF Download API ✅

- `PdfController` — `GET /api/v1/resumes/{resumeId}/versions/{versionId}/pdf` returning `application/pdf` with `Content-Disposition: attachment`
- `PdfExportService.exportVersion` and `buildFilename` annotated `@Transactional(readOnly = true)` to resolve `LazyInitializationException` on `Resume.user`
- `PdfControllerIntegrationTest` — 5 integration tests (200 + correct headers, 401, 404 resume, 404 version, cross-user 404)
- 265/265 backend tests passing, BUILD SUCCESSFUL

### Phase 5D — Export Tracking + Free-Tier Limit Enforcement ✅

- `PdfExportUsage` JPA entity + `V4__create_pdf_export_usage_table.sql` Flyway migration
- `PdfExportUsageRepository` — pessimistic-write query for atomic check/increment; plain-read query for test assertions
- `SubscriptionService` interface + `DefaultSubscriptionService` — checks `SubscriptionTier.PRO` on `User`
- `ExportLimitService` — `checkLimit` and `recordExport` both `@Transactional(REQUIRES_NEW)`; FREE limit = 3/month; Pro bypasses both
- `DomainExceptions.exportLimitExceeded()` — `402 PDF_EXPORT_LIMIT_EXCEEDED`
- `PdfExportService` wired: calls `checkLimit` before generation, `recordExport` after
- `PdfExportLimitIntegrationTest` — 10 tests covering all limit scenarios
- 285/285 backend tests passing, BUILD SUCCESSFUL

### Phase 5E — PDF Export UI ✅

- `exportVersionPdf(resumeId, versionId): Promise<ArrayBuffer>` added to `src/api/resume.ts` (`responseType: 'arraybuffer'`)
- Export PDF button in `ResumeEditorView.vue` sidebar: loading spinner, disabled-while-exporting, `aria-label`, `aria-busy`
- `exportPdf()` — creates Blob → `URL.createObjectURL` → programmatic `<a>` click → `revokeObjectURL`
- Dedicated `PDF_EXPORT_LIMIT_EXCEEDED` error message: "Monthly PDF export limit reached (3/3). Upgrade to Pro for unlimited exports."
- Error box (`data-testid="export-error"`) for generic and limit errors
- 7 new Vitest tests (button renders, loading state, successful download, API failure, limit exceeded, correct IDs, accessibility)
- 107/107 frontend tests passing, lint ✅, build ✅, backend 285/285 tests still passing

---

## Phase 6 — Job Application Tracker ✅

### Phase 6A — Job Application Domain ✅

- `ApplicationStatus` enum: `APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`
- `Application` JPA entity (`@Table("job_applications")`): id (UUID), user (ManyToOne LAZY, not-updatable), companyName, jobTitle, applicationDate, jobUrl, resumeVersion (ManyToOne LAZY, nullable), status (EnumType.STRING, default APPLIED), createdAt/updatedAt (`@PrePersist`/`@PreUpdate`)
- `V5__create_job_applications_table.sql`: FK `user_id → users(id) ON DELETE CASCADE`, FK `resume_version_id → resume_versions(id) ON DELETE SET NULL`, CHECK constraint on status values, 6 indexes
- `ApplicationRepository`: `@EntityGraph(attributePaths={"resumeVersion"})` on `findByUserIdOrderByApplicationDateDesc` and `findByIdAndUserId` to prevent `LazyInitializationException` outside transaction
- `DomainExceptions.applicationNotFound()` → `404 APPLICATION_NOT_FOUND`
- 8 repository integration tests
- 293 total backend tests

### Phase 6B — Job Application REST API ✅

- `CreateApplicationRequest` / `UpdateApplicationRequest` / `ApplicationResponse` DTOs (Java records, Jakarta validation)
- `ApplicationService`: `create`, `listForUser`, `getOwned`, `update`, `delete` (all `@Transactional`); private `resolveOwnedVersion` — null-safe, JPQL traversal `v.resume.user.id = :userId`, throws `resumeVersionNotFound` for cross-user/non-existent (no existence leakage)
- `ApplicationController` at `/api/v1/applications`: POST (201), GET list (200), GET by id (200), PUT (200), DELETE (204)
- `ResumeVersionRepository.findByIdAndUserId` — JPQL ownership check added
- 22 integration tests
- 315 total backend tests

### Phase 6C — Resume Version Linking Hardening ✅

- `ApplicationResponse` enriched with `resumeVersionTitle` (String nullable) and `resumeVersionNumber` (Integer nullable)
- `ApplicationController.toResponse` extracts `ResumeVersion rv = app.getResumeVersion()` and populates title/number
- `@EntityGraph` on both read queries prevents `LazyInitializationException` when mapper accesses `rv.getTitle()` / `rv.getVersionNumber()` outside transaction boundary
- `ON DELETE SET NULL` on `resume_version_id` FK preserves application history when resume/version is deleted
- 5 new integration tests: retrieve version title/number, update linked version, remove link, cross-user version on update, resume deletion nullifies link while preserving application record
- 320 total backend tests

### Phase 6D — Job Application Tracker Frontend ✅

- `src/api/application.ts` — typed API module (`ApplicationStatus`, `ApplicationResponse`, `CreateApplicationPayload`, `UpdateApplicationPayload`, `extractError`)
- `src/stores/application.ts` — Pinia store: `loadApplications`, `addApplication`, `editApplication`, `removeApplication`, `byStatus` computed
- `src/components/application/StatusBadge.vue` — accessible status badge: colored dot + text label (not color-alone); four distinct schemes (blue/amber/green/red) for Applied/Interview/Offer/Rejected
- `src/components/application/ApplicationForm.vue` — create/edit dialog: all fields, client-side validation, resume version selector (loads user's own versions via `listResumes` + `listVersions`), `setSaving`/`setError` exposed for parent control
- `src/views/ApplicationListView.vue` — full list view: loading skeleton, empty state, filtered empty state, search (company/role), status filter pills, application cards (company/role/date/status badge/version badge/job URL link), edit/delete actions, delete confirmation dialog
- `src/router/index.ts` — added `{ path: '/applications', name: 'applications', requiresAuth: true }`
- `src/components/AppHeader.vue` — added "Applications" link in desktop and mobile nav
- `src/components/__tests__/ApplicationListView.spec.ts` — 29 tests covering all required scenarios
- 136 total frontend tests

### Phase 6E — End-to-End Hardening ✅

- Full end-to-end journey verified: login → open applications → create with resume version → list → edit → status transitions (APPLIED → INTERVIEW → OFFER) → delete → persistence
- Cross-user isolation verified at all layers: User A cannot see, modify, or link User B's applications or resume versions
- Existing features verified unaffected: resumes, version history, PDF export, AI tailoring, profile, authentication
- Bug fixed: `ApplicationForm.handleSubmit` was incorrectly managing `saving` state locally, conflicting with parent's `setSaving` control — simplified to synchronous emit
- Backend: `compileJava` ✅ `compileTestJava` ✅; 63 unit tests pass; all integration test failures = `PSQLException → ConnectException` (Docker not running, expected)
- Frontend: 136/136 tests ✅, lint 0 errors ✅, BUILD SUCCESSFUL ✅
- Total backend tests: **320** (63 unit + 257 integration)
- Total frontend tests: **136**

**Key implementation decisions:**
- `findByIdAndUserId` pattern throughout — returns `Optional.empty()` for both not-found and cross-user (no existence leakage)
- `resolveOwnedVersion` uses JPQL traversal `v.resume.user.id = :userId` for atomic ownership check without extra round-trips
- `ON DELETE SET NULL` on `resume_version_id` preserves application history when resumes are deleted
- `@EntityGraph` on both read queries prevents `LazyInitializationException` when mapper accesses lazy `resumeVersion` fields outside transaction
- No reverse `@OneToMany` collections added to `ResumeVersion` or `User` — avoids loading application data when fetching resumes
- Client-side filtering only (search + status) — backend supports server-side filtering via `findByUserIdAndStatusOrderByApplicationDateDesc` for future use
- Status badge uses dot + text label (not color alone) for WCAG accessibility compliance

---

## Phase 7 — Billing ✅

### Phase 7A — Billing & Subscription Domain Foundation ✅

- `Subscription` JPA entity in `billing` package: `tier`, `status`, `provider`, `providerCustomerId`, `providerSubscriptionId`, `currentPeriodStart`, `currentPeriodEnd`, timestamps
- `SubscriptionStatus` enum: `ACTIVE`, `INACTIVE`, `CANCELED`, `PAST_DUE`
- `BillingProvider` enum: `DEMO`, `STRIPE`
- `SubscriptionRepository`: `findActiveByUserId` (JPQL), `findByUserIdOrderByCreatedAtDesc`
- `V6__create_subscriptions_table.sql` Flyway migration: partial unique index `WHERE status = 'ACTIVE'` enforces one active subscription per user; nullable provider ID columns; CHECK constraints on all enums
- `SubscriptionService` interface extended: `isPro` (unchanged), `findActiveSubscription`, `provisionFreeSubscription` (idempotent)
- `DefaultSubscriptionService` wired to `SubscriptionRepository`; `isPro` still reads `User.subscriptionTier` (fast-path, no DB hit)
- `AuthService.register` calls `provisionFreeSubscription` after user creation
- `User.subscriptionTier` retained as denormalized field — all existing callers (`ExportLimitService`) unchanged
- `GlobalExceptionHandler` gap fixed: `HttpMessageNotReadableException` now returns 400 (was falling through to 500)
- `SubscriptionDomainIntegrationTest` — 12 tests covering all domain requirements
- `AuthServiceTest` updated: `@Mock SubscriptionService` added for Mockito injection
- 324/324 backend tests passing, BUILD SUCCESSFUL

### Phase 7B — Billing Provider Abstraction & Demo Provider ✅

- `BillingProviderPort` interface — provider abstraction: `initiateUpgrade`, `cancelSubscription`, `getSubscriptionState`, `getProvider`
- `ProviderSubscriptionState` record — provider-agnostic state DTO; no SDK types leak into domain
- `DemoBillingProvider` — implements `BillingProviderPort`; no external calls; deterministic; immediate FREE→PRO upgrade; PRO→CANCELED cancellation
- `BillingService` interface — `upgrade(User)`, `cancel(User)`, `getStatus(User)`
- `DefaultBillingService` — owns business logic: ownership check, state validation (`NO_ACTIVE_SUBSCRIPTION`, `ALREADY_PRO`), `User.subscriptionTier` sync after every state change
- `BillingConfig` — selects active `BillingProviderPort` bean via `app.billing.provider` property (default: `demo`); no code change required to switch providers
- `DomainExceptions.noActiveSubscription()` — 400 `NO_ACTIVE_SUBSCRIPTION`
- `DomainExceptions.alreadyPro()` — 409 `ALREADY_PRO`
- `DomainExceptions.billingProviderError(detail)` — 502 `BILLING_PROVIDER_ERROR`
- `app.billing.provider=${BILLING_PROVIDER:demo}` added to `application.properties`
- `BillingServiceIntegrationTest` — 10 tests covering all required scenarios
- 334/334 backend tests passing, BUILD SUCCESSFUL

### Phase 7C — Billing REST API ✅

- `BillingController` at `/api/v1/billing` — 3 endpoints, all require authentication via `@AuthenticationPrincipal User`; no userId accepted from request
- `GET /api/v1/billing/subscription` — returns tier, status, provider, billing period; FREE users get `pdfExportsUsed` + `pdfExportsLimit`; PRO users get null usage fields
- `POST /api/v1/billing/checkout` — initiates PRO upgrade via `BillingService.upgrade`; returns `CheckoutResponse` with action, tier, status, message; 409 if already PRO
- `POST /api/v1/billing/cancel` — cancels active subscription via `BillingService.cancel`; returns final subscription state; 400 if no active subscription
- `SubscriptionResponse` record — tier, status, provider, currentPeriodStart, currentPeriodEnd, pdfExportsUsed (nullable), pdfExportsLimit (nullable)
- `CheckoutResponse` record — action, tier, status, message
- `ExportLimitService.FREE_MONTHLY_LIMIT` made `public` (was package-private)
- `BillingApiIntegrationTest` — 14 tests: 401 on all 3 unauthenticated paths, GET subscription (FREE with usage, FREE with existing usage count, PRO with null usage, billing period present), POST checkout (upgrade success, already PRO 409), POST cancel (success, repeated cancel 400), cross-user isolation (GET own subscription, checkout only own user), provider failure (no subscription 400), health endpoint unaffected
- 348/348 backend tests passing, BUILD SUCCESSFUL

### Phase 7D — Subscription & Upgrade UI ✅

- `src/api/billing.ts` — typed API module: `SubscriptionTier`, `SubscriptionStatus`, `BillingProvider`, `SubscriptionResponse`, `CheckoutResponse`, `BillingApiError`; `getSubscription()`, `checkout()`, `cancel()`, `extractError()`
- `src/stores/billing.ts` — `useBillingStore`: state (`subscription`, `loading`, `upgrading`, `canceling`, `error`); actions (`loadSubscription`, `upgrade`, `cancelSubscription`)
- `src/views/BillingView.vue` — FREE: plan name, status badge, PDF usage progress bar, remaining count, limit warning, Upgrade CTA; PRO: plan name, status badge, billing period, unlimited exports perk list, cancel button; demo notice (`🧪`) when `provider === 'DEMO'`; cancel confirmation dialog; all errors via `data-testid="action-error"`
- `src/router/index.ts` — `/billing` route with `requiresAuth: true`
- `src/components/AppHeader.vue` — Billing link in desktop and mobile nav
- `src/stores/index.ts` — re-exports `useBillingStore`
- `BillingView.spec.ts` — 31 tests: loading, free plan display, pro plan display, usage display, upgrade, cancellation, loading states, error states, accessibility
- 167/167 frontend tests passing, lint ✅, build ✅

### Phase 7E — Stripe Test-Mode Billing Provider ✅

- `stripe-java:26.3.0` added to `build.gradle`
- `StripeProperties` — `@Component @ConfigurationProperties(prefix="stripe")`; fields: `secretKey`, `webhookSecret`, `priceProMonthly`; `isConfigured()` checks non-blank secretKey + priceProMonthly; all fields optional — app starts without credentials
- `StripeBillingProvider` — implements `BillingProviderPort`; all Stripe SDK types (`com.stripe.*`) confined to this class; `resolveOrCreateCustomer()` idempotent (stored ID → search by email → create); `toState()` is the only Stripe→domain mapping point; status mapping: `active/trialing→ACTIVE`, `canceled→CANCELED`, `past_due/unpaid→PAST_DUE`; all `StripeException` caught and wrapped as `billingProviderError`
- `BillingConfig` — updated error message to list supported values: `demo, stripe`
- `application.properties` — `stripe.secret-key`, `stripe.webhook-secret`, `stripe.price-pro-monthly` with empty defaults
- `application-test.properties` — `app.billing.provider=demo` + empty Stripe keys; all integration tests use demo
- `.env.example` — `BILLING_PROVIDER=demo`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_PRO_MONTHLY` with test-mode-only warnings
- `StripeBillingProviderTest` — 18 pure unit tests with `mockStatic` (no live API, no DB)
- `BillingServiceIntegrationTest` — updated: `stripeSdk_isOnClasspath` + `activeProvider_isDemoInTestProfile`
- 371/371 backend tests passing, BUILD SUCCESSFUL

### Phase 7F — Stripe Webhook Synchronization ✅

- `V7__create_stripe_webhook_events_table.sql` — deduplication table: `provider_event_id VARCHAR(255) NOT NULL UNIQUE`, `event_type`, `processed_at`
- `StripeWebhookEvent` — JPA entity for the above
- `StripeWebhookEventRepository` — `existsByProviderEventId` for O(1) duplicate check
- `StripeWebhookService` — `verifyAndParse()` calls `Webhook.constructEvent` with webhook secret; `process()` is `@Transactional`, checks deduplication before dispatch, records event after; events handled: `customer.subscription.created/updated` (sync tier/status/period + `User.subscriptionTier`), `customer.subscription.deleted` (CANCELED + FREE), `invoice.payment_failed` (PAST_DUE); unknown events recorded and ignored; `findLocalSubscription` looks up by sub ID first, falls back to customer ID
- `StripeWebhookController` — `POST /api/v1/webhooks/stripe`; returns 400 on `SignatureVerificationException`; no JWT required
- `SubscriptionRepository` — extended with `findByProviderSubscriptionId` and `findByProviderCustomerId`
- `SecurityConfig` — `/api/v1/webhooks/stripe` added to permit-all
- `StripeWebhookServiceTest` — 8 pure unit tests with `@InjectMocks` (no live API, no DB): valid signature, invalid signature, duplicate event, subscription activation, subscription cancellation, payment failure, unknown event, no matching local subscription
- `DemoBillingProvider` unaffected throughout
- 379/379 backend tests passing, BUILD SUCCESSFUL

### Phase 7G — Billing Integration and Security Hardening ✅

#### Audit Results

**FREE tier PDF limits** — verified: `ExportLimitService.FREE_MONTHLY_LIMIT = 3`; `checkLimit` enforces 3/month via pessimistic-write query; 4th export throws `402 PDF_EXPORT_LIMIT_EXCEEDED`; `BillingView.vue` displays usage progress bar and remaining count; `PdfExportLimitIntegrationTest` covers all limit scenarios (10 tests).

**PRO tier** — verified: `ExportLimitService.checkLimit` and `recordExport` both short-circuit on `subscriptionService.isPro(user)`; `isPro` reads `User.subscriptionTier` (denormalized fast-path, no DB hit); upgrade syncs `User.subscriptionTier` in `DefaultBillingService.upgrade` and `StripeWebhookService.syncUserTier`.

**Billing flows** — verified: upgrade (`FREE→PRO`), cancellation (`PRO→CANCELED`), provider sync via webhook, idempotent webhook processing (unique constraint on `provider_event_id`), ownership enforced via `@AuthenticationPrincipal User` on all billing endpoints.

**Authentication** — verified: all `/api/v1/billing/**` endpoints require JWT; `/api/v1/webhooks/stripe` is permit-all (Stripe authenticates via HMAC-SHA256 signature); `JwtAuthenticationFilter` not applied to webhook path.

**Secret leakage** — verified: no Stripe credentials logged anywhere; `StripeProperties` fields never appear in log statements; `application.properties` uses `${STRIPE_SECRET_KEY:}` env-var substitution with empty defaults; `.env` excluded by `.gitignore`; `git ls-files` confirms no `.env` or secret files tracked.

**Environment variables** — verified: `.env.example` documents all required vars with test-mode-only warnings on Stripe keys; `application-test.properties` sets empty Stripe keys and `app.billing.provider=demo`.

**Existing functionality** — verified unaffected: Authentication (29 tests), Profile (49 tests), Resume Builder (70 tests), AI Tailoring (78 tests), PDF Export (29 tests), Job Application Tracker (37 tests).

#### Final Test Counts — Phase 7 Complete

| Test Class | Tests |
|---|---|
| `BillingApiIntegrationTest` | 15 |
| `BillingServiceIntegrationTest` | 14 |
| `StripeBillingProviderTest` | 18 |
| `StripeWebhookServiceTest` | 8 |
| `SubscriptionDomainIntegrationTest` | 12 |
| **Billing subtotal** | **67** |
| All other packages | 312 |
| **Backend total** | **379** |
| **Frontend total** | **167** |

All tests passing. BUILD SUCCESSFUL. lint ✅. build ✅.

#### Stripe Test-Mode Setup

1. Create a Stripe account and navigate to the [test dashboard](https://dashboard.stripe.com/test/apikeys)
2. Copy the **Secret key** (`sk_test_...`) → set `STRIPE_SECRET_KEY` in `.env`
3. Create a Product + recurring Price → copy the Price ID (`price_...`) → set `STRIPE_PRICE_PRO_MONTHLY`
4. Install the [Stripe CLI](https://stripe.com/docs/stripe-cli) and run: `stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe`
5. Copy the webhook signing secret (`whsec_...`) printed by the CLI → set `STRIPE_WEBHOOK_SECRET`
6. Set `BILLING_PROVIDER=stripe` in `.env`
7. Restart the backend

#### Required Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | Yes | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | Yes | DB username |
| `DATABASE_PASSWORD` | Yes | DB password |
| `JWT_SECRET` | Yes | HS256 signing secret (32+ chars) |
| `BILLING_PROVIDER` | No | `demo` (default) or `stripe` |
| `STRIPE_SECRET_KEY` | Stripe only | `sk_test_...` — never use live keys |
| `STRIPE_WEBHOOK_SECRET` | Stripe only | `whsec_...` from Stripe CLI or dashboard |
| `STRIPE_PRICE_PRO_MONTHLY` | Stripe only | `price_...` for the Pro monthly plan |

#### DemoBillingProvider

`DemoBillingProvider` is the default provider (`app.billing.provider=demo`). It requires no credentials, makes no external calls, and is deterministic:
- `initiateUpgrade` — immediately returns `PRO/ACTIVE` with a 30-day period
- `cancelSubscription` — immediately returns `FREE/CANCELED`
- `getSubscriptionState` — reflects the current local `Subscription` record

All integration tests use `DemoBillingProvider` via `application-test.properties`. `StripeBillingProvider` is registered as a Spring bean but its credentials are validated at call time — the application starts without Stripe credentials regardless of which provider is active.

---

## Phase 8 — Dashboard ✅

### Phase 8A — Dashboard Domain & Service Foundation ✅

- `DashboardSummary`, `ProfileSummary`, `ResumeSummary`, `ApplicationSummary`, `SubscriptionSummary`, `UsageSummary`, `QuickActions` DTOs (Java records)
- `DashboardService` — single `@Transactional(readOnly=true)` method assembling all 6 sections
- Profile completion: 4-point scoring (exists 25% + title 25% + summary 25% + contact 25%)
- Resume summary: count, version count, top-5 recent
- Application summary: total + per-status counts (APPLIED/INTERVIEW/OFFER/REJECTED) + top-5 recent
- Subscription summary: tier, status, provider, billing period (no provider secrets)
- Usage summary: PDF exports used/limit/atLimit for FREE; zeros for PRO
- Quick actions: `canCreateResume` (FREE ≤ 2 resumes), `canLogApplication`, `canUpgrade`
- `GET /api/v1/dashboard` endpoint
- 13 `DashboardServiceTest` + 12 `DashboardApiIntegrationTest` tests

### Phase 8B — Dashboard Frontend Core ✅

- `src/api/dashboard.ts` — typed interfaces for all 6 summary sections + `dashboardApi.get()`
- `src/stores/dashboard.ts` — Pinia store: `summary`, `loading`, `error`, `loadDashboard()`
- `src/views/DashboardView.vue` — full dashboard layout: loading skeleton, error+retry, welcome header with greeting, profile nudge, KPI grid (4 cards), application pipeline with progress bars, recent applications list, quick actions, subscription card with usage bar, recent resumes list
- `src/router/index.ts` — `/dashboard` route with `requiresAuth`
- `src/components/AppHeader.vue` — Dashboard link in desktop and mobile nav

### Phase 8C — Dashboard Frontend Tests ✅

- `DashboardView.spec.ts` — 50 tests: loading, error, retry, empty states, KPI values, pipeline rows, recent applications, subscription (FREE/PRO), usage bar, quick actions, profile nudge, accessibility attributes, store integration
- All existing tests unaffected

### Phase 8D — Dashboard Analytics ✅

- `AnalyticsSummary` + `ApplicationTrendEntry` DTOs
- `AnalyticsService` — pipeline counts + 12-month trend via JPQL `GROUP BY` year/month
- `GET /api/v1/dashboard/analytics` endpoint
- `AnalyticsSection.vue` — SVG donut chart (pipeline distribution, 4 segments, legend with %) + CSS bar chart (monthly trend, proportional heights); no chart library
- Loading skeleton, error+retry, empty states; ARIA `role="img"` with descriptive labels
- 6 `AnalyticsServiceTest` + 7 `AnalyticsApiIntegrationTest` + 21 `AnalyticsSection.spec.ts` tests

### Phase 8E — Activity Feed & Quick Action Navigation ✅

- `RecentActivityEntry` DTO
- `ActivityService` — aggregates top-5 from 4 sources (resume updates, version creates, application creates, PDF exports); merges, sorts descending, limits to 10; no N+1
- `GET /api/v1/dashboard/activity` endpoint
- `RecentActivitySection.vue` — activity feed with icon, `RouterLink` label, subLabel, relative time (`<time datetime="...">`); loading skeleton, error+retry, empty state
- `RecentApplicationEntry` extended with `jobUrl`; `safeJobUrl()` validates `https?://` only (blocks `javascript:`, `data:`, malformed)
- Quick actions: Tailor Resume → `/resumes/:firstId?section=ai`; Export Resume → `/resumes/:firstId`; both fall back to `/resumes`
- 8 `ActivityServiceTest` + 7 `ActivityApiIntegrationTest` + 19 `RecentActivitySection.spec.ts` tests
- `DashboardView.spec.ts` extended to 50 tests covering all Phase 8E features

### Phase 8F — Dashboard UX, Responsive & Performance Hardening ✅

#### Performance
- Eliminated N+1 in `DashboardService.buildResumeSummary`: replaced per-resume `findMaxVersionNumber` calls with a single JPQL `SELECT r, MAX(v.versionNumber) … GROUP BY r` query via `findTop5WithMaxVersionByUserId`
- Reduced dashboard from 3 API round-trips to 1: `DashboardSummary` now embeds `analytics` and `activity`; `GET /dashboard` returns all data in a single response
- `onMounted` in `DashboardView` reduced from 3 calls (`loadDashboard` + `loadAnalytics` + `loadActivity`) to 1 (`loadDashboard`)
- Separate `/analytics` and `/activity` endpoints retained for targeted retry on partial failure

#### Responsive
- `main-grid` right column changed from fixed `340px` to `minmax(0, 320px)` — prevents overflow at constrained widths
- Added `@media (max-width: 1024px)` breakpoint: right column narrows to `minmax(0, 280px)`
- `@media (max-width: 768px)`: single-column layout, KPI grid collapses to 2 columns
- `@media (max-width: 600px)`: welcome header stacks, profile nudge full-width, pipeline label column narrows from 80px to 64px
- `analytics-grid` breakpoint extended from 600px to 768px — charts stack on tablet
- `profile-nudge` `flex-shrink: 0` removed — prevents overflow at 360px

#### Accessibility
- Inline `style="margin-bottom:1.25rem"` on `AnalyticsSection` heading replaced with scoped CSS class

### Phase 8G — Final Integration & Verification ✅

#### End-to-End Journey Verified

| Step | Feature | Result |
|---|---|---|
| 1 | Register | `POST /api/v1/auth/register` → 201, free subscription provisioned |
| 2 | Login | `POST /api/v1/auth/login` → JWT access + refresh tokens |
| 3 | Dashboard | `GET /api/v1/dashboard` → single response with all 8 sections |
| 4 | Profile completion | Nudge shown at 0%; disappears at 100% |
| 5 | Create resume | `POST /api/v1/resumes` → snapshot from master profile |
| 6 | Create version | `POST /api/v1/resumes/:id/versions` → version number increments |
| 7 | AI tailor | `POST /api/v1/ai/…/tailor` → suggestions; `POST …/accept-tailoring` → new version |
| 8 | Export PDF | `GET /api/v1/resumes/:id/versions/:vid/pdf` → ATS-friendly PDF, usage recorded |
| 9 | Create application | `POST /api/v1/applications` → linked to resume version |
| 10 | Link resume version | `PUT /api/v1/applications/:id` → `resumeVersionId` updated |
| 11 | Change status | `PUT /api/v1/applications/:id` → APPLIED → INTERVIEW → OFFER |
| 12–13 | Return to dashboard | KPI counts, pipeline bars, recent lists all reflect live data |
| 14 | Upgrade (Demo) | `POST /api/v1/billing/checkout` → FREE→PRO, `User.subscriptionTier` synced |
| 15 | Pro state | Subscription card shows PRO; usage bar hidden; pro perks shown |
| 16 | Export again | No limit enforced for PRO; `ExportLimitService.checkLimit` short-circuits |
| 17 | Logout | Tokens cleared from localStorage; redirect to `/` |
| 18 | Login again | New JWT issued; dashboard reloads from DB |
| 19 | Persistence | All metrics, resumes, applications, subscription state persisted correctly |

#### Security Isolation Verified

| Layer | Isolation mechanism | Verified |
|---|---|---|
| Profile | `findByUserId` — no cross-user lookup possible | ✅ |
| Resumes | `findByIdAndUserId` — 404 on cross-user (no existence leakage) | ✅ |
| Resume versions | JPQL `v.resume.user.id = :userId` traversal | ✅ |
| Applications | `findByIdAndUserId` — 404 on cross-user | ✅ |
| Billing | `@AuthenticationPrincipal User` — no userId from request | ✅ |
| Dashboard | All sub-queries scoped to `user.getId()` | ✅ |
| Stripe secrets | Never logged; env-var substitution; `.env` gitignored | ✅ |
| Provider secrets | `SubscriptionSummary` omits `providerCustomerId` / `providerSubscriptionId` | ✅ |

#### Final Test Counts — Phase 8 Complete

| Module | Backend tests | Frontend tests |
|---|---|---|
| Authentication | 29 | — |
| Profile | 49 | 17 |
| Resume Builder | 70 | 47 |
| AI Tailoring | 78 | 45 |
| PDF Export | 29 | 7 |
| Job Applications | 37 | 29 |
| Billing | 67 | 31 |
| Dashboard | 83 | 90 |
| Security Hardening | 36 | — |
| Accessibility | — | 48 |
| Shared / infra | — | 7 |
| **Total** | **502** | **332** |

**Backend: 502/502 passing. BUILD SUCCESSFUL.**
**Frontend: 332/332 passing. lint 0 errors. build successful.**

#### Known Limitations

1. Dashboard data is assembled synchronously in a single transaction — for users with large datasets (thousands of applications) the response time will grow; pagination or async assembly should be considered post-MVP
2. Activity feed sources are bounded to Top-5 per source — a user with many PDF exports in one billing period will see the same export entry repeated; a per-event table would give a richer feed
3. Analytics trend window is fixed at 12 months with no user-configurable range
4. `minimal.spec.ts` diagnostic file remains in the frontend test suite (1 test) — safe to delete
5. Node 22.12 / jsdom incompatibility carried forward from Phase 2D — upgrading to Node ≥22.13 allows switching back to jsdom

---

## Phase 9 — Polish and Deployment

### Phase 9E — Accessibility Audit ✅

- Full WCAG 2.1 AA-oriented audit across all major user journeys (login, register, profile, resume editor, application tracker, billing, dashboard)
- Fixed 9 categories of issues across 14 files:
  - Missing `aria-label` on icon-only buttons
  - Form fields without associated `<label>` elements
  - Color-only status indicators (added text labels)
  - Missing `role` and `aria-*` on custom interactive components
  - Keyboard navigation gaps (missing `tabindex`, `@keydown` handlers)
  - Focus management after modal open/close
  - Missing `alt` text on meaningful images
  - Insufficient color contrast on secondary text
  - Missing `<title>` on SVG elements used as content
- `accessibility.spec.ts` — 48 regression tests covering all fixed categories
- 332/332 frontend tests passing

### Phase 9F — Production Security Hardening ✅

- Full security hardening pass against PRD SEC-01 through SEC-15
- **JWT secret validation** — `@PostConstruct validateJwtSecret()` in `SecurityConfig`: fails fast in production if secret < 32 bytes or matches known placeholder prefixes (`change-this`, `replace-this`, `test-secret`, `secret`, `your-secret`); logs warning in dev
- **Rate limiting** — `RateLimitService` (in-process fixed-window, 20 req/60s per IP) + `RateLimitFilter` (registered before JWT filter); protects login, register, forgot-password, reset-password; returns `429 RATE_LIMIT_EXCEEDED`
- **Security headers** — `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin` on all responses
- **CORS hardening** — origin string split on comma + trimmed to handle multi-origin env vars
- **ConsoleEmailProvider production block** — `EmailConfig` throws `IllegalStateException` at startup if `app.env=production` and `app.email.provider=console`
- **Password reset query** — replaced `findAll()` full table scan with `findValidTokens()` JPQL (`WHERE used=false AND expires_at > NOW()`)
- **Input size limits** — `@Size(max=255)` on all email fields; `@Size(max=5000)` on all description/summary fields across 8 DTOs
- **Content-Disposition hardening** — `PdfController` uses RFC 5987 `filename*=UTF-8''<encoded>` alongside ASCII fallback to prevent header injection
- **MissingRequestHeaderException** — added explicit 400 handler in `GlobalExceptionHandler` (previously fell through to 500)
- **`AbstractIntegrationTest`** — auto-clears `RateLimitService` in `@BeforeEach` to prevent counter bleed across test classes
- `SecurityHardeningTest.java` — 36 regression tests covering all 10 categories
- 502/502 backend tests passing, BUILD SUCCESSFUL

### Phase 9G — Production Docker Configuration ✅

- `backend/Dockerfile` — multi-stage: `eclipse-temurin:21-jdk-alpine` build stage (Gradle dependency cache layer + `bootJar`) → `eclipse-temurin:21-jre-alpine` runtime (JAR only, non-root `careerforge` user)
- `frontend/Dockerfile` — multi-stage: `node:22-alpine` build stage (`npm ci` + `vite build`) → `nginx:1.27-alpine` runtime (static assets only)
- `frontend/nginx.conf` — SPA fallback (`try_files`), `/api/` proxy to backend service, 1-year immutable cache on hashed assets
- `docker-compose.prod.yml` — full production stack: postgres (no exposed port), backend (port 8080), frontend/nginx (port 80); `CAREERFORGE_ENV: production` and `CAREERFORGE_DEMO_MODE: "false"` hardcoded; required vars (`DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`) have no defaults; all services `restart: unless-stopped` with healthchecks
- `docker-compose.yml` — added `name: careerforge-dev` to isolate dev and prod compose namespaces
- Both `docker compose config` and `docker compose -f docker-compose.prod.yml config` validate cleanly
- Backend Docker image builds successfully (verified locally)
- Frontend production build: 384 modules transformed, build clean

### Phase 9H — CI/CD Quality Pipeline ✅

- `.github/workflows/ci.yml` — 4-job GitHub Actions pipeline
- **`backend` job** — `postgres:16-alpine` service container on port 5432; `actions/setup-java@v4` with Gradle cache; `chmod +x gradlew` guard; `./gradlew test` with `DATABASE_URL` injected; HTML test report uploaded as artifact on failure
- **`backend-build` job** — depends on `backend`; runs `./gradlew bootJar -x test`; uses same Gradle cache
- **`frontend` job** — `actions/setup-node@v4` with npm cache; `npm ci` → `vitest --run` → `oxlint` → `vite build`
- **`docker-validate` job** — depends on `backend-build` + `frontend`; `docker/setup-buildx-action@v3`; builds both images with `push: false` and GHA layer cache; validates both compose configs
- `application-test.properties` — replaced hardcoded `localhost:5433` with `${DATABASE_URL:jdbc:postgresql://localhost:5433/careerforge}` so CI injects `localhost:5432` while local dev defaults to 5433
- `backend/gradlew` — executable bit set to `100755` in git index (was `100644`, caused immediate failure on Linux CI)
- `docs/ci-architecture.md` — full pipeline documentation: job graph, caching strategy, postgres service container explanation, local equivalents, branch protection recommendations
- `README.md` — CI status badge added after pipeline verified passing
- Pipeline verified: run #2 all 4 jobs green — `completed success`

| Job | Duration | Result |
|---|---|---|
| Frontend Tests, Lint & Build | ~32s | ✅ success |
| Backend Tests | ~2m | ✅ success |
| Backend Production Build | ~55s | ✅ success |
| Docker Build Validation | ~2m 17s | ✅ success |

---

## Phase 10 — Deployment ⬜

- Deploy to reference infrastructure (Oracle Cloud Always Free or equivalent VPS)
- HTTPS via reverse proxy (nginx + Let's Encrypt or Caddy)
- Environment variable injection via secrets manager or `.env` on host
- Landing page
- Demo account seed data
- README screenshots
- Final documentation review

---

## Phase 11+ — Future (Post-MVP)

- Multiple PDF templates
- Cover letter generation
- Interview preparation questions
- LinkedIn profile import
- Email notifications
- Application notes and contacts
- Public shareable resume link
- OAuth login
- Team/coach accounts
