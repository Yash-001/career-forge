import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ApplicationListView from '@/views/ApplicationListView.vue'
import { useApplicationStore } from '@/stores/application'
import type { ApplicationResponse } from '@/api/application'

// ── Mocks ──────────────────────────────────────────────────────────────────

vi.mock('@/api/application', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  applicationApi: {
    list: vi.fn().mockResolvedValue([]),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn().mockResolvedValue(undefined),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Something went wrong.' })),
  },
}))

vi.mock('@/api/resume', () => ({
  resumeApi: {
    listResumes: vi.fn().mockResolvedValue([]),
    listVersions: vi.fn().mockResolvedValue([]),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Something went wrong.' })),
  },
}))

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  RouterLink: { template: '<a><slot /></a>' },
}))

// ── Fixtures ───────────────────────────────────────────────────────────────

const makeApp = (overrides: Partial<ApplicationResponse> = {}): ApplicationResponse => ({
  id: 'a1',
  companyName: 'Acme Corp',
  jobTitle: 'Software Engineer',
  applicationDate: '2024-06-01',
  jobUrl: 'https://acme.com/jobs/1',
  status: 'APPLIED',
  resumeVersionId: null,
  resumeVersionTitle: null,
  resumeVersionNumber: null,
  createdAt: '2024-06-01T00:00:00Z',
  updatedAt: '2024-06-01T00:00:00Z',
  ...overrides,
})

let pinia: ReturnType<typeof createPinia>

function mountView() {
  return mount(ApplicationListView, { global: { plugins: [pinia] } })
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('ApplicationListView', () => {
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  // Empty state
  it('shows empty state when no applications exist', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('No applications yet')
  })

  // Loading state
  it('shows loading skeleton while fetching', async () => {
    const wrapper = mountView()
    const store = useApplicationStore()
    store.loading = true
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
  })

  it('hides skeleton after load completes', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.skeleton').exists()).toBe(false)
  })

  // Application list
  it('renders application cards when applications exist', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Acme Corp')
    expect(wrapper.text()).toContain('Software Engineer')
  })

  // Status display
  it('displays status badge with correct label', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp({ status: 'INTERVIEW' })]
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Interview')
  })

  it('displays all four status labels correctly', async () => {
    const statuses = ['APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'] as const
    const labels = ['Applied', 'Interview', 'Offer', 'Rejected']
    for (let i = 0; i < statuses.length; i++) {
      pinia = createPinia()
      setActivePinia(pinia)
      const wrapper = mountView()
      await flushPromises()
      useApplicationStore().applications = [makeApp({ id: `a${i}`, status: statuses[i] })]
      await wrapper.vm.$nextTick()
      expect(wrapper.text()).toContain(labels[i])
    }
  })

  // Resume version display
  it('shows resume version badge when linked', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp({ resumeVersionNumber: 3, resumeVersionTitle: 'Google SWE' })]
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('v3')
    expect(wrapper.text()).toContain('Google SWE')
  })

  it('does not show version badge when no version linked', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.badge-neutral').exists()).toBe(false)
  })

  // Filtering — search
  it('filters by company name search', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [
      makeApp({ id: 'a1', companyName: 'Acme Corp', jobTitle: 'Engineer' }),
      makeApp({ id: 'a2', companyName: 'Beta Inc', jobTitle: 'Designer' }),
    ]
    await wrapper.vm.$nextTick()
    const input = wrapper.find('input[type="search"]')
    await input.setValue('acme')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Acme Corp')
    expect(wrapper.text()).not.toContain('Beta Inc')
  })

  it('filters by role search', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [
      makeApp({ id: 'a1', companyName: 'Acme', jobTitle: 'Frontend Engineer' }),
      makeApp({ id: 'a2', companyName: 'Beta', jobTitle: 'Product Manager' }),
    ]
    await wrapper.vm.$nextTick()
    await wrapper.find('input[type="search"]').setValue('frontend')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Frontend Engineer')
    expect(wrapper.text()).not.toContain('Product Manager')
  })

  // Filtering — status
  it('filters by status button', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [
      makeApp({ id: 'a1', status: 'APPLIED', companyName: 'Acme' }),
      makeApp({ id: 'a2', status: 'OFFER', companyName: 'Beta' }),
    ]
    await wrapper.vm.$nextTick()
    const offerBtn = wrapper.findAll('.filter-btn').find((b) => b.text() === 'Offer')
    await offerBtn?.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Beta')
    expect(wrapper.text()).not.toContain('Acme')
  })

  it('shows empty filtered state when no results match filter', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp({ status: 'APPLIED' })]
    await wrapper.vm.$nextTick()
    const rejectedBtn = wrapper.findAll('.filter-btn').find((b) => b.text() === 'Rejected')
    await rejectedBtn?.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('No results')
  })

  // Create
  it('opens form when Add Application is clicked', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button.btn-primary').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Add Application')
  })

  it('closes form on cancel', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button.btn-primary').trigger('click')
    await wrapper.vm.$nextTick()
    const cancelBtn = wrapper.findAll('button').find((b) => b.text() === 'Cancel')
    await cancelBtn?.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.dialog-box').exists()).toBe(false)
  })

  it('calls addApplication and closes form on successful create', async () => {
    const { applicationApi } = await import('@/api/application')
    const newApp = makeApp({ id: 'new1', companyName: 'NewCo', jobTitle: 'Dev' })
    vi.mocked(applicationApi.create).mockResolvedValueOnce(newApp)

    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button.btn-primary').trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#app-company').setValue('NewCo')
    await wrapper.find('#app-role').setValue('Dev')
    await wrapper.find('#app-date').setValue('2024-07-01')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(applicationApi.create).toHaveBeenCalledWith(
      expect.objectContaining({ companyName: 'NewCo', jobTitle: 'Dev' }),
    )
    expect(wrapper.find('.dialog-box').exists()).toBe(false)
  })

  // Validation
  it('shows validation errors when required fields are empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button.btn-primary').trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#app-company').setValue('')
    await wrapper.find('#app-role').setValue('')
    await wrapper.find('form').trigger('submit')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Company is required.')
    expect(wrapper.text()).toContain('Role is required.')
  })

  it('shows URL validation error for invalid URL', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button.btn-primary').trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#app-company').setValue('Acme')
    await wrapper.find('#app-role').setValue('Dev')
    await wrapper.find('#app-date').setValue('2024-07-01')
    await wrapper.find('#app-url').setValue('not-a-url')
    await wrapper.find('form').trigger('submit')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Must be a valid URL')
  })

  // Edit
  it('opens edit form with pre-filled data', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()

    const editBtn = wrapper.findAll('button').find((b) => b.text() === 'Edit')
    await editBtn?.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Edit Application')
    expect((wrapper.find('#app-company').element as HTMLInputElement).value).toBe('Acme Corp')
  })

  it('calls editApplication on successful edit', async () => {
    const { applicationApi } = await import('@/api/application')
    const existing = makeApp()
    const updated = makeApp({ companyName: 'Updated Corp' })
    vi.mocked(applicationApi.update).mockResolvedValueOnce(updated)

    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [existing]
    await wrapper.vm.$nextTick()

    await wrapper.findAll('button').find((b) => b.text() === 'Edit')?.trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#app-company').setValue('Updated Corp')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(applicationApi.update).toHaveBeenCalledWith('a1', expect.objectContaining({ companyName: 'Updated Corp' }))
  })

  // Delete
  it('shows delete confirmation dialog when Delete is clicked', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()

    await wrapper.findAll('button').find((b) => b.text() === 'Delete')?.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Delete Application')
  })

  it('calls removeApplication after confirming deletion', async () => {
    const { applicationApi } = await import('@/api/application')
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()

    await wrapper.findAll('button').find((b) => b.text() === 'Delete')?.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.find('.btn-danger').trigger('click')
    await flushPromises()

    expect(applicationApi.remove).toHaveBeenCalledWith('a1')
  })

  it('cancels deletion when Cancel is clicked in dialog', async () => {
    const { applicationApi } = await import('@/api/application')
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()

    await wrapper.findAll('button').find((b) => b.text() === 'Delete')?.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.findAll('button').find((b) => b.text() === 'Cancel')?.trigger('click')
    await flushPromises()

    expect(applicationApi.remove).not.toHaveBeenCalled()
  })

  it('shows error in delete dialog when removeApplication fails', async () => {
    const { applicationApi } = await import('@/api/application')
    vi.mocked(applicationApi.remove).mockRejectedValueOnce({})
    vi.mocked(applicationApi.extractError).mockReturnValueOnce({ status: 500, code: 'ERROR', message: 'Delete failed.' })

    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()

    await wrapper.findAll('button').find((b) => b.text() === 'Delete')?.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.find('.btn-danger').trigger('click')
    await flushPromises()

    expect(wrapper.find('.dialog-error').exists()).toBe(true)
    expect(wrapper.text()).toContain('Delete failed.')
  })

  // API error
  it('shows API error when load fails', async () => {
    const { applicationApi } = await import('@/api/application')
    vi.mocked(applicationApi.list).mockRejectedValueOnce(new Error('Network error'))

    const wrapper = mountView()
    await flushPromises()

    const store = useApplicationStore()
    store.error = 'Something went wrong.'
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Something went wrong.')
  })

  it('shows API error when create fails', async () => {
    const { applicationApi } = await import('@/api/application')
    vi.mocked(applicationApi.create).mockRejectedValueOnce({ response: { status: 500, data: { code: 'ERROR', message: 'Server error.' } } })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button.btn-primary').trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#app-company').setValue('Acme')
    await wrapper.find('#app-role').setValue('Dev')
    await wrapper.find('#app-date').setValue('2024-07-01')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.dialog-box').exists()).toBe(true)
  })

  it('shows error state when store has an error', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().error = 'Unauthorized.'
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.api-error').exists()).toBe(true)
  })

  it('shows retry button on load error', async () => {
    const { applicationApi } = await import('@/api/application')
    vi.mocked(applicationApi.list).mockRejectedValueOnce({})
    vi.mocked(applicationApi.extractError).mockReturnValueOnce({ status: 500, code: 'ERROR', message: 'Load failed.' })

    const wrapper = mountView()
    await flushPromises()

    useApplicationStore().error = 'Load failed.'
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Retry')
  })

  // Resume version selection
  it('renders resume version options in form', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.listResumes).mockResolvedValueOnce([
      { id: 'r1', name: 'My Resume', latestVersionNumber: 2, createdAt: '', updatedAt: '' },
    ])
    vi.mocked(resumeApi.listVersions).mockResolvedValueOnce([
      { id: 'v1', versionNumber: 1, title: 'Google App', isLatest: false, createdAt: '' },
      { id: 'v2', versionNumber: 2, title: null, isLatest: true, createdAt: '' },
    ])

    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('button.btn-primary').trigger('click')
    await flushPromises()

    const select = wrapper.find('#app-version')
    expect(select.text()).toContain('My Resume')
    expect(select.text()).toContain('v1')
    expect(select.text()).toContain('Google App')
  })

  // Accessibility
  it('has aria-label on the application list', async () => {
    const wrapper = mountView()
    await flushPromises()
    useApplicationStore().applications = [makeApp()]
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[aria-label="Job applications"]').exists()).toBe(true)
  })

  it('has aria-label on filter search input', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('input[aria-label="Search by company or role"]').exists()).toBe(true)
  })

  it('status filter buttons have aria-pressed attribute', async () => {
    const wrapper = mountView()
    await flushPromises()
    const allBtn = wrapper.findAll('.filter-btn').find((b) => b.text() === 'All')
    expect(allBtn?.attributes('aria-pressed')).toBe('true')
  })
})
