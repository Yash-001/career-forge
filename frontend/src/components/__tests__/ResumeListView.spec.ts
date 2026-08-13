import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ResumeListView from '@/views/ResumeListView.vue'
import { useResumeStore } from '@/stores/resume'

vi.mock('@/api/resume', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  resumeApi: {
    listResumes: vi.fn().mockResolvedValue([]),
    deleteResume: vi.fn().mockResolvedValue(undefined),
    updateResume: vi.fn().mockResolvedValue({ id: 'r1', name: 'Renamed', latestVersionNumber: 1, createdAt: '', updatedAt: '' }),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Something went wrong.' })),
  },
}))

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  RouterLink: { template: '<a><slot /></a>' },
}))

const sampleResume = {
  id: 'r1',
  name: 'My Resume',
  latestVersionNumber: 2,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-02T00:00:00Z',
}

let pinia: ReturnType<typeof createPinia>

function mountView() {
  return mount(ResumeListView, { global: { plugins: [pinia] } })
}

describe('ResumeListView', () => {
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  it('shows empty state when no resumes exist', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('No resumes yet')
  })

  it('renders resume cards when resumes exist', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.listResumes).mockResolvedValueOnce([sampleResume])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('My Resume')
    expect(wrapper.text()).toContain('v2')
  })

  it('shows loading skeleton while fetching', async () => {
    const wrapper = mountView()
    const store = useResumeStore()
    store.loading = true
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
  })

  it('hides skeleton after load completes', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.skeleton').exists()).toBe(false)
  })

  it('shows delete confirmation dialog when Delete is clicked', async () => {
    const wrapper = mountView()
    await flushPromises()
    useResumeStore().resumes = [sampleResume]
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    await deleteBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Delete Resume')
  })

  it('calls deleteResume after confirming deletion', async () => {
    const { resumeApi } = await import('@/api/resume')
    const wrapper = mountView()
    await flushPromises()
    useResumeStore().resumes = [sampleResume]
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text() === 'Delete')?.trigger('click')
    await flushPromises()
    await wrapper.find('.btn-danger').trigger('click')
    await flushPromises()

    expect(resumeApi.deleteResume).toHaveBeenCalledWith('r1')
  })

  it('cancels deletion when Cancel is clicked in dialog', async () => {
    const { resumeApi } = await import('@/api/resume')
    const wrapper = mountView()
    await flushPromises()
    useResumeStore().resumes = [sampleResume]
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text() === 'Delete')?.trigger('click')
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === 'Cancel')?.trigger('click')
    await flushPromises()

    expect(resumeApi.deleteResume).not.toHaveBeenCalled()
  })
})
