<template>
  <section class="analytics-section" aria-labelledby="analytics-heading" data-testid="analytics-section">
    <h2 class="card-title analytics-section__heading" id="analytics-heading">Application Analytics</h2>

    <!-- Loading -->
    <div v-if="analyticsLoading" class="analytics-loading" aria-busy="true" aria-label="Loading analytics" data-testid="analytics-loading">
      <div class="skeleton" style="height: 160px; border-radius: var(--radius)" />
    </div>

    <!-- Error -->
    <div v-else-if="analyticsError" class="analytics-error" role="alert" data-testid="analytics-error">
      <p class="analytics-error__msg">{{ analyticsError }}</p>
      <button class="btn btn-ghost btn-sm" type="button" @click="$emit('retry')">Retry</button>
    </div>

    <template v-else-if="analytics">
      <div class="analytics-grid">

        <!-- Pipeline donut distribution -->
        <div class="chart-card" data-testid="pipeline-chart">
          <h3 class="chart-title">Pipeline Distribution</h3>
          <div v-if="pipelineTotal === 0" class="chart-empty" data-testid="pipeline-chart-empty">
            No applications yet
          </div>
          <template v-else>
            <div class="donut-wrap" role="img" :aria-label="donutAriaLabel">
              <svg viewBox="0 0 120 120" class="donut-svg" aria-hidden="true">
                <circle cx="60" cy="60" r="48" fill="none" stroke="var(--color-border)" stroke-width="16" />
                <circle
                  v-for="seg in donutSegments"
                  :key="seg.key"
                  cx="60" cy="60" r="48"
                  fill="none"
                  :stroke="seg.color"
                  stroke-width="16"
                  :stroke-dasharray="`${seg.dash} ${circumference - seg.dash}`"
                  :stroke-dashoffset="seg.offset"
                  stroke-linecap="butt"
                />
                <text x="60" y="56" text-anchor="middle" class="donut-total-num">{{ pipelineTotal }}</text>
                <text x="60" y="70" text-anchor="middle" class="donut-total-label">total</text>
              </svg>
            </div>
            <ul class="donut-legend" aria-label="Pipeline legend">
              <li
                v-for="seg in donutSegments"
                :key="seg.key"
                class="donut-legend__item"
                :data-testid="`legend-${seg.key}`"
              >
                <span class="donut-legend__dot" :style="{ background: seg.color }" aria-hidden="true" />
                <span class="donut-legend__label">{{ seg.label }}</span>
                <span class="donut-legend__count">{{ seg.count }}</span>
                <span class="donut-legend__pct">{{ seg.pct }}%</span>
              </li>
            </ul>
          </template>
        </div>

        <!-- Monthly trend bar chart -->
        <div class="chart-card" data-testid="trend-chart">
          <h3 class="chart-title">
            Applications Over Time
            <span class="chart-subtitle">(last 12 months)</span>
          </h3>
          <div v-if="analytics.trend.length === 0" class="chart-empty" data-testid="trend-chart-empty">
            No data yet
          </div>
          <div
            v-else
            class="bar-chart"
            role="img"
            :aria-label="trendAriaLabel"
            data-testid="trend-bars"
          >
            <div class="bar-chart__y-max" aria-hidden="true">{{ trendMax }}</div>
            <div class="bar-chart__bars">
              <div
                v-for="bar in trendBars"
                :key="bar.label"
                class="bar-chart__col"
                :data-testid="`trend-bar-${bar.label}`"
              >
                <div class="bar-chart__bar-wrap">
                  <div
                    class="bar-chart__bar"
                    :style="{ height: bar.heightPct + '%' }"
                    :title="`${bar.label}: ${bar.count}`"
                  />
                </div>
                <span class="bar-chart__x-label" aria-hidden="true">{{ bar.shortLabel }}</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AnalyticsSummary } from '@/api/dashboard'

const props = defineProps<{
  analytics: AnalyticsSummary | null
  analyticsLoading: boolean
  analyticsError: string | null
}>()

defineEmits<{ retry: [] }>()

const MONTH_ABBR = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const PIPELINE_COLORS: Record<string, string> = {
  applied:   '#1d4ed8',
  interview: '#d97706',
  offer:     '#16a34a',
  rejected:  '#dc2626',
}

// circumference of r=48 circle
const circumference = 2 * Math.PI * 48

// ── Pipeline donut ─────────────────────────────────────────────────────────

const pipelineTotal = computed(() => {
  if (!props.analytics) return 0
  return (
    props.analytics.pipelineApplied +
    props.analytics.pipelineInterview +
    props.analytics.pipelineOffer +
    props.analytics.pipelineRejected
  )
})

const donutSegments = computed(() => {
  if (!props.analytics || pipelineTotal.value === 0) return []
  const items = [
    { key: 'applied',   label: 'Applied',   count: props.analytics.pipelineApplied },
    { key: 'interview', label: 'Interview',  count: props.analytics.pipelineInterview },
    { key: 'offer',     label: 'Offer',      count: props.analytics.pipelineOffer },
    { key: 'rejected',  label: 'Rejected',   count: props.analytics.pipelineRejected },
  ]
  // Start at top of circle: offset = circumference * 0.25
  let cumulative = 0
  const startOffset = circumference * 0.25
  return items.map((item) => {
    const fraction = item.count / pipelineTotal.value
    const dash = fraction * circumference
    const offset = startOffset - cumulative
    cumulative += dash
    return {
      ...item,
      color: PIPELINE_COLORS[item.key],
      dash,
      offset,
      pct: Math.round(fraction * 100),
    }
  })
})

const donutAriaLabel = computed(() => {
  if (!props.analytics) return ''
  const a = props.analytics
  return `Pipeline distribution: ${a.pipelineApplied} applied, ${a.pipelineInterview} interview, ${a.pipelineOffer} offer, ${a.pipelineRejected} rejected`
})

// ── Trend bars ─────────────────────────────────────────────────────────────

const trendMax = computed(() => {
  if (!props.analytics || props.analytics.trend.length === 0) return 0
  return Math.max(...props.analytics.trend.map((e) => e.count))
})

const trendBars = computed(() => {
  if (!props.analytics) return []
  const max = trendMax.value
  return props.analytics.trend.map((entry) => {
    const label = `${entry.year}-${String(entry.month).padStart(2, '0')}`
    const shortLabel = MONTH_ABBR[entry.month - 1]
    const heightPct = max > 0 ? Math.round((entry.count / max) * 100) : 0
    return { label, shortLabel, count: entry.count, heightPct }
  })
})

const trendAriaLabel = computed(() => {
  if (!props.analytics) return ''
  const parts = props.analytics.trend.map(
    (e) => `${MONTH_ABBR[e.month - 1]} ${e.year}: ${e.count}`,
  )
  return `Monthly applications: ${parts.join(', ')}`
})
</script>

<style scoped>
.analytics-section {
  display: flex;
  flex-direction: column;
}

.analytics-section__heading {
  margin-bottom: 1.25rem;
}

.analytics-loading {
  padding: 0.5rem 0;
}

.analytics-error {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0;
}

.analytics-error__msg {
  font-size: 0.875rem;
  color: var(--color-error-text);
}

.analytics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.chart-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.chart-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
}

.chart-subtitle {
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--color-text-muted);
}

.chart-empty {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  text-align: center;
  padding: 2rem 0;
}

/* ── Donut ─────────────────────────────────────────────────────────────── */

.donut-wrap {
  display: flex;
  justify-content: center;
}

.donut-svg {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.donut-total-num {
  font-size: 22px;
  font-weight: 700;
  fill: var(--color-text);
}

.donut-total-label {
  font-size: 10px;
  fill: var(--color-text-muted);
}

.donut-legend {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.donut-legend__item {
  display: grid;
  grid-template-columns: 10px 1fr auto auto;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
}

.donut-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.donut-legend__label {
  color: var(--color-text);
}

.donut-legend__count {
  font-weight: 600;
  color: var(--color-text);
  min-width: 20px;
  text-align: right;
}

.donut-legend__pct {
  color: var(--color-text-muted);
  min-width: 36px;
  text-align: right;
}

/* ── Bar chart ─────────────────────────────────────────────────────────── */

.bar-chart {
  position: relative;
  padding-top: 1.25rem;
}

.bar-chart__y-max {
  position: absolute;
  top: 0;
  left: 0;
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.bar-chart__bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 100px;
  border-bottom: 1px solid var(--color-border);
}

.bar-chart__col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  gap: 4px;
}

.bar-chart__bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
}

.bar-chart__bar {
  width: 100%;
  background: var(--color-primary);
  border-radius: 3px 3px 0 0;
  min-height: 3px;
  transition: height 0.3s ease;
}

.bar-chart__x-label {
  font-size: 0.625rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  text-align: center;
}

@media (max-width: 768px) {
  .analytics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
