<template>
  <div class="landing">
    <!-- Hero -->
    <section class="hero">
      <div class="hero__inner">
        <span class="hero__eyebrow">AI-powered career toolkit</span>
        <h1 class="hero__headline">
          Resumes that get you<br />
          <span class="hero__accent">past the ATS</span>
        </h1>
        <p class="hero__sub">
          Build a master profile once. Generate tailored, ATS-friendly resumes for every role in
          minutes — powered by AI that rewrites your bullets to match the job description.
        </p>
        <div class="hero__actions">
          <RouterLink to="/register" class="btn btn-primary hero__cta">
            Get started free
          </RouterLink>
          <RouterLink to="/login" class="btn btn-ghost hero__cta">Sign in</RouterLink>
        </div>
        <p class="hero__note">Free tier includes 2 resumes and 3 PDF exports per month.</p>
      </div>
    </section>

    <!-- Features -->
    <section class="features" aria-labelledby="features-heading">
      <div class="features__inner">
        <h2 id="features-heading" class="features__heading">Everything you need to land the role</h2>
        <div class="features__grid">
          <div v-for="f in features" :key="f.title" class="feature-card card">
            <div class="feature-card__icon" aria-hidden="true">{{ f.icon }}</div>
            <h3 class="feature-card__title">{{ f.title }}</h3>
            <p class="feature-card__desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- How it works -->
    <section class="how" aria-labelledby="how-heading">
      <div class="how__inner">
        <h2 id="how-heading" class="how__heading">How it works</h2>
        <ol class="how__steps">
          <li v-for="(step, i) in steps" :key="step.title" class="how__step">
            <span class="how__step-num" aria-hidden="true">{{ i + 1 }}</span>
            <div>
              <strong class="how__step-title">{{ step.title }}</strong>
              <p class="how__step-desc">{{ step.desc }}</p>
            </div>
          </li>
        </ol>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-section">
      <div class="cta-section__inner">
        <h2 class="cta-section__heading">Ready to start applying smarter?</h2>
        <p class="cta-section__sub">No credit card required. Free tier available forever.</p>
        <RouterLink to="/register" class="btn btn-primary">Create your free account</RouterLink>
      </div>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <div class="landing-footer__inner">
        <span class="landing-footer__logo">⚡ CareerForge</span>
        <span class="landing-footer__copy">Built as a portfolio project. No paid services required.</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

if (auth.isAuthenticated) {
  router.replace('/resumes')
}

const features = [
  {
    icon: '📄',
    title: 'Resume Builder',
    desc: 'Create named resumes from your master profile. Edit content inline without touching your source data.',
  },
  {
    icon: '🤖',
    title: 'AI Tailoring',
    desc: 'Paste a job description and let AI rewrite your bullet points to match the role\'s language and keywords.',
  },
  {
    icon: '📊',
    title: 'Application Tracker',
    desc: 'Log every application, link a resume version, and track status from applied to offer.',
  },
  {
    icon: '📑',
    title: 'PDF Export',
    desc: 'Download ATS-friendly, single-column PDFs generated server-side — no design skills needed.',
  },
  {
    icon: '🗂️',
    title: 'Version History',
    desc: 'Every tailored resume creates a new version. Compare, restore, or branch from any point.',
  },
  {
    icon: '🔒',
    title: 'Secure by default',
    desc: 'JWT auth, ownership checks on every resource, and no secrets ever exposed to the client.',
  },
]

const steps = [
  {
    title: 'Build your master profile',
    desc: 'Enter your work history, education, and skills once. This is your source of truth.',
  },
  {
    title: 'Create a resume',
    desc: 'Snapshot your profile into a named resume. Edit it freely without affecting your profile.',
  },
  {
    title: 'Paste a job description',
    desc: 'The AI analyzes the role and rewrites your bullet points to match the language and keywords.',
  },
  {
    title: 'Accept suggestions and export',
    desc: 'Review AI suggestions, accept the ones you like, and download a clean ATS-ready PDF.',
  },
]
</script>

<style scoped>
.landing {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Hero */
.hero {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  padding: 5rem 1.5rem 4rem;
}

.hero__inner {
  max-width: 680px;
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
  line-height: 1.65;
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
  margin: 0;
}

/* Features */
.features {
  padding: 4rem 1.5rem;
  background: var(--color-bg);
}

.features__inner {
  max-width: 1100px;
  margin: 0 auto;
}

.features__heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  text-align: center;
  margin: 0 0 2.5rem;
}

.features__grid {
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

/* How it works */
.how {
  padding: 4rem 1.5rem;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.how__inner {
  max-width: 640px;
  margin: 0 auto;
}

.how__heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  text-align: center;
  margin: 0 0 2.5rem;
}

.how__steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.how__step {
  display: flex;
  gap: 1.25rem;
  align-items: flex-start;
}

.how__step-num {
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

.how__step-title {
  display: block;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.25rem;
}

.how__step-desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  line-height: 1.6;
  margin: 0;
}

/* CTA section */
.cta-section {
  padding: 4rem 1.5rem;
  background: var(--color-bg);
  text-align: center;
}

.cta-section__inner {
  max-width: 480px;
  margin: 0 auto;
}

.cta-section__heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 0.5rem;
}

.cta-section__sub {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin: 0 0 1.5rem;
}

/* Footer */
.landing-footer {
  margin-top: auto;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  padding: 1.25rem 1.5rem;
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

.landing-footer__copy {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}
</style>
