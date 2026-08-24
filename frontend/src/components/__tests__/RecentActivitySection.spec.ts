import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import RecentActivitySection from '@/components/dashboard/RecentActivitySection.vue'
import type { ActivityEntry } from '@/api/dashboard'

vi.mock('vue-router', () => ({
  // oxlint-disable-next-line vitest/require-mock-type-parameters
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
}))

// ── Fixtures ───────────────────────────────────────────────────────────────

const now = new Date().toISOString()

const sampleActivity: ActivityEntry[] = [
  { type: 'RESUME_UPDATED',    label: 'Updated resume',       subLabel: 'My Resume',   linkPath: '/resumes/abc', occurredAt: now },
  { type: 'APPLICATION_ADDED', label: 'Applied to Acme Corp', subLabel: 'Engineer',    linkPath: '/applications', occurredAt: now },
  { type: 'VERSION_CREATED',   label: 'Created version v2',   subLabel: 'Tech Resume', linkPath: '/resumes/xyz', occurredAt: now },
  { type: 'PDF_EXPORTED',      label: 'Exported PDF',         subLabel: 'June 2024',   linkPath: '/resumes',     occurredAt: now },
]

function mountSection(
  activity: ActivityEntry[] = [],
  activityLoading = false,
  activityError: string | null = null,
) {
  setActivePinia(createPinia())
  return mount(RecentActivitySection, {
    props: { activity, activityLoading, activityError },
    global: { stubs: { RouterLink: { template: '<a :href="to" :aria-label="ariaLabel"><slot /></a>', props: ['to', 'ariaLabel'] } } },
  })
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('RecentActivitySection', () => {

  // ── Loading ───────────────────────────────────────────────────────────────

  it('shows loading skeleton when activityLoading is true', () => {
    const wrapper = mountSection([], true)
    expect(wrapper.find('[data-testid="activity-loading"]').exists()).toBe(true)
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
  })

  it('hides loading when not loading', () => {
    const wrapper = mountSection([])
    expect(wrapper.find('[data-testid="activity-loading"]').exists()).toBe(false)
  })

  // ── Error ─────────────────────────────────────────────────────────────────

  it('shows error message when activityError is set', () => {
    const wrapper = mountSection([], false, 'Failed to load.')
    expect(wrapper.find('[data-testid="activity-error"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Failed to load.')
  })

  it('shows retry button on error', () => {
    const wrapper = mountSection([], false, 'Error')
    expect(wrapper.find('[data-testid="activity-error"] button').text()).toBe('Retry')
  })

  it('emits retry when retry button clicked', async () => {
    const wrapper = mountSection([], false, 'Error')
    await wrapper.find('[data-testid="activity-error"] button').trigger('click')
    expect(wrapper.emitted('retry')).toBeTruthy()
  })

  // ── Empty ─────────────────────────────────────────────────────────────────

  it('shows empty state when activity is empty', () => {
    const wrapper = mountSection([])
    expect(wrapper.find('[data-testid="activity-empty"]').exists()).toBe(true)
  })

  it('does not show list when empty', () => {
    const wrapper = mountSection([])
    expect(wrapper.find('[data-testid="activity-list"]').exists()).toBe(false)
  })

  // ── Populated ─────────────────────────────────────────────────────────────

  it('renders activity list when entries exist', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.find('[data-testid="activity-list"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="activity-empty"]').exists()).toBe(false)
  })

  it('renders correct number of items', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.findAll('li').length).toBe(4)
  })

  it('renders RESUME_UPDATED item', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.find('[data-testid="activity-item-resume_updated"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="activity-item-resume_updated"]').text()).toContain('Updated resume')
  })

  it('renders APPLICATION_ADDED item', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.find('[data-testid="activity-item-application_added"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="activity-item-application_added"]').text()).toContain('Applied to Acme Corp')
  })

  it('renders VERSION_CREATED item', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.find('[data-testid="activity-item-version_created"]').exists()).toBe(true)
  })

  it('renders PDF_EXPORTED item', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.find('[data-testid="activity-item-pdf_exported"]').exists()).toBe(true)
  })

  it('shows subLabel text', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.text()).toContain('My Resume')
    expect(wrapper.text()).toContain('Engineer')
  })

  // ── Links ─────────────────────────────────────────────────────────────────

  it('activity label links to correct path', () => {
    const wrapper = mountSection([sampleActivity[0]!])
    const link = wrapper.find('a')
    expect(link.attributes('href')).toBe('/resumes/abc')
  })

  it('application activity links to /applications', () => {
    const wrapper = mountSection([sampleActivity[1]!])
    const link = wrapper.find('a')
    expect(link.attributes('href')).toBe('/applications')
  })

  // ── Accessibility ─────────────────────────────────────────────────────────

  it('section has aria-labelledby', () => {
    const wrapper = mountSection([])
    expect(wrapper.find('[data-testid="activity-section"]').attributes('aria-labelledby')).toBe('activity-heading')
  })

  it('activity list has aria-label', () => {
    const wrapper = mountSection(sampleActivity)
    expect(wrapper.find('[data-testid="activity-list"]').attributes('aria-label')).toBe('Recent activity feed')
  })

  it('time elements have datetime attribute', () => {
    const wrapper = mountSection(sampleActivity)
    const times = wrapper.findAll('time')
    expect(times.length).toBeGreaterThan(0)
    times.forEach((t) => {
      expect(t.attributes('datetime')).toBeTruthy()
    })
  })
})
