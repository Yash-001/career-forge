import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// oxlint-disable vitest/require-mock-type-parameters

// ── Shared mocks ───────────────────────────────────────────────────────────

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn(), replace: vi.fn() })),
  RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
  useRoute: vi.fn(() => ({ params: { resumeId: 'r1' }, query: {} })),
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
}))

vi.mock('@/api/demo', () => ({
  demoLogin: vi.fn(),
}))

vi.mock('@/api/profile', () => ({
  profileApi: {
    getProfile: vi.fn().mockResolvedValue(null),
    upsertProfile: vi.fn().mockResolvedValue({}),
    getExperiences: vi.fn().mockResolvedValue([]),
    createExperience: vi.fn().mockResolvedValue({}),
    updateExperience: vi.fn().mockResolvedValue({}),
    deleteExperience: vi.fn().mockResolvedValue(undefined),
    getEducations: vi.fn().mockResolvedValue([]),
    createEducation: vi.fn().mockResolvedValue({}),
    updateEducation: vi.fn().mockResolvedValue({}),
    deleteEducation: vi.fn().mockResolvedValue(undefined),
    getSkills: vi.fn().mockResolvedValue([]),
    createSkill: vi.fn().mockResolvedValue({}),
    updateSkill: vi.fn().mockResolvedValue({}),
    deleteSkill: vi.fn().mockResolvedValue(undefined),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Error.' })),
  },
}))

vi.mock('@/api/resume', () => ({
  resumeApi: {
    listResumes: vi.fn().mockResolvedValue([]),
    getResume: vi.fn().mockResolvedValue({ id: 'r1', name: 'My Resume', latestVersion: { id: 'v1' } }),
    getVersion: vi.fn().mockResolvedValue({ id: 'v1', versionNumber: 1, isLatest: true, title: null, professionalSummary: null, experiences: [], educations: [], skills: [] }),
    listVersions: vi.fn().mockResolvedValue([]),
    createResume: vi.fn().mockResolvedValue({ id: 'r1' }),
    renameResume: vi.fn().mockResolvedValue({}),
    deleteResume: vi.fn().mockResolvedValue(undefined),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Error.' })),
  },
}))

vi.mock('@/api/billing', () => ({
  billingApi: {
    getSubscription: vi.fn().mockResolvedValue({
      tier: 'FREE', status: 'ACTIVE', provider: 'DEMO',
      currentPeriodStart: null, currentPeriodEnd: null,
      pdfExportsUsed: 1, pdfExportsLimit: 3,
    }),
    checkout: vi.fn(),
    cancel: vi.fn(),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Error.' })),
  },
}))

vi.mock('@/api/application', () => ({
  applicationApi: {
    list: vi.fn().mockResolvedValue([]),
    create: vi.fn().mockResolvedValue({}),
    update: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue(undefined),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Error.' })),
  },
}))

function pinia() {
  const p = createPinia()
  setActivePinia(p)
  return p
}

// ── ConfirmDialog ──────────────────────────────────────────────────────────

describe('ConfirmDialog — accessibility', () => {
  it('has role=dialog and aria-modal=true when open', async () => {
    const { default: ConfirmDialog } = await import('@/components/profile/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { open: true, title: 'Delete Item', message: 'Are you sure?' },
    })
    const overlay = wrapper.find('[role="dialog"]')
    expect(overlay.exists()).toBe(true)
    expect(overlay.attributes('aria-modal')).toBe('true')
  })

  it('aria-labelledby points to the title element', async () => {
    const { default: ConfirmDialog } = await import('@/components/profile/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { open: true, title: 'Delete Item', message: 'Are you sure?' },
    })
    const overlay = wrapper.find('[role="dialog"]')
    const labelId = overlay.attributes('aria-labelledby')
    expect(labelId).toBeTruthy()
    expect(wrapper.find(`#${labelId}`).text()).toBe('Delete Item')
  })

  it('aria-describedby points to the message element', async () => {
    const { default: ConfirmDialog } = await import('@/components/profile/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { open: true, title: 'Delete Item', message: 'Are you sure?' },
    })
    const overlay = wrapper.find('[role="dialog"]')
    const descId = overlay.attributes('aria-describedby')
    expect(descId).toBeTruthy()
    expect(wrapper.find(`#${descId}`).text()).toBe('Are you sure?')
  })

  it('emits cancel on Escape keydown', async () => {
    const { default: ConfirmDialog } = await import('@/components/profile/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { open: true, title: 'Delete Item', message: 'Are you sure?' },
    })
    await wrapper.find('[role="dialog"]').trigger('keydown.esc')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('dialog box has tabindex=-1 for programmatic focus', async () => {
    const { default: ConfirmDialog } = await import('@/components/profile/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { open: true, title: 'Delete Item', message: 'Are you sure?' },
    })
    expect(wrapper.find('.dialog-box').attributes('tabindex')).toBe('-1')
  })

  it('does not render when open=false', async () => {
    const { default: ConfirmDialog } = await import('@/components/profile/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { open: false, title: 'Delete Item', message: 'Are you sure?' },
    })
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })

  it('shows error with role=alert when error prop is set', async () => {
    const { default: ConfirmDialog } = await import('@/components/profile/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { open: true, title: 'Delete Item', message: 'Are you sure?', error: 'Delete failed.' },
    })
    const alert = wrapper.find('[role="alert"]')
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toBe('Delete failed.')
  })
})

// ── LoginView ──────────────────────────────────────────────────────────────

describe('LoginView — accessibility', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('email input has aria-invalid when validation fails', async () => {
    const { default: LoginView } = await import('@/views/LoginView.vue')
    const wrapper = mount(LoginView, { global: { plugins: [pinia()] } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    const emailInput = wrapper.find('#email')
    expect(emailInput.attributes('aria-invalid')).toBe('true')
  })

  it('email input has aria-describedby pointing to error span', async () => {
    const { default: LoginView } = await import('@/views/LoginView.vue')
    const wrapper = mount(LoginView, { global: { plugins: [pinia()] } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    const emailInput = wrapper.find('#email')
    const descId = emailInput.attributes('aria-describedby')
    expect(descId).toBeTruthy()
    expect(wrapper.find(`#${descId}`).exists()).toBe(true)
  })

  it('password input has aria-invalid when validation fails', async () => {
    const { default: LoginView } = await import('@/views/LoginView.vue')
    const wrapper = mount(LoginView, { global: { plugins: [pinia()] } })
    await wrapper.find('#email').setValue('user@example.com')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('#password').attributes('aria-invalid')).toBe('true')
  })

  it('field error spans have role=alert', async () => {
    const { default: LoginView } = await import('@/views/LoginView.vue')
    const wrapper = mount(LoginView, { global: { plugins: [pinia()] } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    const alerts = wrapper.findAll('[role="alert"]')
    expect(alerts.length).toBeGreaterThan(0)
  })

  it('email label is associated with email input via for/id', async () => {
    const { default: LoginView } = await import('@/views/LoginView.vue')
    const wrapper = mount(LoginView, { global: { plugins: [pinia()] } })
    const label = wrapper.findAll('label').find((l) => l.text().includes('Email'))
    expect(label?.attributes('for')).toBe('email')
    expect(wrapper.find('#email').exists()).toBe(true)
  })
})

// ── RegisterView ───────────────────────────────────────────────────────────

describe('RegisterView — accessibility', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('firstName input has aria-invalid when blank', async () => {
    const { default: RegisterView } = await import('@/views/RegisterView.vue')
    const wrapper = mount(RegisterView, { global: { plugins: [pinia()] } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('#firstName').attributes('aria-invalid')).toBe('true')
  })

  it('firstName error span has role=alert', async () => {
    const { default: RegisterView } = await import('@/views/RegisterView.vue')
    const wrapper = mount(RegisterView, { global: { plugins: [pinia()] } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    const alerts = wrapper.findAll('[role="alert"]')
    expect(alerts.length).toBeGreaterThan(0)
  })

  it('firstName input has aria-describedby pointing to error', async () => {
    const { default: RegisterView } = await import('@/views/RegisterView.vue')
    const wrapper = mount(RegisterView, { global: { plugins: [pinia()] } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    const input = wrapper.find('#firstName')
    const descId = input.attributes('aria-describedby')
    expect(descId).toBeTruthy()
    expect(wrapper.find(`#${descId}`).exists()).toBe(true)
  })
})

// ── ExperienceForm ─────────────────────────────────────────────────────────

describe('ExperienceForm — accessibility', () => {
  it('company input has aria-required=true', async () => {
    const { default: ExperienceForm } = await import('@/components/profile/ExperienceForm.vue')
    const wrapper = mount(ExperienceForm, { props: { submitLabel: 'Add' } })
    expect(wrapper.find('#exp-company').attributes('aria-required')).toBe('true')
  })

  it('company input has aria-invalid when blank on submit', async () => {
    const { default: ExperienceForm } = await import('@/components/profile/ExperienceForm.vue')
    const wrapper = mount(ExperienceForm, { props: { submitLabel: 'Add' } })
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('#exp-company').attributes('aria-invalid')).toBe('true')
  })

  it('company error span has role=alert', async () => {
    const { default: ExperienceForm } = await import('@/components/profile/ExperienceForm.vue')
    const wrapper = mount(ExperienceForm, { props: { submitLabel: 'Add' } })
    await wrapper.find('form').trigger('submit')
    const alert = wrapper.find('[role="alert"]')
    expect(alert.exists()).toBe(true)
  })

  it('company input aria-describedby points to error span', async () => {
    const { default: ExperienceForm } = await import('@/components/profile/ExperienceForm.vue')
    const wrapper = mount(ExperienceForm, { props: { submitLabel: 'Add' } })
    await wrapper.find('form').trigger('submit')
    const input = wrapper.find('#exp-company')
    const descId = input.attributes('aria-describedby')
    expect(descId).toBeTruthy()
    expect(wrapper.find(`#${descId}`).exists()).toBe(true)
  })

  it('start date input has aria-required=true', async () => {
    const { default: ExperienceForm } = await import('@/components/profile/ExperienceForm.vue')
    const wrapper = mount(ExperienceForm, { props: { submitLabel: 'Add' } })
    expect(wrapper.find('#exp-start').attributes('aria-required')).toBe('true')
  })

  it('apiError div has role=alert', async () => {
    const { default: ExperienceForm } = await import('@/components/profile/ExperienceForm.vue')
    const wrapper = mount(ExperienceForm, { props: { submitLabel: 'Add', apiError: 'Server error.' } })
    expect(wrapper.find('[role="alert"]').text()).toContain('Server error.')
  })
})

// ── SkillForm ──────────────────────────────────────────────────────────────

describe('SkillForm — accessibility', () => {
  it('skill name input has aria-required=true', async () => {
    const { default: SkillForm } = await import('@/components/profile/SkillForm.vue')
    const wrapper = mount(SkillForm, { props: { submitLabel: 'Add' } })
    expect(wrapper.find('#skill-name').attributes('aria-required')).toBe('true')
  })

  it('skill name input has aria-invalid when blank on submit', async () => {
    const { default: SkillForm } = await import('@/components/profile/SkillForm.vue')
    const wrapper = mount(SkillForm, { props: { submitLabel: 'Add' } })
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('#skill-name').attributes('aria-invalid')).toBe('true')
  })
})

// ── EducationForm ──────────────────────────────────────────────────────────

describe('EducationForm — accessibility', () => {
  it('institution input has aria-required=true', async () => {
    const { default: EducationForm } = await import('@/components/profile/EducationForm.vue')
    const wrapper = mount(EducationForm, { props: { submitLabel: 'Add' } })
    expect(wrapper.find('#edu-institution').attributes('aria-required')).toBe('true')
  })

  it('institution input has aria-invalid when blank on submit', async () => {
    const { default: EducationForm } = await import('@/components/profile/EducationForm.vue')
    const wrapper = mount(EducationForm, { props: { submitLabel: 'Add' } })
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('#edu-institution').attributes('aria-invalid')).toBe('true')
  })
})

// ── CreateResumeView ───────────────────────────────────────────────────────

describe('CreateResumeView — accessibility', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('active step has aria-current=step', async () => {
    const { default: CreateResumeView } = await import('@/views/CreateResumeView.vue')
    const wrapper = mount(CreateResumeView, { global: { plugins: [pinia()] } })
    const activeStep = wrapper.find('[aria-current="step"]')
    expect(activeStep.exists()).toBe(true)
    expect(activeStep.text()).toContain('Name your resume')
  })

  it('inactive step does not have aria-current', async () => {
    const { default: CreateResumeView } = await import('@/views/CreateResumeView.vue')
    const wrapper = mount(CreateResumeView, { global: { plugins: [pinia()] } })
    const steps = wrapper.findAll('.step')
    const inactiveStep = steps.find((s) => !s.attributes('aria-current'))
    expect(inactiveStep).toBeTruthy()
  })

  it('step numbers are aria-hidden', async () => {
    const { default: CreateResumeView } = await import('@/views/CreateResumeView.vue')
    const wrapper = mount(CreateResumeView, { global: { plugins: [pinia()] } })
    const stepNums = wrapper.findAll('.step-num')
    stepNums.forEach((n) => expect(n.attributes('aria-hidden')).toBe('true'))
  })
})

// ── AppHeader ──────────────────────────────────────────────────────────────

describe('AppHeader — accessibility', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('hamburger button has aria-controls pointing to mobile nav id', async () => {
    const { default: AppHeader } = await import('@/components/AppHeader.vue')
    const wrapper = mount(AppHeader, { global: { plugins: [pinia()] } })
    const toggle = wrapper.find('.app-header__menu-toggle')
    expect(toggle.attributes('aria-controls')).toBe('mobile-nav')
  })

  it('hamburger button has aria-expanded=false initially', async () => {
    const { default: AppHeader } = await import('@/components/AppHeader.vue')
    const wrapper = mount(AppHeader, { global: { plugins: [pinia()] } })
    expect(wrapper.find('.app-header__menu-toggle').attributes('aria-expanded')).toBe('false')
  })

  it('hamburger button aria-expanded becomes true when menu opens', async () => {
    const { default: AppHeader } = await import('@/components/AppHeader.vue')
    const wrapper = mount(AppHeader, { global: { plugins: [pinia()] } })
    await wrapper.find('.app-header__menu-toggle').trigger('click')
    expect(wrapper.find('.app-header__menu-toggle').attributes('aria-expanded')).toBe('true')
  })

  it('mobile nav has id=mobile-nav when open', async () => {
    const { default: AppHeader } = await import('@/components/AppHeader.vue')
    const wrapper = mount(AppHeader, { global: { plugins: [pinia()] } })
    await wrapper.find('.app-header__menu-toggle').trigger('click')
    expect(wrapper.find('#mobile-nav').exists()).toBe(true)
  })

  it('main nav has aria-label', async () => {
    const { default: AppHeader } = await import('@/components/AppHeader.vue')
    const wrapper = mount(AppHeader, { global: { plugins: [pinia()] } })
    expect(wrapper.find('[aria-label="Main navigation"]').exists()).toBe(true)
  })
})

// ── StatusBadge ────────────────────────────────────────────────────────────

describe('StatusBadge — accessibility', () => {
  it('has aria-label with status text', async () => {
    const { default: StatusBadge } = await import('@/components/application/StatusBadge.vue')
    const wrapper = mount(StatusBadge, { props: { status: 'INTERVIEW' } })
    expect(wrapper.find('[aria-label]').attributes('aria-label')).toContain('Interview')
  })

  it('dot indicator is aria-hidden', async () => {
    const { default: StatusBadge } = await import('@/components/application/StatusBadge.vue')
    const wrapper = mount(StatusBadge, { props: { status: 'OFFER' } })
    expect(wrapper.find('.status-badge__dot').attributes('aria-hidden')).toBe('true')
  })

  it('renders visible text label alongside color indicator', async () => {
    const { default: StatusBadge } = await import('@/components/application/StatusBadge.vue')
    const wrapper = mount(StatusBadge, { props: { status: 'REJECTED' } })
    expect(wrapper.text()).toContain('Rejected')
  })
})

// ── BillingView cancel dialog ──────────────────────────────────────────────

describe('BillingView cancel dialog — accessibility', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('cancel dialog emits close on Escape', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue({
      tier: 'PRO', status: 'ACTIVE', provider: 'DEMO',
      currentPeriodStart: '2025-01-01T00:00:00Z', currentPeriodEnd: '2025-02-01T00:00:00Z',
      pdfExportsUsed: null, pdfExportsLimit: null,
    })
    const { default: BillingView } = await import('@/views/BillingView.vue')
    const wrapper = mount(BillingView, { global: { plugins: [pinia()] } })
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="cancel-dialog"]').exists()).toBe(true)
    await wrapper.find('[data-testid="cancel-dialog"]').trigger('keydown.esc')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="cancel-dialog"]').exists()).toBe(false)
  })

  it('cancel dialog box has tabindex=-1', async () => {
    const { billingApi } = await import('@/api/billing')
    vi.mocked(billingApi.getSubscription).mockResolvedValue({
      tier: 'PRO', status: 'ACTIVE', provider: 'DEMO',
      currentPeriodStart: '2025-01-01T00:00:00Z', currentPeriodEnd: '2025-02-01T00:00:00Z',
      pdfExportsUsed: null, pdfExportsLimit: null,
    })
    const { default: BillingView } = await import('@/views/BillingView.vue')
    const wrapper = mount(BillingView, { global: { plugins: [pinia()] } })
    await flushPromises()

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="cancel-dialog"] .dialog-box').attributes('tabindex')).toBe('-1')
  })
})

// ── AnalyticsSection charts ────────────────────────────────────────────────

describe('AnalyticsSection — chart accessibility', () => {
  const analytics = {
    pipelineApplied: 3, pipelineInterview: 1, pipelineOffer: 1, pipelineRejected: 1,
    trend: [
      { year: 2024, month: 5, count: 2 },
      { year: 2024, month: 6, count: 4 },
    ],
  }

  it('donut chart has role=img with descriptive aria-label', async () => {
    const { default: AnalyticsSection } = await import('@/components/dashboard/AnalyticsSection.vue')
    const wrapper = mount(AnalyticsSection, {
      props: { analytics, analyticsLoading: false, analyticsError: null },
    })
    const donut = wrapper.find('[data-testid="pipeline-chart"] [role="img"]')
    expect(donut.exists()).toBe(true)
    const label = donut.attributes('aria-label') ?? ''
    expect(label).toContain('applied')
    expect(label).toContain('interview')
  })

  it('donut SVG is aria-hidden (data is in role=img container)', async () => {
    const { default: AnalyticsSection } = await import('@/components/dashboard/AnalyticsSection.vue')
    const wrapper = mount(AnalyticsSection, {
      props: { analytics, analyticsLoading: false, analyticsError: null },
    })
    expect(wrapper.find('svg').attributes('aria-hidden')).toBe('true')
  })

  it('bar chart has role=img with descriptive aria-label', async () => {
    const { default: AnalyticsSection } = await import('@/components/dashboard/AnalyticsSection.vue')
    const wrapper = mount(AnalyticsSection, {
      props: { analytics, analyticsLoading: false, analyticsError: null },
    })
    const chart = wrapper.find('[data-testid="trend-bars"]')
    expect(chart.attributes('role')).toBe('img')
    const label = chart.attributes('aria-label') ?? ''
    expect(label).toContain('May')
    expect(label).toContain('Jun')
  })

  it('legend items include label, count, and percentage text', async () => {
    const { default: AnalyticsSection } = await import('@/components/dashboard/AnalyticsSection.vue')
    const wrapper = mount(AnalyticsSection, {
      props: { analytics, analyticsLoading: false, analyticsError: null },
    })
    const legend = wrapper.find('[aria-label="Pipeline legend"]')
    expect(legend.text()).toContain('Applied')
    expect(legend.text()).toContain('3')
  })

  it('loading state has aria-busy=true', async () => {
    const { default: AnalyticsSection } = await import('@/components/dashboard/AnalyticsSection.vue')
    const wrapper = mount(AnalyticsSection, {
      props: { analytics: null, analyticsLoading: true, analyticsError: null },
    })
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
  })

  it('error state has role=alert', async () => {
    const { default: AnalyticsSection } = await import('@/components/dashboard/AnalyticsSection.vue')
    const wrapper = mount(AnalyticsSection, {
      props: { analytics: null, analyticsLoading: false, analyticsError: 'Failed to load.' },
    })
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  })
})

// ── ResumeEditorView sidebar nav ───────────────────────────────────────────

describe('ResumeEditorView sidebar nav — accessibility', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('active nav item has aria-current=true', async () => {
    const { default: ResumeEditorView } = await import('@/views/ResumeEditorView.vue')
    const wrapper = mount(ResumeEditorView, { global: { plugins: [pinia()] } })
    await flushPromises()
    const activeBtn = wrapper.find('[aria-current="true"]')
    expect(activeBtn.exists()).toBe(true)
  })

  it('sidebar has aria-label', async () => {
    const { default: ResumeEditorView } = await import('@/views/ResumeEditorView.vue')
    const wrapper = mount(ResumeEditorView, { global: { plugins: [pinia()] } })
    await flushPromises()
    expect(wrapper.find('aside[aria-label]').exists()).toBe(true)
  })

  it('main content area has aria-label', async () => {
    const { default: ResumeEditorView } = await import('@/views/ResumeEditorView.vue')
    const wrapper = mount(ResumeEditorView, { global: { plugins: [pinia()] } })
    await flushPromises()
    expect(wrapper.find('main[aria-label]').exists()).toBe(true)
  })

  it('section nav has aria-label', async () => {
    const { default: ResumeEditorView } = await import('@/views/ResumeEditorView.vue')
    const wrapper = mount(ResumeEditorView, { global: { plugins: [pinia()] } })
    await flushPromises()
    expect(wrapper.find('nav[aria-label="Resume sections"]').exists()).toBe(true)
  })
})
