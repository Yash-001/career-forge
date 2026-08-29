# CareerForge

[![CI](https://github.com/Yash-001/career-forge/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Yash-001/career-forge/actions/workflows/ci.yml)

An AI-assisted career platform for building professional resumes, tailoring applications to job descriptions, and tracking the full job search lifecycle.

---

## Status

> **Phase 7F — Stripe Webhook Synchronization**
> `StripeWebhookController` at `POST /api/v1/webhooks/stripe` (permit-all — Stripe authenticates via signature). `StripeWebhookService` verifies signature via `Webhook.constructEvent`, processes events idempotently, and updates internal `Subscription` state. Events handled: `customer.subscription.created`, `customer.subscription.updated` (sync tier/status/period), `customer.subscription.deleted` (CANCELED + FREE), `invoice.payment_failed` (PAST_DUE). Unknown events are recorded and ignored. `stripe_webhook_events` table (V7 migration) stores `provider_event_id` with a unique constraint for deduplication — same event ID is never processed twice. `SubscriptionRepository` extended with `findByProviderSubscriptionId` and `findByProviderCustomerId`. Webhook endpoint excluded from JWT filter. `StripeWebhookServiceTest` — 8 pure unit tests with `@InjectMocks` (no live API, no DB). `DemoBillingProvider` unaffected.
> Phase 7F complete: 379/379 backend tests passing, BUILD SUCCESSFUL.

---

> **Phase 7E — Stripe Test-Mode Billing Provider**
> `StripeBillingProvider` implements `BillingProviderPort` with full Stripe test-mode integration. Customer create/retrieve by email (idempotent), subscription creation, cancellation, and status retrieval. All Stripe SDK types confined to `StripeBillingProvider` — nothing leaks to domain. `StripeProperties` (`@ConfigurationProperties(prefix="stripe")`) holds credentials; `isConfigured()` validates at call time so the app starts without Stripe credentials. `DemoBillingProvider` remains the default (`app.billing.provider=demo`). `stripe-java:26.3.0` added to `build.gradle`. `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_PRO_MONTHLY` env vars documented in `.env.example`. `application-test.properties` explicitly sets `app.billing.provider=demo` and empty Stripe keys. `StripeBillingProviderTest` — 23 pure unit tests with `mockStatic` (no live API, no DB). `BillingServiceIntegrationTest` updated: stale ‘no Stripe on classpath’ test replaced with `stripeSdk_isOnClasspath` and `activeProvider_isDemoInTestProfile`.
> Phase 7E complete: 371/371 backend tests passing, BUILD SUCCESSFUL.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, PrimeVue |
| Backend | Java 21, Spring Boot, Gradle |
| Database | PostgreSQL, Flyway |
| Testing | JUnit 5 (backend), Vitest (frontend) |
| Infrastructure | Docker, Docker Compose |

---

## Prerequisites

- Node.js 22+
- Java 21+
- npm 10+
- Docker (for PostgreSQL via Docker Compose)

---

## Quick Start

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Copy environment config
cp .env.example .env   # edit JWT_SECRET before production use

# 3. Start backend
cd backend && gradlew.bat bootRun

# 4. Start frontend
cd frontend && npm install && npm run dev
```

---

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs at `http://localhost:5173`

---

## Running the Backend

```bash
cd backend
./gradlew bootRun        # macOS / Linux
gradlew.bat bootRun      # Windows
```

Runs at `http://localhost:8080`

Health check: `GET http://localhost:8080/api/v1/health`

---

## Planned Modules

| Module | Description |
|---|---|
| Auth | Registration, login, JWT session management |
| Profile | Master profile — experience, education, skills |
| Resume | Resume builder, versioning, templates |
| AI Tailoring | Job description analysis, keyword matching, bullet rewriting |
| PDF Export | Server-side ATS-friendly PDF generation |
| Application Tracker | Job application pipeline and status tracking |
| Billing | Free/Pro subscription tiers (Stripe test mode) |

---

## Project Structure

```
careerforge/
├── frontend/        Vue 3 + TypeScript application
├── backend/         Spring Boot REST API
├── docs/            Architecture and roadmap documentation
├── database/        SQL migrations and seed data
├── scripts/         Utility scripts
└── docker-compose.yml
```

---

## Running Tests

**Frontend**
```bash
cd frontend && npm run test:unit
```

**Backend**
```bash
cd backend && ./gradlew test
```
