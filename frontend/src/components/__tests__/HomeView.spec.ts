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
    expect(wrapper.text()).toContain('past the ATS')
  })

  it('renders Get started free CTA', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Get started free')
  })

  it('renders feature cards', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Resume Builder')
    expect(wrapper.text()).toContain('AI Tailoring')
    expect(wrapper.text()).toContain('Application Tracker')
  })

  it('renders how-it-works steps', () => {
    const wrapper = mountHome()
    expect(wrapper.text()).toContain('Build your master profile')
  })
})
