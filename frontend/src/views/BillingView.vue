<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-heading">Billing &amp; Subscription</h1>
        <p class="page-subheading">Manage your CareerForge plan.</p>
      </div>
    </header>

    <!-- Loading skeleton -->
    <div v-if="store.loading" class="skeleton-wrap" aria-busy="true" aria-label="Loading subscription">
      <div class="skeleton skeleton-block" style="height: 160px" />
      <div class="skeleton skeleton-block" style="height: 120px" />
    </div>

    <!-- Error -->
    <div v-else-if="store.error && !store.subscription" class="api-error" role="alert" data-testid="billing-error">
      {{ store.error }}
      <button class="btn btn-ghost btn-sm" style="margin-left:0.75rem" type="button" @click="store.loadSubscription()">Retry</button>
    </div>

    <template v-else-if="store.subscription">
      <!-- Current plan card -->
      <section class="card plan-card" aria-labelledby="plan-heading">
        <div class="plan-card-top">
          <div>
            <div class="plan-label" id="plan-heading">Current Plan</div>
            <div class="plan-name" :class="isPro ? 'plan-name--pro' : 'plan-name--free'">
              {{ isPro ? 'Pro' : 'Free' }}
            </div>
          </div>
          <span
            class="plan-status-badge"
            :class="statusBadgeClass"
            :aria-label="`Subscription status: ${statusLabel}`"
          >
            {{ statusLabel }}
          </span>
        </div>

        <!-- Demo provider notice -->
        <div v-if="store.subscription.provider === 'DEMO'" class="demo-notice" role="note" data-testid="demo-notice">
          <span class="demo-notice-icon" aria-hidden="true">🧪</span>
          <span>Demo billing — no real payment is processed. This is a simulated subscription environment.</span>
        </div>

        <!-- Billing period (Pro only) -->
        <dl v-if="isPro && store.subscription.currentPeriodEnd" class="plan-details">
          <div class="plan-detail-row">
            <dt>Billing period ends</dt>
            <dd>{{ formatDate(store.subscription.currentPeriodEnd) }}</dd>
          </div>
        </dl>

        <!-- PDF usage (Free only) -->
        <div v-if="!isPro && store.subscription.pdfExportsLimit !== null" class="usage-section" aria-label="PDF export usage">
          <div class="usage-header">
            <span class="usage-label">PDF Exports this month</span>
            <span class="usage-count" aria-live="polite">
              {{ store.subscription.pdfExportsUsed ?? 0 }} / {{ store.subscription.pdfExportsLimit }}
            </span>
          </div>
          <div
            class="usage-bar"
            role="progressbar"
            :aria-valuenow="store.subscription.pdfExportsUsed ?? 0"
            :aria-valuemax="store.subscription.pdfExportsLimit"
            aria-valuemin="0"
            :aria-label="`${store.subscription.pdfExportsUsed ?? 0} of ${store.subscription.pdfExportsLimit} PDF exports used`"
          >
            <div class="usage-bar-fill" :style="{ width: usagePercent + '%' }" :class="{ 'usage-bar-fill--full': usageAtLimit }" />
          </div>
          <p class="usage-remaining" :class="{ 'usage-remaining--warn': usageAtLimit }">
            <template v-if="usageAtLimit">
              Monthly limit reached. Upgrade to Pro for unlimited exports.
            </template>
            <template v-else>
              {{ exportsRemaining }} export{{ exportsRemaining === 1 ? '' : 's' }} remaining this month
            </template>
          </p>
        </div>

        <!-- Pro: unlimited exports -->
        <div v-if="isPro" class="pro-perks" aria-label="Pro plan benefits">
          <div class="perk-row">
            <span class="perk-icon" aria-hidden="true">✓</span>
            <span>Unlimited PDF exports</span>
          </div>
          <div class="perk-row">
            <span class="perk-icon" aria-hidden="true">✓</span>
            <span>Full AI tailoring access</span>
          </div>
        </div>
      </section>

      <!-- Action error -->
      <div v-if="store.error" class="api-error" role="alert" data-testid="action-error">
        {{ store.error }}
      </div>

      <!-- Upgrade CTA (Free users) -->
      <section v-if="!isPro" class="card upgrade-card" aria-labelledby="upgrade-heading">
        <div class="upgrade-card-body">
          <div>
            <h2 class="upgrade-heading" id="upgrade-heading">Upgrade to Pro</h2>
            <p class="upgrade-desc">Unlock unlimited PDF exports and full AI tailoring.</p>
            <ul class="upgrade-features" aria-label="Pro plan features">
              <li>Unlimited PDF exports per month</li>
              <li>Priority AI tailoring</li>
              <li>All future Pro features</li>
            </ul>
          </div>
          <div class="upgrade-action">
            <button
              class="btn btn-primary upgrade-btn"
              type="button"
              :disabled="store.upgrading"
              :aria-busy="store.upgrading"
              data-testid="upgrade-btn"
              @click="handleUpgrade"
            >
              <span v-if="store.upgrading" class="spinner" aria-hidden="true" />
              {{ store.upgrading ? 'Upgrading…' : 'Upgrade to Pro' }}
            </button>
            <p v-if="store.subscription.provider === 'DEMO'" class="upgrade-demo-note">
              Demo mode — upgrade is instant and simulated.
            </p>
          </div>
        </div>
      </section>

      <!-- Cancel subscription (Pro users) -->
      <section v-if="isPro" class="card cancel-card" aria-labelledby="cancel-heading">
        <h2 class="cancel-heading" id="cancel-heading">Cancel Subscription</h2>
        <p class="cancel-desc">
          Canceling will revert your account to the Free plan at the end of the current billing period.
          You will lose access to unlimited PDF exports.
        </p>
        <button
          class="btn btn-ghost cancel-btn"
          type="button"
          :disabled="store.canceling"
          :aria-busy="store.canceling"
          data-testid="cancel-btn"
          @click="showCancelConfirm = true"
        >
          Cancel subscription
        </button>
      </section>

      <!-- Cancel confirmation dialog -->
      <div
        v-if="showCancelConfirm"
        class="dialog-overlay"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cancel-dialog-title"
        aria-describedby="cancel-dialog-desc"
        data-testid="cancel-dialog"
        @keydown.esc="showCancelConfirm = false"
      >
        <div class="dialog-box" ref="cancelDialogRef" tabindex="-1">
          <h3 class="dialog-title" id="cancel-dialog-title">Cancel Subscription?</h3>
          <p class="dialog-message" id="cancel-dialog-desc">
            Your Pro plan will be canceled. You'll keep Pro access until the end of the current billing period,
            then revert to Free.
            <template v-if="store.subscription.provider === 'DEMO'">
              <br /><strong>Demo mode:</strong> cancellation is immediate and simulated.
            </template>
          </p>
          <div class="dialog-actions">
            <button class="btn btn-ghost" type="button" :disabled="store.canceling" @click="showCancelConfirm = false">
              Keep Pro
            </button>
            <button
              class="btn btn-danger"
              type="button"
              :disabled="store.canceling"
              :aria-busy="store.canceling"
              data-testid="confirm-cancel-btn"
              @click="handleCancel"
            >
              <span v-if="store.canceling" class="spinner" aria-hidden="true" />
              {{ store.canceling ? 'Canceling…' : 'Yes, cancel' }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useBillingStore } from '@/stores/billing'

const store = useBillingStore()
const showCancelConfirm = ref(false)
const cancelDialogRef = ref<HTMLElement | null>(null)

watch(showCancelConfirm, async (open) => {
  if (open) {
    await nextTick()
    cancelDialogRef.value?.focus()
  }
})

onMounted(() => store.loadSubscription())

const isPro = computed(() => store.subscription?.tier === 'PRO')

const statusLabel = computed(() => {
  switch (store.subscription?.status) {
    case 'ACTIVE': return 'Active'
    case 'CANCELED': return 'Canceled'
    case 'PAST_DUE': return 'Past due'
    case 'INACTIVE': return 'Inactive'
    default: return ''
  }
})

const statusBadgeClass = computed(() => ({
  'status-badge--active': store.subscription?.status === 'ACTIVE',
  'status-badge--canceled': store.subscription?.status === 'CANCELED',
  'status-badge--warn': store.subscription?.status === 'PAST_DUE' || store.subscription?.status === 'INACTIVE',
}))

const usagePercent = computed(() => {
  const used = store.subscription?.pdfExportsUsed ?? 0
  const limit = store.subscription?.pdfExportsLimit ?? 1
  return Math.min(100, Math.round((used / limit) * 100))
})

const usageAtLimit = computed(() => {
  const used = store.subscription?.pdfExportsUsed ?? 0
  const limit = store.subscription?.pdfExportsLimit ?? Infinity
  return used >= limit
})

const exportsRemaining = computed(() => {
  const used = store.subscription?.pdfExportsUsed ?? 0
  const limit = store.subscription?.pdfExportsLimit ?? 0
  return Math.max(0, limit - used)
})

async function handleUpgrade() {
  await store.upgrade()
}

async function handleCancel() {
  const ok = await store.cancelSubscription()
  if (ok) showCancelConfirm.value = false
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
}
</script>

<style scoped>
.page {
  max-width: 720px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.page-heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.page-subheading {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
}

/* Skeleton */
.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.skeleton-block {
  border-radius: var(--radius-lg);
}

/* Plan card */
.plan-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.plan-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.plan-label {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  margin-bottom: 0.25rem;
}

.plan-name {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
}

.plan-name--free {
  color: var(--color-text);
}

.plan-name--pro {
  color: var(--color-primary);
}

.plan-status-badge {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}

.status-badge--active {
  background: #dcfce7;
  color: #166534;
}

.status-badge--canceled {
  background: #f3f4f6;
  color: var(--color-text-muted);
}

.status-badge--warn {
  background: #fef9c3;
  color: #854d0e;
}

/* Demo notice */
.demo-notice {
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
}

.demo-notice-icon {
  flex-shrink: 0;
  font-size: 1rem;
}

/* Plan details */
.plan-details {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.plan-detail-row {
  display: flex;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.plan-detail-row dt {
  color: var(--color-text-muted);
  min-width: 160px;
}

.plan-detail-row dd {
  color: var(--color-text);
  font-weight: 500;
}

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
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
}

.usage-count {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
}

.usage-bar {
  height: 8px;
  background: var(--color-border);
  border-radius: 999px;
  overflow: hidden;
}

.usage-bar-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.usage-bar-fill--full {
  background: var(--color-danger);
}

.usage-remaining {
  font-size: 0.8125rem;
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
  gap: 0.5rem;
}

.perk-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text);
}

.perk-icon {
  color: #16a34a;
  font-weight: 700;
  flex-shrink: 0;
}

/* Upgrade card */
.upgrade-card {
  border: 2px solid var(--color-primary);
}

.upgrade-card-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.upgrade-heading {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 0.375rem;
}

.upgrade-desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-bottom: 0.75rem;
}

.upgrade-features {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.upgrade-features li {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  padding-left: 1.25rem;
  position: relative;
}

.upgrade-features li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #16a34a;
  font-weight: 700;
}

.upgrade-action {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
  flex-shrink: 0;
}

.upgrade-btn {
  padding: 0.625rem 1.5rem;
  font-size: 0.9375rem;
}

.upgrade-demo-note {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  max-width: 200px;
}

/* Cancel card */
.cancel-card {
  border-color: var(--color-border);
}

.cancel-heading {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.cancel-desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-bottom: 1rem;
  line-height: 1.6;
}

.cancel-btn {
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.cancel-btn:hover:not(:disabled) {
  background: var(--color-error-bg);
}

/* Dialog (reuse global pattern) */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.dialog-box {
  background: #fff;
  border-radius: 8px;
  padding: 1.5rem;
  width: min(440px, 90vw);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.16);
}

.dialog-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #111827;
}

.dialog-message {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 1.25rem;
  line-height: 1.6;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

@media (max-width: 600px) {
  .upgrade-card-body {
    flex-direction: column;
  }

  .upgrade-action {
    width: 100%;
  }

  .upgrade-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
