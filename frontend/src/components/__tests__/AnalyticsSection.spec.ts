import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AnalyticsSection from '@/components/dashboard/AnalyticsSection.vue'
import type { AnalyticsSummary } from '@/api/dashboard'

// ── Fixtures ───────────────────────────────────────────────────────────────

const emptyAnalytics: AnalyticsSummary = {
  pipelineApplied: 0,
  pipelineInterview: 0,
  pipelineOffer: 0,
  pipelineRejected: 0,
  trend: [],
}

const populatedAnalytics: AnalyticsSummary = {
  pipelineApplied: 4,
  pipelineInterview: 2,
  pipelineOffer: 1,
  pipelineRejected: 3,
  trend: [
    { year: 2024, month: 4, count: 2 },
    { year: 2024, month: 5, count: 5 },
    { year: 2024, month: 6, count: 3 },
  ],
}

function mountSection(
  analytics: AnalyticsSummary | null,
  analyticsLoading = false,
  analyticsError: string | null = null,
) {
  setActivePinia(createPinia())
  return mount(AnalyticsSection, {
    props: { analytics, analyticsLoading, analyticsError },
  })
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('AnalyticsSection', () => {

  // ── Loading state ─────────────────────────────────────────────────────────

  it('shows loading skeleton when analyticsLoading is true', () => {
    const wrapper = mountSection(null, true)
    expect(wrapper.find('[data-testid="analytics-loading"]').exists()).toBe(true)
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
  })

  it('hides loading skeleton when not loading', () => {
    const wrapper = mountSection(emptyAnalytics, false)
    expect(wrapper.find('[data-testid="analytics-loading"]').exists()).toBe(false)
  })

  // ── Error state ───────────────────────────────────────────────────────────

  it('shows error message when analyticsError is set', () => {
    const wrapper = mountSection(null, false, 'Failed to load analytics.')
    expect(wrapper.find('[data-testid="analytics-error"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Failed to load analytics.')
  })

  it('shows retry button on error', () => {
    const wrapper = mountSection(null, false, 'Error')
    const btn = wrapper.find('[data-testid="analytics-error"] button')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toBe('Retry')
  })

  it('emits retry event when retry button clicked', async () => {
    const wrapper = mountSection(null, false, 'Error')
    await wrapper.find('[data-testid="analytics-error"] button').trigger('click')
    expect(wrapper.emitted('retry')).toBeTruthy()
  })

  // ── Empty state ───────────────────────────────────────────────────────────

  it('shows pipeline empty state when no applications', () => {
    const wrapper = mountSection(emptyAnalytics)
    expect(wrapper.find('[data-testid="pipeline-chart-empty"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('No applications yet')
  })

  it('shows trend empty state when no trend data', () => {
    const wrapper = mountSection(emptyAnalytics)
    expect(wrapper.find('[data-testid="trend-chart-empty"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('No data yet')
  })

  // ── Pipeline chart — populated ────────────────────────────────────────────

  it('renders pipeline chart when data exists', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('[data-testid="pipeline-chart"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="pipeline-chart-empty"]').exists()).toBe(false)
  })

  it('renders SVG donut chart', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('svg').exists()).toBe(true)
  })

  it('renders legend items for all four statuses', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('[data-testid="legend-applied"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="legend-interview"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="legend-offer"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="legend-rejected"]').exists()).toBe(true)
  })

  it('shows correct count in applied legend item', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('[data-testid="legend-applied"]').text()).toContain('4')
  })

  it('shows correct count in rejected legend item', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('[data-testid="legend-rejected"]').text()).toContain('3')
  })

  it('donut has accessible aria-label', () => {
    const wrapper = mountSection(populatedAnalytics)
    const donut = wrapper.find('[role="img"]')
    expect(donut.exists()).toBe(true)
    expect(donut.attributes('aria-label')).toContain('4')
    expect(donut.attributes('aria-label')).toContain('applied')
  })

  it('shows total in donut centre', () => {
    const wrapper = mountSection(populatedAnalytics)
    // total = 4+2+1+3 = 10
    expect(wrapper.find('[data-testid="pipeline-chart"]').text()).toContain('10')
  })

  // ── Trend chart — populated ───────────────────────────────────────────────

  it('renders trend bars when trend data exists', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('[data-testid="trend-bars"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trend-chart-empty"]').exists()).toBe(false)
  })

  it('renders correct number of bar columns', () => {
    const wrapper = mountSection(populatedAnalytics)
    const bars = wrapper.find('[data-testid="trend-bars"]').findAll('[class*="bar-chart__col"]')
    expect(bars.length).toBe(3)
  })

  it('renders bar for each trend entry by label', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('[data-testid="trend-bar-2024-04"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trend-bar-2024-05"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trend-bar-2024-06"]').exists()).toBe(true)
  })

  it('trend chart has accessible aria-label', () => {
    const wrapper = mountSection(populatedAnalytics)
    const chart = wrapper.find('[data-testid="trend-bars"]')
    expect(chart.attributes('aria-label')).toContain('Apr')
    expect(chart.attributes('aria-label')).toContain('May')
    expect(chart.attributes('aria-label')).toContain('Jun')
  })

  it('shows y-axis max value', () => {
    const wrapper = mountSection(populatedAnalytics)
    // max count is 5
    expect(wrapper.find('[data-testid="trend-chart"]').text()).toContain('5')
  })

  // ── Section structure ─────────────────────────────────────────────────────

  it('renders analytics section with aria-labelledby', () => {
    const wrapper = mountSection(emptyAnalytics)
    expect(wrapper.find('[data-testid="analytics-section"]').attributes('aria-labelledby')).toBe('analytics-heading')
  })

  it('renders both chart cards', () => {
    const wrapper = mountSection(populatedAnalytics)
    expect(wrapper.find('[data-testid="pipeline-chart"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="trend-chart"]').exists()).toBe(true)
  })
})
