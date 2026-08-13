import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ResumeVersionHistoryView from '@/views/ResumeVersionHistoryView.vue'

vi.mock('vue-router', () => ({
  useRoute: vi.fn<() => { params: { resumeId: string } }>(() => ({ params: { resumeId: 'r1' } })),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/api/resume', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  resumeApi: {
    getResume: vi.fn().mockResolvedValue({
      id: 'r1',
      name: 'My Resume',
      latestVersion: { id: 'v2', versionNumber: 2, title: null, isLatest: true, createdAt: '' },
      createdAt: '',
      updatedAt: '',
    }),
    listVersions: vi.fn().mockResolvedValue([
      { id: 'v1', versionNumber: 1, title: null, isLatest: false, createdAt: '2024-01-01T00:00:00Z' },
      { id: 'v2', versionNumber: 2, title: 'Google App', isLatest: true, createdAt: '2024-02-01T00:00:00Z' },
    ]),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Something went wrong.' })),
  },
}))

function mountView() {
  return mount(ResumeVersionHistoryView, {
    global: { plugins: [createPinia()] },
  })
}

describe('ResumeVersionHistoryView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('shows loading skeleton before data loads', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.listVersions).mockReturnValueOnce(new Promise(() => {}))
    const wrapper = mountView()
    expect(wrapper.find('.skeleton').exists()).toBe(true)
  })

  it('renders resume name after load', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('My Resume')
  })

  it('renders all versions', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Version 1')
    expect(wrapper.text()).toContain('Version 2')
  })

  it('shows Latest badge on the latest version', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.badge').text()).toBe('Latest')
  })

  it('renders version title when present', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Google App')
  })

  it('renders versions in descending order (newest first)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const versionNumbers = wrapper.findAll('.version-number').map((el) => el.text())
    expect(versionNumbers[0]).toContain('2')
    expect(versionNumbers[1]).toContain('1')
  })

  it('shows empty state when no versions exist', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.listVersions).mockResolvedValueOnce([])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('No versions found.')
  })

  it('shows error when load fails', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.listVersions).mockRejectedValueOnce({})
    vi.mocked(resumeApi.extractError).mockReturnValueOnce({ status: 500, code: 'ERROR', message: 'Load failed.' })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Load failed.')
  })
})
