import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DashboardView from '@/views/DashboardView.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useAuthStore } from '@/stores/auth'
import type { DashboardSummary } from '@/api/dashboard'

// ── Mocks ──────────────────────────────────────────────────────────────────

vi.mock('@/api/dashboard', () => ({
  // oxlint-disable-next-line vitest/require-mock-type-parameters
  dashboardApi: {
    // oxlint-disable-next-line vitest/require-mock-type-parameters
    get: vi.fn(),
    // oxlint-disable-next-line vitest/require-mock-type-parameters
    getAnalytics: vi.fn(),
    // oxlint-disable-next-line vitest/require-mock-type-parameters
    extractError: vi.fn((err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } }
      return { message: e?.response?.data?.message ?? 'Something went wrong.' }
    }),
  },
}))

vi.mock('vue-router', () => ({
  // oxlint-disable-next-line vitest/require-mock-type-parameters
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
}))

// ── Fixtures ───────────────────────────────────────────────────────────────

const emptySummary: DashboardSummary = {
  profile: { exists: false, hasTitle: false, hasSummary: false, hasContactInfo: false, completionPercent: 0 },
  resumes: { resumeCount: 0, versionCount: 0, recentResumes: [] },
  applications: { total: 0, applied: 0, interview: 0, offer: 0, rejected: 0, recentApplications: [] },
  subscription: { tier: 'FREE', status: 'ACTIVE', provider: 'DEMO', currentPeriodStart: null, currentPeriodEnd: null },
  usage: { pdfExportsUsed: 0, pdfExportsLimit: 3, atLimit: false },
  quickActions: { canCreateResume: true, canLogApplication: true, canUpgrade: true },
}

const populatedSummary: DashboardSummary = {
  profile: { exists: true, hasTitle: true, hasSummary: true, hasContactInfo: true, completionPercent: 100 },
  resumes: {
    resumeCount: 2,
    versionCount: 4,
    recentResumes: [
      { id: 'r1', name: 'Software Engineer Resume', latestVersionNumber: 2, updatedAt: '2024-06-01T10:00:00Z' },
      { id: 'r2', name: 'Product Manager Resume', latestVersionNumber: 1, updatedAt: '2024-05-15T10:00:00Z' },
    ],
  },
  applications: {
    total: 5,
    applied: 2,
    interview: 1,
    offer: 1,
    rejected: 1,
    recentApplications: [
      { id: 'a1', companyName: 'Acme Corp', jobTitle: 'Backend Engineer', applicationDate: '2024-06-10', status: 'INTERVIEW' },
      { id: 'a2', companyName: 'Beta Inc', jobTitle: 'Frontend Dev', applicationDate: '2024-06-05', status: 'APPLIED' },
    ],
  },
  subscription: { tier: 'FREE', status: 'ACTIVE', provider: 'DEMO', currentPeriodStart: null, currentPeriodEnd: null },
  usage: { pdfExportsUsed: 2, pdfExportsLimit: 3, atLimit: false },
  quickActions: { canCreateResume: true, canLogApplication: true, canUpgrade: true },
}

const proSummary: DashboardSummary = {
  ...populatedSummary,
  subscription: { tier: 'PRO', status: 'ACTIVE', provider: 'DEMO', currentPeriodStart: '2024-06-01T00:00:00Z', currentPeriodEnd: '2024-07-01T00:00:00Z' },
  usage: { pdfExportsUsed: 0, pdfExportsLimit: 0, atLimit: false },
  quickActions: { canCreateResume: true, canLogApplication: true, canUpgrade: false },
}

let pinia: ReturnType<typeof createPinia>

function mountView() {
  return mount(DashboardView, { global: { plugins: [pinia] } })
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('DashboardView', () => {
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
    // Default stub for getAnalytics so it never hangs
    import('@/api/dashboard').then(({ dashboardApi }) => {
      vi.mocked(dashboardApi.getAnalytics).mockResolvedValue({
        pipelineApplied: 0, pipelineInterview: 0, pipelineOffer: 0, pipelineRejected: 0, trend: [],
      })
    })
  })

  // ── Loading state ─────────────────────────────────────────────────────────

  it('shows loading skeleton while fetching', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockReturnValue(new Promise(() => {}))

    const wrapper = mountView()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="dashboard-loading"]').exists()).toBe(true)
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
  })

  it('hides skeleton after load completes', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="dashboard-loading"]').exists()).toBe(false)
  })

  // ── Error state ───────────────────────────────────────────────────────────

  it('shows error message when API fails', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockRejectedValue({
      response: { data: { message: 'Server error.' } },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="dashboard-error"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Server error.')
  })

  it('shows retry button on error', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockRejectedValue({ response: { data: { message: 'Fail.' } } })

    const wrapper = mountView()
    await flushPromises()

    const retryBtn = wrapper.find('[data-testid="dashboard-error"]').find('button')
    expect(retryBtn.exists()).toBe(true)
    expect(retryBtn.text()).toBe('Retry')
  })

  it('retries load when retry button clicked', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get)
      .mockRejectedValueOnce({ response: { data: { message: 'Fail.' } } })
      .mockResolvedValueOnce(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="dashboard-error"]').find('button').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="dashboard-error"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="welcome-header"]').exists()).toBe(true)
  })

  // ── Empty state ───────────────────────────────────────────────────────────

  it('renders welcome header for empty dashboard', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="welcome-header"]').exists()).toBe(true)
    expect(wrapper.text()).toMatch(/Good (morning|afternoon|evening)/)
  })

  it('shows empty state for pipeline when no applications', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="pipeline-empty"]').exists()).toBe(true)
  })

  it('shows empty state for recent applications when none', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="recent-apps-empty"]').exists()).toBe(true)
  })

  it('shows empty state for recent resumes when none', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="recent-resumes-empty"]').exists()).toBe(true)
  })

  // ── KPI cards ─────────────────────────────────────────────────────────────

  it('renders KPI grid', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="kpi-grid"]').exists()).toBe(true)
  })

  it('shows correct application count in KPI', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="kpi-applications"]').text()).toContain('5')
  })

  it('shows correct interview count in KPI', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="kpi-interviews"]').text()).toContain('1')
  })

  it('shows correct offer count in KPI', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="kpi-offers"]').text()).toContain('1')
  })

  it('shows correct resume count in KPI', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="kpi-resumes"]').text()).toContain('2')
  })

  // ── Application pipeline ──────────────────────────────────────────────────

  it('renders pipeline rows when applications exist', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="pipeline-applied"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="pipeline-interview"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="pipeline-offer"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="pipeline-rejected"]').exists()).toBe(true)
  })

  it('pipeline bars have progressbar role', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    const bars = wrapper.find('[data-testid="pipeline-section"]').findAll('[role="progressbar"]')
    expect(bars.length).toBeGreaterThan(0)
  })

  // ── Recent applications ───────────────────────────────────────────────────

  it('renders recent applications list', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    const list = wrapper.find('[data-testid="recent-apps-list"]')
    expect(list.exists()).toBe(true)
    expect(list.findAll('li').length).toBe(2)
  })

  it('shows company name in recent applications', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="recent-apps-list"]').text()).toContain('Acme Corp')
  })

  it('shows status badge in recent applications', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="recent-apps-section"]').text()).toContain('Interview')
  })

  // ── Subscription card ─────────────────────────────────────────────────────

  it('shows FREE tier for free user', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="sub-tier"]').text()).toBe('Free')
  })

  it('shows PRO tier for pro user', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(proSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="sub-tier"]').text()).toBe('Pro')
  })

  it('shows usage section for free user', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="usage-section"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('2 / 3')
  })

  it('shows pro perks for pro user', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(proSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="pro-perks"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Unlimited PDF exports')
  })

  it('shows at-limit warning when exports exhausted', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue({
      ...populatedSummary,
      usage: { pdfExportsUsed: 3, pdfExportsLimit: 3, atLimit: true },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Monthly limit reached')
  })

  // ── Quick actions ─────────────────────────────────────────────────────────

  it('shows quick actions section', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="quick-actions-section"]').exists()).toBe(true)
  })

  it('shows create resume action when canCreateResume is true', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="action-create-resume"]').exists()).toBe(true)
  })

  it('shows resume limit message when canCreateResume is false', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue({
      ...emptySummary,
      quickActions: { canCreateResume: false, canLogApplication: true, canUpgrade: true },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="action-resume-limit"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="action-create-resume"]').exists()).toBe(false)
  })

  it('shows track application action', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="action-track-application"]').exists()).toBe(true)
  })

  it('shows upgrade action for free user', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="action-upgrade"]').exists()).toBe(true)
  })

  it('hides upgrade action for pro user', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(proSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="action-upgrade"]').exists()).toBe(false)
  })

  // ── Welcome with name ─────────────────────────────────────────────────────

  it('shows first name in greeting when available', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    const authStore = useAuthStore()
    authStore.firstName = 'Jane'
    await flushPromises()

    expect(wrapper.find('[data-testid="welcome-header"]').text()).toContain('Jane')
  })

  it('shows greeting without name when firstName is null', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    const authStore = useAuthStore()
    authStore.firstName = null
    await flushPromises()

    expect(wrapper.find('[data-testid="welcome-header"]').find('h1').text()).toMatch(/Good (morning|afternoon|evening)$/)
  })

  // ── Profile nudge ─────────────────────────────────────────────────────────

  it('shows profile nudge when profile is incomplete', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="profile-nudge"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('0% complete')
  })

  it('hides profile nudge when profile is 100% complete', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="profile-nudge"]').exists()).toBe(false)
  })

  // ── Accessibility ─────────────────────────────────────────────────────────

  it('pipeline section has aria-labelledby', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[aria-labelledby="pipeline-heading"]').exists()).toBe(true)
  })

  it('usage progressbar has correct aria attributes', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    const bar = wrapper.find('[data-testid="usage-section"] [role="progressbar"]')
    expect(bar.attributes('aria-valuenow')).toBe('2')
    expect(bar.attributes('aria-valuemax')).toBe('3')
    expect(bar.attributes('aria-valuemin')).toBe('0')
  })

  it('recent applications list has aria-label', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(populatedSummary)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[aria-label="Recent applications"]').exists()).toBe(true)
  })

  // ── Store integration ─────────────────────────────────────────────────────

  it('calls loadDashboard and loadAnalytics on mount', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)
    vi.mocked(dashboardApi.getAnalytics).mockResolvedValue({
      pipelineApplied: 0, pipelineInterview: 0, pipelineOffer: 0, pipelineRejected: 0, trend: [],
    })

    mountView()
    await flushPromises()

    expect(dashboardApi.get).toHaveBeenCalledOnce()
    expect(dashboardApi.getAnalytics).toHaveBeenCalledOnce()
  })

  it('renders analytics section when summary is loaded', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockResolvedValue(emptySummary)
    vi.mocked(dashboardApi.getAnalytics).mockResolvedValue({
      pipelineApplied: 0, pipelineInterview: 0, pipelineOffer: 0, pipelineRejected: 0, trend: [],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="analytics-section"]').exists()).toBe(true)
  })

  it('store error is shown in error state', async () => {
    const { dashboardApi } = await import('@/api/dashboard')
    vi.mocked(dashboardApi.get).mockRejectedValue({ response: { data: { message: 'Unauthorized.' } } })

    const wrapper = mountView()
    await flushPromises()

    const store = useDashboardStore()
    expect(store.error).toBeTruthy()
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  })
})
