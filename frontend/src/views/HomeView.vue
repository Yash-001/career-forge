<template>
  <div class="landing">

    <!-- ── Hero ──────────────────────────────────────────────────────────── -->
    <section class="hero" aria-labelledby="hero-heading">
      <div class="hero__inner">
        <span class="hero__eyebrow">AI-assisted career platform</span>
        <h1 id="hero-heading" class="hero__headline">
          Build better careers.<br />
          <span class="hero__accent">Apply smarter.</span>
        </h1>
        <p class="hero__sub">
          CareerForge helps you build a master profile, create tailored resumes for every role,
          generate ATS-friendly PDFs, track every application, and understand your job-search
          progress — all in one place.
        </p>
        <div class="hero__actions">
          <RouterLink to="/register" class="btn btn-primary hero__cta">Start for free</RouterLink>
          <button
            class="btn btn-ghost hero__cta"
            type="button"
            :disabled="demoLoading"
            :aria-busy="demoLoading"
            data-testid="try-demo-btn"
            @click="handleDemoLogin"
          >
            <span v-if="demoLoading" class="spinner" aria-hidden="true" />
            {{ demoLoading ? 'Loading demo…' : 'Try demo' }}
          </button>
        </div>
        <p v-if="demoError" class="hero__demo-error" role="alert" data-testid="demo-error">{{ demoError }}</p>
        <p class="hero__note">Free tier: 2 resumes · 3 PDF exports/month · no credit card required.</p>
      </div>
    </section>

    <!-- ── Features ──────────────────────────────────────────────────────── -->
    <section class="section section--alt" aria-labelledby="features-heading">
      <div class="section__inner">
        <h2 id="features-heading" class="section__heading">Everything you need to land the role</h2>
        <div class="features-grid">
          <article v-for="f in features" :key="f.title" class="feature-card card">
            <div class="feature-card__icon" aria-hidden="true">{{ f.icon }}</div>
            <h3 class="feature-card__title">{{ f.title }}</h3>
            <p class="feature-card__desc">{{ f.desc }}</p>
          </article>
        </div>
      </div>
    </section>

    <!-- ── How it works ───────────────────────────────────────────────────── -->
    <section class="section" aria-labelledby="how-heading">
      <div class="section__inner section__inner--narrow">
        <h2 id="how-heading" class="section__heading">How it works</h2>
        <ol class="steps" aria-label="Four-step process">
          <li v-for="(step, i) in steps" :key="step.title" class="steps__item">
            <span class="steps__num" aria-hidden="true">{{ i + 1 }}</span>
            <div>
              <strong class="steps__title">{{ step.title }}</strong>
              <p class="steps__desc">{{ step.desc }}</p>
            </div>
          </li>
        </ol>
      </div>
    </section>

    <!-- ── Pricing ────────────────────────────────────────────────────────── -->
    <section class="section section--alt" aria-labelledby="pricing-heading">
      <div class="section__inner">
        <h2 id="pricing-heading" class="section__heading">Simple, transparent pricing</h2>

        <div class="demo-billing-notice" role="note" data-testid="demo-billing-notice">
          <span aria-hidden="true">🧪</span>
          <span>
            Billing runs on <strong>DemoBillingProvider</strong> — no real payment is processed.
            Upgrade is instant and simulated. Stripe test-mode is available when configured.
          </span>
        </div>

        <div class="pricing-grid">
          <div class="pricing-card card" data-testid="pricing-free">
            <div class="pricing-card__tier">Free</div>
            <div class="pricing-card__price">$0<span class="pricing-card__period">/month</span></div>
            <ul class="pricing-card__features" aria-label="Free plan features">
              <li>2 saved resumes</li>
              <li>3 PDF exports per month</li>
              <li>AI tailoring</li>
              <li>Application tracker</li>
              <li>Dashboard analytics</li>
            </ul>
            <RouterLink to="/register" class="btn btn-ghost pricing-card__cta">Get started</RouterLink>
          </div>

          <div class="pricing-card pricing-card--pro card" data-testid="pricing-pro">
            <div class="pricing-card__badge">Pro</div>
            <div class="pricing-card__tier">Pro</div>
            <div class="pricing-card__price">$9<span class="pricing-card__period">/month</span></div>
            <ul class="pricing-card__features" aria-label="Pro plan features">
              <li>Unlimited saved resumes</li>
              <li>Unlimited PDF exports</li>
              <li>All Free features</li>
              <li>Priority AI tailoring</li>
              <li>All future Pro features</li>
            </ul>
            <RouterLink to="/register" class="btn btn-primary pricing-card__cta">Start free, upgrade anytime</RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Trust / Engineering ────────────────────────────────────────────── -->
    <section class="section" aria-labelledby="tech-heading">
      <div class="section__inner">
        <h2 id="tech-heading" class="section__heading">Built with production-grade engineering</h2>
        <p class="section__sub">
          CareerForge is a portfolio project demonstrating full-stack SaaS architecture.
          Every claim below is verifiable in the public codebase.
        </p>
        <div class="tech-grid">
          <div v-for="t in techItems" :key="t.label" class="tech-item">
            <span class="tech-item__icon" aria-hidden="true">{{ t.icon }}</span>
            <div>
              <strong class="tech-item__label">{{ t.label }}</strong>
              <p class="tech-item__desc">{{ t.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Final CTA ──────────────────────────────────────────────────────── -->
    <section class="cta-section" aria-labelledby="cta-heading">
      <div class="cta-section__inner">
        <h2 id="cta-heading" class="cta-section__heading">Create your first resume</h2>
        <p class="cta-section__sub">Free tier available. No credit card required.</p>
        <RouterLink to="/register" class="btn btn-primary cta-section__btn">Get started free</RouterLink>
      </div>
    </section>

    <!-- ── Footer ─────────────────────────────────────────────────────────── -->
    <footer class="landing-footer" role="contentinfo">
      <div class="landing-footer__inner">
        <span class="landing-footer__logo" aria-label="CareerForge">⚡ CareerForge</span>
        <nav class="landing-footer__links" aria-label="Footer navigation">
          <a
            href="https://github.com"
            class="landing-footer__link"
            target="_blank"
            rel="noopener noreferrer"
          >GitHub</a>
          <RouterLink to="/login" class="landing-footer__link">Login</RouterLink>
          <RouterLink to="/register" class="landing-footer__link">Register</RouterLink>
        </nav>
        <span class="landing-footer__copy">Portfolio project. No paid services required to run.</span>
      </div>
    </footer>

  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { demoLogin } from '@/api/demo'

const auth = useAuthStore()
const router = useRouter()

if (auth.isAuthenticated) {
  router.replace('/dashboard')
}

const demoLoading = ref(false)
const demoError = ref<string | null>(null)

async function handleDemoLogin() {
  demoLoading.value = true
  demoError.value = null
  try {
    const result = await demoLogin()
    if (!result) {
      demoError.value = 'Demo mode is not enabled on this server.'
      return
    }
    auth.setTokens(result.accessToken, result.refreshToken, result.firstName)
    router.replace('/dashboard')
  } catch {
    demoError.value = 'Could not connect to the demo server. Please try again.'
  } finally {
    demoLoading.value = false
  }
}

const features = [
  {
    icon: '🗂️',
    title: 'Master Profile',
    desc: 'Enter your work history, education, and skills once. Every resume draws from this single source of truth.',
  },
  {
    icon: '📄',
    title: 'Resume Builder',
    desc: 'Create named resumes from your profile. Edit content inline without touching your master data.',
  },
  {
    icon: '🤖',
    title: 'AI Tailoring',
    desc: 'Paste a job description and AI rewrites your bullet points to match the role\'s language and keywords.',
  },
  {
    icon: '📑',
    title: 'ATS PDF Export',
    desc: 'Download clean, single-column PDFs generated server-side — no external API, no design skills needed.',
  },
  {
    icon: '📋',
    title: 'Application Tracker',
    desc: 'Log every application, link a resume version, and track status from applied through offer.',
  },
  {
    icon: '📊',
    title: 'Dashboard Analytics',
    desc: 'See your full pipeline at a glance — applications by status, recent activity, and export usage.',
  },
  {
    icon: '💳',
    title: 'Free / Pro Billing',
    desc: 'Start free with 2 resumes and 3 exports/month. Upgrade to Pro for unlimited access. Stripe test-mode ready.',
  },
]

const steps = [
  {
    title: 'Build your profile',
    desc: 'Add your work history, education, and skills. This is your master record — edit it once, use it everywhere.',
  },
  {
    title: 'Create and tailor your resume',
    desc: 'Snapshot your profile into a named resume, paste a job description, and let AI align your bullets to the role.',
  },
  {
    title: 'Apply and track results',
    desc: 'Export an ATS-friendly PDF, log the application, and link the exact resume version you submitted.',
  },
  {
    title: 'Improve your job search',
    desc: 'Review your pipeline analytics, spot patterns, and iterate on your approach with real data.',
  },
]

const techItems = [
  {
    icon: '🔐',
    label: 'JWT authentication',
    desc: 'Stateless JWT access tokens. Ownership checks enforced on every resource endpoint.',
  },
  {
    icon: '🗄️',
    label: 'PostgreSQL + Flyway',
    desc: 'Relational schema with versioned migrations. No data loss on schema changes.',
  },
  {
    icon: '☕',
    label: 'Spring Boot backend',
    desc: 'Layered Java 21 monolith: controller → service → domain → persistence. Clean separation of concerns.',
  },
  {
    icon: '⚡',
    label: 'Vue 3 + TypeScript frontend',
    desc: 'Composition API, Pinia stores, Vue Router. Type-safe throughout.',
  },
  {
    icon: '📄',
    label: 'Server-side PDF generation',
    desc: 'ATS-compatible PDFs built with an open-source Java library. No external PDF API required.',
  },
  {
    icon: '🔌',
    label: 'Provider abstraction',
    desc: 'AI, email, and billing are swappable via environment variable. No code changes needed to switch providers.',
  },
  {
    icon: '✅',
    label: 'Automated test suite',
    desc: '442 backend tests (JUnit 5) and 257 frontend tests (Vitest) covering provider failure scenarios.',
  },
  {
    icon: '🐳',
    label: 'Docker Compose',
    desc: 'Full local stack — backend, frontend dev server, and PostgreSQL — with a single command.',
  },
]
</script>

<style scoped>
.landing {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ── Hero ──────────────────────────────────────────────────────────────── */
.hero {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  padding: 5rem 1.5rem 4.5rem;
}

.hero__inner {
  max-width: 700px;
  margin: 0 auto;
  text-align: center;
}

.hero__eyebrow {
  display: inline-block;
  font-size: 0.8125rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-primary);
  margin-bottom: 1rem;
}

.hero__headline {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--color-text);
  margin: 0 0 1.25rem;
}

.hero__accent {
  color: var(--color-primary);
}

.hero__sub {
  font-size: 1.0625rem;
  color: var(--color-text-muted);
  line-height: 1.7;
  margin: 0 0 2rem;
}

.hero__actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.hero__cta {
  padding: 0.65rem 1.5rem;
  font-size: 0.9375rem;
}

.hero__note {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.hero__demo-error {
  font-size: 0.8125rem;
  color: var(--color-danger);
  margin-bottom: 0.25rem;
}

/* ── Shared section layout ─────────────────────────────────────────────── */
.section {
  padding: 4.5rem 1.5rem;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.section--alt {
  background: var(--color-bg);
}

.section__inner {
  max-width: 1100px;
  margin: 0 auto;
}

.section__inner--narrow {
  max-width: 640px;
}

.section__heading {
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--color-text);
  text-align: center;
  margin: 0 0 2.5rem;
  letter-spacing: -0.01em;
}

.section__sub {
  font-size: 0.9375rem;
  color: var(--color-text-muted);
  text-align: center;
  max-width: 600px;
  margin: -1.5rem auto 2.5rem;
  line-height: 1.65;
}

/* ── Features grid ─────────────────────────────────────────────────────── */
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.25rem;
}

.feature-card {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.feature-card__icon {
  font-size: 1.75rem;
  line-height: 1;
}

.feature-card__title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.feature-card__desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  line-height: 1.6;
  margin: 0;
}

/* ── How it works ──────────────────────────────────────────────────────── */
.steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.steps__item {
  display: flex;
  gap: 1.25rem;
  align-items: flex-start;
}

.steps__num {
  flex-shrink: 0;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.1rem;
}

.steps__title {
  display: block;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.25rem;
}

.steps__desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  line-height: 1.6;
  margin: 0;
}

/* ── Pricing ───────────────────────────────────────────────────────────── */
.demo-billing-notice {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.625rem 0.875rem;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: var(--radius);
  font-size: 0.8125rem;
  color: #92400e;
  line-height: 1.5;
  max-width: 640px;
  margin: -1.5rem auto 2rem;
}

.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.25rem;
  max-width: 680px;
  margin: 0 auto;
}

.pricing-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  position: relative;
}

.pricing-card--pro {
  border-color: var(--color-primary);
  border-width: 2px;
}

.pricing-card__badge {
  position: absolute;
  top: -0.75rem;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-primary);
  color: #fff;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  white-space: nowrap;
}

.pricing-card__tier {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.pricing-card__price {
  font-size: 2.25rem;
  font-weight: 800;
  color: var(--color-text);
  line-height: 1;
}

.pricing-card__period {
  font-size: 1rem;
  font-weight: 400;
  color: var(--color-text-muted);
}

.pricing-card__features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  flex: 1;
}

.pricing-card__features li {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  padding-left: 1.25rem;
  position: relative;
}

.pricing-card__features li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #16a34a;
  font-weight: 700;
}

.pricing-card__cta {
  text-align: center;
  justify-content: center;
}

/* ── Tech / trust grid ─────────────────────────────────────────────────── */
.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1.5rem;
}

.tech-item {
  display: flex;
  gap: 0.875rem;
  align-items: flex-start;
}

.tech-item__icon {
  font-size: 1.5rem;
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.tech-item__label {
  display: block;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.2rem;
}

.tech-item__desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.55;
  margin: 0;
}

/* ── Final CTA ─────────────────────────────────────────────────────────── */
.cta-section {
  padding: 5rem 1.5rem;
  background: var(--color-primary);
  text-align: center;
}

.cta-section__inner {
  max-width: 480px;
  margin: 0 auto;
}

.cta-section__heading {
  font-size: 1.75rem;
  font-weight: 800;
  color: #fff;
  margin: 0 0 0.5rem;
  letter-spacing: -0.01em;
}

.cta-section__sub {
  font-size: 0.9375rem;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 1.75rem;
}

.cta-section__btn {
  background: #fff;
  color: var(--color-primary);
  border-color: #fff;
  padding: 0.75rem 2rem;
  font-size: 1rem;
  font-weight: 600;
}

.cta-section__btn:hover:not(:disabled) {
  background: #f5f3ff;
  border-color: #f5f3ff;
}

/* ── Footer ────────────────────────────────────────────────────────────── */
.landing-footer {
  margin-top: auto;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  padding: 1.5rem;
}

.landing-footer__inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.landing-footer__logo {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text);
}

.landing-footer__links {
  display: flex;
  gap: 1.25rem;
  flex-wrap: wrap;
}

.landing-footer__link {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  text-decoration: none;
  transition: color 0.15s;
}

.landing-footer__link:hover {
  color: var(--color-text);
}

.landing-footer__link:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-radius: 2px;
}

.landing-footer__copy {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

/* ── Responsive ────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .features-grid {
    grid-template-columns: 1fr;
  }

  .tech-grid {
    grid-template-columns: 1fr;
  }

  .pricing-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .hero {
    padding: 3.5rem 1rem 3rem;
  }

  .section {
    padding: 3rem 1rem;
  }

  .landing-footer__inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .cta-section {
    padding: 3.5rem 1rem;
  }
}
</style>
