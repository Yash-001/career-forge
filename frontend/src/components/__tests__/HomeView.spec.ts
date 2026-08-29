import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import HomeView from '@/views/HomeView.vue'

vi.mock('vue-router', () => ({
  useRouter: vi.fn<() => { replace: ReturnType<typeof vi.fn> }>(() => ({ replace: vi.fn<() => void>() })),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn<() => void>(),
  register: vi.fn<() => void>(),
}))

function mountHome() {
  return mount(HomeView, { global: { plugins: [createPinia()] } })
}

describe('HomeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the application name', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('CareerForge')
  })

  it('renders the hero headline', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Build better careers.')
    expect(wrapper.text()).toContain('Apply smarter.')
  })

  it('renders primary CTA', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Start for free')
  })

  it('renders secondary CTA', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Explore demo')
  })

  it('renders all seven feature cards', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Master Profile')
    expect(wrapper.text()).toContain('Resume Builder')
    expect(wrapper.text()).toContain('AI Tailoring')
    expect(wrapper.text()).toContain('ATS PDF Export')
    expect(wrapper.text()).toContain('Application Tracker')
    expect(wrapper.text()).toContain('Dashboard Analytics')
    expect(wrapper.text()).toContain('Free / Pro Billing')
  })

  it('renders how-it-works steps', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Build your profile')
    expect(wrapper.text()).toContain('Create and tailor your resume')
    expect(wrapper.text()).toContain('Apply and track results')
    expect(wrapper.text()).toContain('Improve your job search')
  })

  it('renders pricing tiers', () => {
    const wrapper = mountHome()
    expect(wrapper.find('[data-testid="pricing-free"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="pricing-pro"]').exists()).toBe(true)
  })

  it('renders free tier limits', () => {
    const wrapper = mountHome()
    const freeCard = wrapper.find('[data-testid="pricing-free"]')
    expect(freeCard.text()).toContain('2 saved resumes')
    expect(freeCard.text()).toContain('3 PDF exports per month')
  })

  it('renders pro tier unlimited features', () => {
    const wrapper = mountHome()
    const proCard = wrapper.find('[data-testid="pricing-pro"]')
    expect(proCard.text()).toContain('Unlimited saved resumes')
    expect(proCard.text()).toContain('Unlimited PDF exports')
  })

  it('renders demo billing notice', () => {
    const wrapper = mountHome()
    expect(wrapper.find('[data-testid="demo-billing-notice"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('DemoBillingProvider')
  })

  it('renders trust/engineering section', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('JWT authentication')
    expect(wrapper.text()).toContain('Spring Boot backend')
    expect(wrapper.text()).toContain('Vue 3 + TypeScript frontend')
    expect(wrapper.text()).toContain('Server-side PDF generation')
    expect(wrapper.text()).toContain('Provider abstraction')
    expect(wrapper.text()).toContain('Docker Compose')
  })

  it('renders final CTA', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Create your first resume')
  })

  it('renders footer navigation links', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('GitHub')
    expect(wrapper.text()).toContain('Login')
    expect(wrapper.text()).toContain('Register')
  })

  it('redirects authenticated users to dashboard', async () => {
    const { useRouter } = await import('vue-router')
    const replaceMock = vi.fn<() => void>()
    vi.mocked(useRouter).mockReturnValue({ replace: replaceMock } as unknown as ReturnType<typeof useRouter>)

    const pinia = createPinia()
    setActivePinia(pinia)
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    auth.setTokens('fake-token', 'fake-refresh', 'Test')

    mount(HomeView, { global: { plugins: [pinia] } })
    expect(replaceMock).toHaveBeenCalledWith('/dashboard')
  })
})
