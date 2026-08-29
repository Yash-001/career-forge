# CI Architecture

## Overview

CareerForge uses GitHub Actions for continuous integration. The pipeline runs on every push to `main` and on every pull request targeting `main`.

Workflow file: `.github/workflows/ci.yml`

---

## Jobs

```
backend ──────────────────┐
                          ├── docker-validate
backend-build (needs: backend) ─┘
frontend ─────────────────┘
```

### 1. `backend` — Backend Tests

- Spins up a `postgres:16-alpine` service container on port 5432
- Sets up Java 21 (Eclipse Temurin) with Gradle dependency cache
- Runs `./gradlew test` with `DATABASE_URL` pointed at the service container
- Uploads the HTML test report as an artifact on failure

No external credentials required. The test profile uses:
- `app.billing.provider=demo` → `DemoBillingProvider`
- `app.email.provider=console` → `ConsoleEmailProvider`
- `ai.provider=demo` → `DemoAIProvider`
- Empty Stripe keys (app starts without them)

### 2. `backend-build` — Backend Production JAR

- Depends on `backend` (tests must pass first)
- Runs `./gradlew bootJar -x test` to verify the production JAR assembles cleanly
- Uses the same Gradle cache as the test job

### 3. `frontend` — Frontend Tests, Lint & Build

Runs three steps in sequence (single job to share the `npm ci` install):

| Step | Command | Purpose |
|---|---|---|
| Tests | `npm run test:unit -- --run` | Vitest unit tests (non-watch) |
| Lint | `npm run lint:oxlint` | oxlint static analysis |
| Build | `npm run build-only` | Vite production bundle |

Uses `actions/setup-node@v4` with `cache: npm` keyed on `frontend/package-lock.json`.

### 4. `docker-validate` — Docker Build Validation

- Depends on both `backend-build` and `frontend` passing
- Uses `docker/setup-buildx-action` + `docker/build-push-action` with `push: false`
- Builds both the backend and frontend Docker images (multi-stage)
- Layer cache stored in GitHub Actions cache (`type=gha`) to speed up repeat builds
- Validates both `docker-compose.yml` and `docker-compose.prod.yml` parse cleanly

---

## Dependency Caching

| Cache | Key | Scope |
|---|---|---|
| Gradle dependencies | `actions/setup-java cache: gradle` | `backend/**` |
| npm modules | `actions/setup-node cache: npm` | `frontend/package-lock.json` |
| Docker layers | `type=gha` via Buildx | Per Dockerfile |

---

## PostgreSQL in CI

Integration tests require a live PostgreSQL instance. In CI this is provided by a GitHub Actions service container:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    ports:
      - 5432:5432
```

`application-test.properties` uses `${DATABASE_URL:jdbc:postgresql://localhost:5433/careerforge}` so:
- **Local dev**: defaults to port 5433 (the `docker-compose.yml` dev database)
- **CI**: the workflow injects `DATABASE_URL=jdbc:postgresql://localhost:5432/careerforge` to match the service container

---

## What CI Does Not Require

- Stripe credentials — `DemoBillingProvider` is active in the test profile
- AI API keys — `DemoAIProvider` is active
- Email credentials — `ConsoleEmailProvider` logs to stdout
- Production database — service container is ephemeral
- Paid services — all GitHub Actions used are free tier

---

## Running the Pipeline Locally

Each CI stage has a direct local equivalent:

```bash
# Backend tests (requires docker-compose postgres running)
cd backend && ./gradlew test

# Backend production JAR
cd backend && ./gradlew bootJar -x test

# Frontend tests
cd frontend && npm run test:unit -- --run

# Frontend lint
cd frontend && npm run lint:oxlint

# Frontend production build
cd frontend && npm run build-only

# Docker image builds
docker build -f backend/Dockerfile backend
docker build -f frontend/Dockerfile frontend

# Compose config validation
docker compose config --quiet
docker compose -f docker-compose.prod.yml config --quiet
```

---

## Branch Protection (Recommended)

To enforce the pipeline as a merge gate, configure the following in GitHub → Settings → Branches → Branch protection rules for `main`:

- Require status checks to pass before merging
- Required checks: `Backend Tests`, `Backend Production Build`, `Frontend Tests, Lint & Build`, `Docker Build Validation`
- Require branches to be up to date before merging
