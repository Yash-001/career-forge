import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import BillingView from '@/views/BillingView.vue'
import { useBillingStore } from '@/stores/billing'
import type { SubscriptionResponse } from '@/api/billing'

// ── Mocks ──────────────────────────────────────────────────────────────────

vi.mock('@/api/billing', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  billingApi: {
    getSubscription: vi.fn(),
    checkout: vi.fn(),
    cancel: vi.fn(),
    extractError: vi.fn((err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } }
      return { status: 500, code: 'ERROR', message: e?.response?.data?.message ?? 'Something went wrong.' }
    }),
  },
}))

vi.mock('vue-router', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  RouterLink: { template: '<a><slot /></a>' },
}))

// ── Fixtures ───────────────────────────────────────────────────────────────

const freeSub: SubscriptionResponse = {
  tier: 'FREE',
  status: 'ACTIVE',
  provider: 'DEMO',
  currentPeriodStart: null,
  currentPeriodEnd: null,
  pdfExportsUsed: 1,
  pdfExportsLimit: 3,
}

const proSub: SubscriptionResponse = {
  tier: 'PRO',
  status: 'ACTIVE',
  provider: 'DEMO',
  currentPeriodStart: '2025-01-01T00:00:00Z',
  currentPeriodEnd: '2025-02-01T00:00:00Z',
  pdfExportsUsed: null,
  pdfExportsLimit: null,
}

let pinia: ReturnType<typeof createPinia>

function mountView() {
  return mount(BillingView, { global: { plugins: [pinia] } })
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('BillingView', () => {
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  // ── Loading state ─────────────────────────────────────────────────────────

  it('shows loading skeleton while fetching', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockReturnValue(new Promise(() => {}))

    const wrapper = mountView()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
    expect(wrapper.find('[aria-label="Loading subscription"]').exists()).toBe(true)
  })

  it('hides skeleton after load completes', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(false)
  })

  // ── Free plan display ─────────────────────────────────────────────────────

  it('displays Free plan name for free user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Free')
    expect(wrapper.text()).toContain('Current Plan')
  })

  it('shows Active status badge for free user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Active')
  })

  it('shows PDF usage for free user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('1 / 3')
    expect(wrapper.text()).toContain('2 exports remaining this month')
  })

  it('shows usage at limit warning when exports exhausted', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue({ ...freeSub, pdfExportsUsed: 3 })

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Monthly limit reached')
  })

  it('shows usage progress bar for free user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[role="progressbar"]').exists()).toBe(true)
  })

  it('shows Upgrade to Pro CTA for free user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="upgrade-btn"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Upgrade to Pro')
  })

  it('does not show cancel button for free user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="cancel-btn"]').exists()).toBe(false)
  })

  // ── Pro plan display ──────────────────────────────────────────────────────

  it('displays Pro plan name for pro user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Pro')
    expect(wrapper.text()).toContain('Current Plan')
  })

  it('shows unlimited PDF exports for pro user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Unlimited PDF exports')
  })

  it('does not show usage bar for pro user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[role="progressbar"]').exists()).toBe(false)
  })

  it('shows cancel subscription button for pro user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="cancel-btn"]').exists()).toBe(true)
  })

  it('does not show upgrade CTA for pro user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="upgrade-btn"]').exists()).toBe(false)
  })

  it('shows billing period end date for pro user', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Billing period ends')
  })

  // ── Demo provider notice ──────────────────────────────────────────────────

  it('shows demo billing notice for DEMO provider', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="demo-notice"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Demo billing')
    expect(wrapper.text()).toContain('no real payment is processed')
  })

  // ── Upgrade ───────────────────────────────────────────────────────────────

  it('calls checkout and reloads subscription on upgrade', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValueOnce(freeSub).mockResolvedValueOnce(proSub)
    vi.mocked(billingApi.checkout).mockResolvedValue({
      action: 'UPGRADED',
      tier: 'PRO',
      status: 'ACTIVE',
      message: 'Upgraded.',
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="upgrade-btn"]').trigger('click')
    await flushPromises()

    expect(billingApi.checkout).toHaveBeenCalledOnce()
    expect(billingApi.getSubscription).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-testid="upgrade-btn"]').exists()).toBe(false)
  })

  it('shows loading state during upgrade', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)
    vi.mocked(billingApi.checkout).mockReturnValue(new Promise(() => {}))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="upgrade-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-testid="upgrade-btn"]')
    expect(btn.attributes('disabled')).toBeDefined()
    expect(btn.text()).toContain('Upgrading')
  })

  it('shows error when upgrade fails', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)
    vi.mocked(billingApi.checkout).mockRejectedValue({
      response: { status: 409, data: { code: 'ALREADY_PRO', message: 'Already Pro.' } },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="upgrade-btn"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="action-error"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Already Pro.')
  })

  // ── Cancellation ──────────────────────────────────────────────────────────

  it('opens cancel confirmation dialog when cancel button clicked', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="cancel-dialog"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Cancel Subscription?')
  })

  it('closes dialog when Keep Pro is clicked', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    const keepBtn = wrapper.findAll('button').find((b) => b.text() === 'Keep Pro')
    await keepBtn?.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="cancel-dialog"]').exists()).toBe(false)
  })

  it('calls cancel API and reloads subscription on confirm', async () => {
    const { billingApi } = await import('@/api/billing')
    const canceledSub: SubscriptionResponse = { ...proSub, tier: 'FREE', status: 'CANCELED' }
    vi.mocked(billingApi.getSubscription).mockResolvedValueOnce(proSub).mockResolvedValueOnce(canceledSub)
    vi.mocked(billingApi.cancel).mockResolvedValue(canceledSub)

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-testid="confirm-cancel-btn"]').trigger('click')
    await flushPromises()

    expect(billingApi.cancel).toHaveBeenCalledOnce()
    expect(billingApi.getSubscription).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-testid="cancel-dialog"]').exists()).toBe(false)
  })

  it('shows loading state during cancellation', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)
    vi.mocked(billingApi.cancel).mockReturnValue(new Promise(() => {}))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-testid="confirm-cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    const btn = wrapper.find('[data-testid="confirm-cancel-btn"]')
    expect(btn.attributes('disabled')).toBeDefined()
    expect(btn.text()).toContain('Canceling')
  })

  it('shows error when cancellation fails', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)
    vi.mocked(billingApi.cancel).mockRejectedValue({
      response: { status: 400, data: { code: 'NO_ACTIVE_SUBSCRIPTION', message: 'No active subscription.' } },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="confirm-cancel-btn"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="action-error"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('No active subscription.')
  })

  // ── Error state ───────────────────────────────────────────────────────────

  it('shows error when subscription load fails', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockRejectedValue({
      response: { status: 401, data: { code: 'UNAUTHORIZED', message: 'Authentication expired.' } },
    })

    const wrapper = mountView()
    await flushPromises()

    const store = useBillingStore()
    store.error = 'Authentication expired.'
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  })

  it('shows provider error message', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)
    vi.mocked(billingApi.checkout).mockRejectedValue({
      response: { status: 502, data: { code: 'BILLING_PROVIDER_ERROR', message: 'Provider unavailable.' } },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="upgrade-btn"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="action-error"]').text()).toContain('Provider unavailable.')
  })

  // ── Accessibility ─────────────────────────────────────────────────────────

  it('has aria-labelledby on plan section', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[aria-labelledby="plan-heading"]').exists()).toBe(true)
  })

  it('has aria-label on PDF usage section', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[aria-label="PDF export usage"]').exists()).toBe(true)
  })

  it('progress bar has correct aria attributes', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)

    const wrapper = mountView()
    await flushPromises()
    const bar = wrapper.find('[role="progressbar"]')
    expect(bar.attributes('aria-valuenow')).toBe('1')
    expect(bar.attributes('aria-valuemax')).toBe('3')
    expect(bar.attributes('aria-valuemin')).toBe('0')
  })

  it('cancel dialog has aria-modal and aria-labelledby', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(proSub)

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    const dialog = wrapper.find('[data-testid="cancel-dialog"]')
    expect(dialog.attributes('aria-modal')).toBe('true')
    expect(dialog.attributes('aria-labelledby')).toBe('cancel-dialog-title')
  })

  it('upgrade button has aria-busy during upgrade', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue(freeSub)
    vi.mocked(billingApi.checkout).mockReturnValue(new Promise(() => {}))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="upgrade-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="upgrade-btn"]').attributes('aria-busy')).toBe('true')
  })
})
