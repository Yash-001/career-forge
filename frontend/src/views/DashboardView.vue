<template>
  <div class="page">

    <!-- Loading skeleton -->
    <div v-if="store.loading" class="skeleton-wrap" aria-busy="true" aria-label="Loading dashboard" data-testid="dashboard-loading">
      <div class="skeleton" style="height:56px;border-radius:var(--radius-lg)" />
      <div class="kpi-grid">
        <div v-for="n in 4" :key="n" class="skeleton" style="height:88px;border-radius:var(--radius-lg)" />
      </div>
      <div class="skeleton" style="height:160px;border-radius:var(--radius-lg)" />
      <div class="skeleton" style="height:200px;border-radius:var(--radius-lg)" />
    </div>

    <!-- Error state -->
    <div v-else-if="store.error" class="error-state" role="alert" data-testid="dashboard-error">
      <p class="error-state__msg">{{ store.error }}</p>
      <button class="btn btn-ghost" type="button" @click="store.loadDashboard()">Retry</button>
    </div>

    <template v-else-if="store.summary">
      <!-- Welcome header -->
      <header class="welcome" data-testid="welcome-header">
        <div>
          <h1 class="welcome__heading">{{ greeting }}<template v-if="auth.firstName">, {{ auth.firstName }}</template></h1>
          <p class="welcome__sub">Track your career progress in one place.</p>
        </div>
        <div v-if="store.summary.profile.completionPercent < 100" class="profile-nudge" role="note" data-testid="profile-nudge">
          <span>Profile {{ store.summary.profile.completionPercent }}% complete</span>
          <RouterLink to="/profile" class="btn btn-ghost btn-sm">Complete profile →</RouterLink>
        </div>
      </header>

      <!-- KPI cards -->
      <section aria-labelledby="kpi-heading">
        <h2 id="kpi-heading" class="sr-only">Key metrics</h2>
        <div class="kpi-grid" data-testid="kpi-grid">
          <div class="kpi-card card" data-testid="kpi-applications">
            <div class="kpi-card__value">{{ store.summary.applications.total }}</div>
            <div class="kpi-card__label">Applications</div>
          </div>
          <div class="kpi-card card" data-testid="kpi-interviews">
            <div class="kpi-card__value kpi-card__value--amber">{{ store.summary.applications.interview }}</div>
            <div class="kpi-card__label">Interviews</div>
          </div>
          <div class="kpi-card card" data-testid="kpi-offers">
            <div class="kpi-card__value kpi-card__value--green">{{ store.summary.applications.offer }}</div>
            <div class="kpi-card__label">Offers</div>
          </div>
          <div class="kpi-card card" data-testid="kpi-resumes">
            <div class="kpi-card__value kpi-card__value--indigo">{{ store.summary.resumes.resumeCount }}</div>
            <div class="kpi-card__label">Resumes</div>
          </div>
        </div>
      </section>

      <!-- Main grid -->
      <div class="main-grid">

        <!-- Left column -->
        <div class="main-grid__left">

          <!-- Application pipeline -->
          <section class="card" aria-labelledby="pipeline-heading" data-testid="pipeline-section">
            <div class="card-header">
              <h2 class="card-title" id="pipeline-heading">Application Pipeline</h2>
              <RouterLink to="/applications" class="link-action">View all →</RouterLink>
            </div>
            <div v-if="store.summary.applications.total === 0" class="empty-state" data-testid="pipeline-empty">
              No applications yet.
              <RouterLink to="/applications" class="link-action">Add one →</RouterLink>
            </div>
            <div v-else class="pipeline" role="list" aria-label="Application status breakdown">
              <div
                v-for="stage in pipeline"
                :key="stage.key"
                class="pipeline__row"
                role="listitem"
                :data-testid="`pipeline-${stage.key}`"
              >
                <span class="pipeline__label">{{ stage.label }}</span>
                <div
                  class="pipeline__bar-wrap"
                  role="progressbar"
                  :aria-valuenow="stage.count"
                  :aria-valuemax="store.summary.applications.total"
                  aria-valuemin="0"
                  :aria-label="`${stage.label}: ${stage.count}`"
                >
                  <div class="pipeline__bar" :class="`pipeline__bar--${stage.key}`" :style="{ width: pipelineWidth(stage.count) }" />
                </div>
                <span class="pipeline__count">{{ stage.count }}</span>
              </div>
            </div>
          </section>

          <!-- Analytics -->
          <div class="card">
            <AnalyticsSection
              :analytics="store.analytics"
              :analytics-loading="store.analyticsLoading"
              :analytics-error="store.analyticsError"
              @retry="store.loadAnalytics()"
            />
          </div>

          <!-- Recent applications -->
          <section class="card" aria-labelledby="recent-apps-heading" data-testid="recent-apps-section">
            <div class="card-header">
              <h2 class="card-title" id="recent-apps-heading">Recent Applications</h2>
              <RouterLink to="/applications" class="link-action">View all →</RouterLink>
            </div>
            <div v-if="store.summary.applications.recentApplications.length === 0" class="empty-state" data-testid="recent-apps-empty">
              No applications yet.
            </div>
            <ul v-else class="recent-list" aria-label="Recent applications" data-testid="recent-apps-list">
              <li
                v-for="app in store.summary.applications.recentApplications"
                :key="app.id"
                class="recent-list__item"
              >
                <div class="recent-list__body">
                  <RouterLink
                    to="/applications"
                    class="recent-list__primary recent-list__link"
                    :aria-label="`${app.companyName} — ${app.jobTitle}, view applications`"
                    :data-testid="`app-link-${app.id}`"
                  >{{ app.companyName }}</RouterLink>
                  <span class="recent-list__secondary">{{ app.jobTitle }}</span>
                </div>
                <div class="recent-list__meta">
                  <span class="recent-list__date">{{ formatDate(app.applicationDate) }}</span>
                  <a
                    v-if="safeJobUrl(app.jobUrl)"
                    :href="safeJobUrl(app.jobUrl)!"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="job-url-link"
                    :aria-label="`View job posting for ${app.jobTitle} at ${app.companyName} (opens in new tab)`"
                    :data-testid="`job-url-${app.id}`"
                  >↗</a>
                  <StatusBadge :status="app.status as ApplicationStatus" />
                </div>
              </li>
            </ul>
          </section>

        </div>

        <!-- Right column -->
        <div class="main-grid__right">

          <!-- Quick actions -->
          <section class="card" aria-labelledby="actions-heading" data-testid="quick-actions-section">
            <h2 class="card-title" id="actions-heading" style="margin-bottom:1rem">Quick Actions</h2>
            <div class="actions-list">
              <RouterLink
                v-if="store.summary.quickActions.canCreateResume"
                to="/resumes/new"
                class="action-btn"
                data-testid="action-create-resume"
                aria-label="Create a new resume"
              >
                <span class="action-btn__icon" aria-hidden="true">📄</span>
                <span>Create Resume</span>
              </RouterLink>
              <RouterLink
                v-else
                to="/billing"
                class="action-btn action-btn--muted"
                data-testid="action-resume-limit"
                aria-label="Resume limit reached — upgrade to create more"
              >
                <span class="action-btn__icon" aria-hidden="true">📄</span>
                <span>Resume limit reached</span>
              </RouterLink>
              <RouterLink
                :to="tailorResumeLink"
                class="action-btn"
                data-testid="action-tailor-resume"
                aria-label="Tailor a resume with AI"
              >
                <span class="action-btn__icon" aria-hidden="true">🤖</span>
                <span>Tailor Resume</span>
              </RouterLink>
              <RouterLink
                to="/applications"
                class="action-btn"
                data-testid="action-track-application"
                aria-label="Track a job application"
              >
                <span class="action-btn__icon" aria-hidden="true">📋</span>
                <span>Track Application</span>
              </RouterLink>
              <RouterLink
                :to="exportResumeLink"
                class="action-btn"
                data-testid="action-export-resume"
                aria-label="Export resume as PDF"
              >
                <span class="action-btn__icon" aria-hidden="true">⬇️</span>
                <span>Export Resume</span>
              </RouterLink>
              <RouterLink
                v-if="store.summary.quickActions.canUpgrade"
                to="/billing"
                class="action-btn action-btn--upgrade"
                data-testid="action-upgrade"
                aria-label="Upgrade to Pro"
              >
                <span class="action-btn__icon" aria-hidden="true">⚡</span>
                <span>Upgrade to Pro</span>
              </RouterLink>
            </div>
          </section>

          <!-- Recent Activity -->
          <div class="card">
            <RecentActivitySection
              :activity="store.activity"
              :activity-loading="store.activityLoading"
              :activity-error="store.activityError"
              @retry="store.loadActivity()"
            />
          </div>

          <!-- Subscription / usage card -->
          <section class="card" aria-labelledby="sub-heading" data-testid="subscription-section">
            <div class="card-header">
              <h2 class="card-title" id="sub-heading">Subscription</h2>
              <RouterLink to="/billing" class="link-action">Manage →</RouterLink>
            </div>
            <div class="sub-tier" :class="isPro ? 'sub-tier--pro' : 'sub-tier--free'" data-testid="sub-tier">
              {{ isPro ? 'Pro' : 'Free' }}
            </div>
            <!-- Free: usage bar -->
            <div v-if="!isPro" class="usage-section" aria-label="PDF export usage" data-testid="usage-section">
              <div class="usage-header">
                <span class="usage-label">PDF Exports this month</span>
                <span class="usage-count" aria-live="polite">
                  {{ store.summary.usage.pdfExportsUsed }} / {{ store.summary.usage.pdfExportsLimit }}
                </span>
              </div>
              <div
                class="usage-bar"
                role="progressbar"
                :aria-valuenow="store.summary.usage.pdfExportsUsed"
                :aria-valuemax="store.summary.usage.pdfExportsLimit"
                aria-valuemin="0"
                :aria-label="`${store.summary.usage.pdfExportsUsed} of ${store.summary.usage.pdfExportsLimit} PDF exports used`"
              >
                <div
                  class="usage-bar__fill"
                  :class="{ 'usage-bar__fill--full': store.summary.usage.atLimit }"
                  :style="{ width: usagePercent + '%' }"
                />
              </div>
              <p class="usage-remaining" :class="{ 'usage-remaining--warn': store.summary.usage.atLimit }">
                <template v-if="store.summary.usage.atLimit">
                  Monthly limit reached.
                  <RouterLink to="/billing" class="link-action">Upgrade →</RouterLink>
                </template>
                <template v-else>
                  {{ exportsRemaining }} export{{ exportsRemaining === 1 ? '' : 's' }} remaining
                </template>
              </p>
            </div>
            <!-- Pro: perks -->
            <div v-else class="pro-perks" data-testid="pro-perks">
              <div class="perk-row"><span class="perk-check" aria-hidden="true">✓</span> Unlimited PDF exports</div>
              <div class="perk-row"><span class="perk-check" aria-hidden="true">✓</span> Full AI tailoring</div>
            </div>
          </section>

          <!-- Recent resumes -->
          <section class="card" aria-labelledby="recent-resumes-heading" data-testid="recent-resumes-section">
            <div class="card-header">
              <h2 class="card-title" id="recent-resumes-heading">Recent Resumes</h2>
              <RouterLink to="/resumes" class="link-action">View all →</RouterLink>
            </div>
            <div v-if="store.summary.resumes.recentResumes.length === 0" class="empty-state" data-testid="recent-resumes-empty">
              No resumes yet.
              <RouterLink to="/resumes/new" class="link-action">Create one →</RouterLink>
            </div>
            <ul v-else class="recent-list" aria-label="Recent resumes" data-testid="recent-resumes-list">
              <li
                v-for="resume in store.summary.resumes.recentResumes"
                :key="resume.id"
                class="recent-list__item"
              >
                <RouterLink :to="`/resumes/${resume.id}`" class="recent-list__primary recent-list__link">
                  {{ resume.name }}
                </RouterLink>
                <div class="recent-list__meta">
                  <span class="badge">v{{ resume.latestVersionNumber }}</span>
                  <span class="recent-list__date">{{ formatDate(resume.updatedAt) }}</span>
                </div>
              </li>
            </ul>
          </section>

        </div>
      </div>
    </template>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useAuthStore } from '@/stores/auth'
import StatusBadge from '@/components/application/StatusBadge.vue'
import AnalyticsSection from '@/components/dashboard/AnalyticsSection.vue'
import RecentActivitySection from '@/components/dashboard/RecentActivitySection.vue'
import type { ApplicationStatus } from '@/api/application'

const store = useDashboardStore()
const auth = useAuthStore()

onMounted(() => {
  store.loadDashboard()
  store.loadAnalytics()
  store.loadActivity()
})

// ── Greeting ───────────────────────────────────────────────────────────────
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
})

// ── Subscription ───────────────────────────────────────────────────────────
const isPro = computed(() => store.summary?.subscription.tier === 'PRO')

const usagePercent = computed(() => {
  const used = store.summary?.usage.pdfExportsUsed ?? 0
  const limit = store.summary?.usage.pdfExportsLimit ?? 1
  return Math.min(100, Math.round((used / limit) * 100))
})

const exportsRemaining = computed(() => {
  const used = store.summary?.usage.pdfExportsUsed ?? 0
  const limit = store.summary?.usage.pdfExportsLimit ?? 0
  return Math.max(0, limit - used)
})

// ── Pipeline ───────────────────────────────────────────────────────────────
const pipeline = computed(() => [
  { key: 'applied',   label: 'Applied',   count: store.summary?.applications.applied   ?? 0 },
  { key: 'interview', label: 'Interview', count: store.summary?.applications.interview ?? 0 },
  { key: 'offer',     label: 'Offer',     count: store.summary?.applications.offer     ?? 0 },
  { key: 'rejected',  label: 'Rejected',  count: store.summary?.applications.rejected  ?? 0 },
])

function pipelineWidth(count: number): string {
  const total = store.summary?.applications.total ?? 0
  if (total === 0) return '0%'
  return Math.round((count / total) * 100) + '%'
}

// ── Quick action links ─────────────────────────────────────────────────────
// Tailor: go to most recent resume editor at AI tab; fall back to /resumes
const tailorResumeLink = computed(() => {
  const first = store.summary?.resumes.recentResumes[0]
  return first ? `/resumes/${first.id}?section=ai` : '/resumes'
})

// Export: go to most recent resume editor; fall back to /resumes
const exportResumeLink = computed(() => {
  const first = store.summary?.resumes.recentResumes[0]
  return first ? `/resumes/${first.id}` : '/resumes'
})

// ── Safe external URL ──────────────────────────────────────────────────────
function safeJobUrl(url: string | null | undefined): string | null {
  if (!url) return null
  try {
    const parsed = new URL(url)
    if (parsed.protocol === 'https:' || parsed.protocol === 'http:') return url
  } catch {
    // invalid URL
  }
  return null
}

// ── Formatters ─────────────────────────────────────────────────────────────
function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Screen-reader only */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* Skeleton */
.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Error state */
.error-state {
  text-align: center;
  padding: 3rem 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.error-state__msg {
  color: var(--color-error-text);
  font-size: 0.9375rem;
}

/* Welcome */
.welcome {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.welcome__heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.welcome__sub {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
}

.profile-nudge {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.875rem;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: var(--radius);
  font-size: 0.8125rem;
  color: #92400e;
  flex-shrink: 0;
}

/* KPI grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.25rem 1rem;
  text-align: center;
  gap: 0.25rem;
}

.kpi-card__value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
}

.kpi-card__value--amber  { color: #d97706; }
.kpi-card__value--green  { color: #16a34a; }
.kpi-card__value--indigo { color: var(--color-primary); }

.kpi-card__label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
}

/* Main grid */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.5rem;
  align-items: start;
}

.main-grid__left,
.main-grid__right {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Link action */
.link-action {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-primary);
  text-decoration: none;
}

.link-action:hover {
  text-decoration: underline;
}

/* Pipeline */
.pipeline {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.pipeline__row {
  display: grid;
  grid-template-columns: 80px 1fr 32px;
  align-items: center;
  gap: 0.75rem;
}

.pipeline__label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
}

.pipeline__bar-wrap {
  height: 8px;
  background: var(--color-border);
  border-radius: 999px;
  overflow: hidden;
}

.pipeline__bar {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
  min-width: 4px;
}

.pipeline__bar--applied   { background: #1d4ed8; }
.pipeline__bar--interview { background: #d97706; }
.pipeline__bar--offer     { background: #16a34a; }
.pipeline__bar--rejected  { background: #dc2626; }

.pipeline__count {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text);
  text-align: right;
}

/* Recent list */
.recent-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.recent-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.625rem 0;
  border-bottom: 1px solid var(--color-border);
}

.recent-list__item:last-child {
  border-bottom: none;
}

.recent-list__body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.recent-list__primary {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-list__link {
  text-decoration: none;
}

.recent-list__link:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.recent-list__link:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-radius: 2px;
}

.recent-list__secondary {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-list__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.recent-list__date {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}

/* Job URL link */
.job-url-link {
  font-size: 0.8125rem;
  color: var(--color-primary);
  text-decoration: none;
  line-height: 1;
  padding: 0.125rem 0.25rem;
  border-radius: var(--radius);
  transition: background 0.15s;
}

.job-url-link:hover {
  background: #ede9fe;
}

.job-url-link:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Quick actions */
.actions-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  text-decoration: none;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.action-btn:hover {
  background: #ede9fe;
  border-color: #c4b5fd;
  color: var(--color-primary);
}

.action-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.action-btn--muted {
  color: var(--color-text-muted);
  opacity: 0.7;
}

.action-btn--upgrade {
  background: #ede9fe;
  border-color: #c4b5fd;
  color: var(--color-primary);
}

.action-btn--upgrade:hover {
  background: #ddd6fe;
}

.action-btn__icon {
  font-size: 1rem;
  flex-shrink: 0;
}

/* Subscription card */
.sub-tier {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 1rem;
}

.sub-tier--free { color: var(--color-text); }
.sub-tier--pro  { color: var(--color-primary); }

/* Usage */
.usage-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.usage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.usage-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text);
}

.usage-count {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text);
}

.usage-bar {
  height: 8px;
  background: var(--color-border);
  border-radius: 999px;
  overflow: hidden;
}

.usage-bar__fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.usage-bar__fill--full {
  background: var(--color-danger);
}

.usage-remaining {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.usage-remaining--warn {
  color: var(--color-danger);
  font-weight: 500;
}

/* Pro perks */
.pro-perks {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.perk-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text);
}

.perk-check {
  color: #16a34a;
  font-weight: 700;
}

/* Responsive */
@media (max-width: 900px) {
  .main-grid {
    grid-template-columns: 1fr;
  }

  .main-grid__right {
    order: -1;
  }
}

@media (max-width: 600px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .welcome {
    flex-direction: column;
  }

  .profile-nudge {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
