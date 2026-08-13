import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CreateResumeView from '@/views/CreateResumeView.vue'

const mockPush = vi.fn<() => void>()

vi.mock('@/api/resume', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  resumeApi: {
    createResume: vi.fn().mockResolvedValue({
      id: 'new-id',
      name: 'My Resume',
      latestVersion: { id: 'v1', versionNumber: 1, title: null, isLatest: true, createdAt: '' },
      createdAt: '',
      updatedAt: '',
    }),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Something went wrong.' })),
  },
}))

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: mockPush })),
  RouterLink: { template: '<a><slot /></a>' },
}))

function mountView() {
  return mount(CreateResumeView, {
    global: { plugins: [createPinia()] },
  })
}

describe('CreateResumeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders step 1 name input on mount', async () => {
    const wrapper = mountView()
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
  })

  it('shows validation error when name is blank on Next', async () => {
    const wrapper = mountView()
    const form = wrapper.find('form')
    await form.trigger('submit')
    expect(wrapper.text()).toContain('Resume name is required')
  })

  it('advances to step 2 when a valid name is entered', async () => {
    const wrapper = mountView()
    await wrapper.find('input[type="text"]').setValue('My Resume')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('My Resume')
    expect(wrapper.find('input[type="text"]').exists()).toBe(false)
  })

  it('calls createResume and navigates on confirm', async () => {
    const { resumeApi } = await import('@/api/resume')
    const wrapper = mountView()
    await wrapper.find('input[type="text"]').setValue('My Resume')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const confirmBtn = wrapper.findAll('button').find((b) => b.text().includes('Create'))
    await confirmBtn?.trigger('click')
    await flushPromises()

    expect(resumeApi.createResume).toHaveBeenCalledWith({ name: 'My Resume' })
    expect(mockPush).toHaveBeenCalledWith('/resumes/new-id')
  })

  it('shows API error when createResume fails', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.createResume).mockRejectedValueOnce({
      response: { status: 400, data: { code: 'ERROR', message: 'Creation failed.' } },
    })
    vi.mocked(resumeApi.extractError).mockReturnValueOnce({ status: 400, code: 'ERROR', message: 'Creation failed.' })

    const wrapper = mountView()
    await wrapper.find('input[type="text"]').setValue('My Resume')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const confirmBtn = wrapper.findAll('button').find((b) => b.text().includes('Create'))
    await confirmBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Creation failed.')
  })
})
