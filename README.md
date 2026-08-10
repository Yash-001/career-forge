# CareerForge

An AI-assisted career platform for building professional resumes, tailoring applications to job descriptions, and tracking the full job search lifecycle.

---

## Status

> **Phase 1 — Authentication Complete**
> Backend authentication infrastructure is implemented and tested.
> JWT auth, password reset, email provider abstraction, and structured error handling are in place.
> PostgreSQL runs via Docker Compose. Flyway migrations are applied automatically on startup.

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
