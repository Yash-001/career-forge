import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ResumeEditorView from '@/views/ResumeEditorView.vue'

vi.mock('vue-router', () => ({
  useRoute: vi.fn<() => { params: { resumeId: string }; query: Record<string, string> }>(() => ({ params: { resumeId: 'r1' }, query: {} })),
  useRouter: vi.fn<() => { push: ReturnType<typeof vi.fn> }>(() => ({ push: vi.fn<() => void>() })),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/api/resume', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  resumeApi: {
    getResume: vi.fn().mockResolvedValue({
      id: 'r1',
      name: 'My Resume',
      latestVersion: { id: 'v1', versionNumber: 1, title: null, isLatest: true, createdAt: '' },
      createdAt: '',
      updatedAt: '',
    }),
    getVersion: vi.fn().mockResolvedValue({
      id: 'v1',
      versionNumber: 1,
      title: null,
      professionalSummary: null,
      isLatest: true,
      experiences: [],
      educations: [],
      skills: [],
      createdAt: '2024-01-01T00:00:00Z',
    }),
    updateVersionMeta: vi.fn().mockResolvedValue({
      id: 'v1',
      versionNumber: 1,
      title: 'Google App',
      professionalSummary: 'Summary text',
      isLatest: true,
      experiences: [],
      educations: [],
      skills: [],
      createdAt: '2024-01-01T00:00:00Z',
    }),
    createVersion: vi.fn().mockResolvedValue({
      id: 'v2',
      versionNumber: 2,
      title: null,
      professionalSummary: null,
      isLatest: true,
      experiences: [],
      educations: [],
      skills: [],
      createdAt: '2024-02-01T00:00:00Z',
    }),
    exportVersionPdf: vi.fn().mockResolvedValue(new ArrayBuffer(8)),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'Something went wrong.' })),
  },
}))

vi.mock('@/api/ai', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  aiApi: {
    analyzeResume: vi.fn().mockResolvedValue({
      detectedRole: 'Backend Engineer',
      keywords: ['java', 'agile'],
      technologies: ['java', 'spring boot'],
      responsibilities: ['develop'],
      matchedResumeSkills: ['java'],
      missingSkills: ['spring boot'],
      providerName: 'Demo AI (rule-based)',
    }),
    tailorResume: vi.fn().mockResolvedValue({
      suggestions: [
        {
          experienceId: 'exp-1',
          originalText: 'Developed REST APIs.',
          suggestedText: 'Developed REST APIs. Demonstrates proficiency in java.',
          matchedKeywords: ['java'],
          rationale: 'Matched keywords: java.',
        },
      ],
      detectedKeywords: ['java'],
      providerName: 'Demo AI (rule-based)',
    }),
    acceptTailoring: vi.fn().mockResolvedValue({
      id: 'v3',
      versionNumber: 3,
      title: 'Software Engineer — AI Tailored',
      professionalSummary: null,
      isLatest: true,
      experiences: [],
      educations: [],
      skills: [],
      createdAt: '2024-03-01T00:00:00Z',
    }),
    extractError: vi.fn(() => ({ status: 500, code: 'ERROR', message: 'AI error.' })),
  },
}))

function mountView() {
  return mount(ResumeEditorView, {
    global: { plugins: [createPinia()] },
  })
}

describe('ResumeEditorView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('shows loading skeleton before data loads', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.getResume).mockReturnValueOnce(new Promise(() => {}))
    const wrapper = mountView()
    expect(wrapper.find('.skeleton').exists()).toBe(true)
  })

  it('renders resume name and version after load', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('My Resume')
    expect(wrapper.text()).toContain('Version 1')
  })

  it('shows Latest badge for latest version', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.badge').text()).toBe('Latest')
  })

  it('renders summary section by default', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('#ver-title').exists()).toBe(true)
    expect(wrapper.find('#ver-summary').exists()).toBe(true)
  })

  it('switches to experience section on nav click', async () => {
    const wrapper = mountView()
    await flushPromises()
    const expNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'Experience')
    await expNav?.trigger('click')
    expect(wrapper.find('#ver-title').exists()).toBe(false)
  })

  it('calls updateVersionMeta on Save', async () => {
    const { resumeApi } = await import('@/api/resume')
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('#ver-title').setValue('Google App')
    await wrapper.find('#ver-summary').setValue('Summary text')

    const saveBtn = wrapper.findAll('button').find((b) => b.text() === 'Save')
    await saveBtn?.trigger('click')
    await flushPromises()

    expect(resumeApi.updateVersionMeta).toHaveBeenCalledWith('r1', 'v1', {
      title: 'Google App',
      professionalSummary: 'Summary text',
    })
  })

  it('shows Saved! feedback after successful meta save', async () => {
    const wrapper = mountView()
    await flushPromises()

    const saveBtn = wrapper.findAll('button').find((b) => b.text() === 'Save')
    await saveBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Saved!')
  })

  it('calls createVersion and updates sidebar on New Version', async () => {
    const { resumeApi } = await import('@/api/resume')
    const wrapper = mountView()
    await flushPromises()

    const newVersionBtn = wrapper.findAll('button').find((b) => b.text() === 'New Version')
    await newVersionBtn?.trigger('click')
    await flushPromises()

    expect(resumeApi.createVersion).toHaveBeenCalledWith('r1')
    expect(wrapper.text()).toContain('Version 2')
  })

  it('shows load error when getResume fails', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.getResume).mockRejectedValueOnce({})
    vi.mocked(resumeApi.extractError).mockReturnValueOnce({ status: 500, code: 'ERROR', message: 'Failed to load.' })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load.')
  })

  // ── AI Tailoring section ──────────────────────────────────────────────────

  it('renders AI Tailoring nav item', async () => {
    const wrapper = mountView()
    await flushPromises()
    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    expect(aiNav).toBeTruthy()
  })

  it('switches to AI section on nav click', async () => {
    const wrapper = mountView()
    await flushPromises()
    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')
    expect(wrapper.find('#job-description').exists()).toBe(true)
  })

  it('shows empty state before analysis', async () => {
    const wrapper = mountView()
    await flushPromises()
    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')
    expect(wrapper.text()).toContain('Paste a job description above')
  })

  it('calls aiApi.analyzeResume when Analyze is clicked', async () => {
    const { aiApi } = await import('@/api/ai')
    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')

    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    const analyzeBtn = wrapper.findAll('button').find((b) => b.text() === 'Analyze')
    await analyzeBtn?.trigger('click')
    await flushPromises()

    expect(aiApi.analyzeResume).toHaveBeenCalledWith('r1', 'v1', 'Java Spring Boot backend engineer role.')
  })

  it('renders analysis results after analyze', async () => {
    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')

    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Backend Engineer')
  })

  it('calls aiApi.tailorResume when Tailor Resume is clicked after analysis', async () => {
    const { aiApi } = await import('@/api/ai')
    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')

    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text() === 'Tailor Resume')?.trigger('click')
    await flushPromises()

    expect(aiApi.tailorResume).toHaveBeenCalledWith('r1', 'v1', 'Java Spring Boot backend engineer role.')
  })

  it('renders tailoring suggestions after tailor', async () => {
    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')

    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === 'Tailor Resume')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Developed REST APIs.')
  })

  it('shows API error message when analyze fails', async () => {
    const { aiApi } = await import('@/api/ai')
    vi.mocked(aiApi.analyzeResume).mockRejectedValueOnce({})
    vi.mocked(aiApi.extractError).mockReturnValueOnce({ status: 500, code: 'ERROR', message: 'AI error.' })

    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')

    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('AI error.')
  })

  it('renders Accept and Reject buttons after tailoring', async () => {
    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')
    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === 'Tailor Resume')?.trigger('click')
    await flushPromises()

    const buttons = wrapper.findAll('button').map((b) => b.text())
    expect(buttons).toContain('Accept')
    expect(buttons).toContain('Reject')
  })

  it('calls aiApi.acceptTailoring when apply is triggered', async () => {
    const { aiApi } = await import('@/api/ai')
    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')
    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === 'Tailor Resume')?.trigger('click')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text() === 'Accept')?.trigger('click')
    await wrapper.find('[data-testid="apply-btn"]').trigger('click')
    await flushPromises()

    expect(aiApi.acceptTailoring).toHaveBeenCalledWith('r1', 'v1', {
      acceptedSuggestions: [{ experienceId: 'exp-1', suggestedText: 'Developed REST APIs. Demonstrates proficiency in java.' }],
    })
  })

  it('shows success message after accept tailoring', async () => {
    const wrapper = mountView()
    await flushPromises()

    const aiNav = wrapper.findAll('.nav-item').find((b) => b.text() === 'AI Tailoring')
    await aiNav?.trigger('click')
    await wrapper.find('#job-description').setValue('Java Spring Boot backend engineer role.')
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === 'Tailor Resume')?.trigger('click')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text() === 'Accept')?.trigger('click')
    await wrapper.find('[data-testid="apply-btn"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('New tailored version created!')
  })
})

// ── PDF Export ────────────────────────────────────────────────────────────────

describe('ResumeEditorView — PDF Export', () => {
  let createObjectURL: ReturnType<typeof vi.fn>
  let revokeObjectURL: ReturnType<typeof vi.fn>
  let anchorClick: ReturnType<typeof vi.fn>

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()

    createObjectURL = vi.fn(() => 'blob:mock-url')
    revokeObjectURL = vi.fn()
    Object.defineProperty(globalThis, 'URL', {
      value: { createObjectURL, revokeObjectURL },
      writable: true,
      configurable: true,
    })

    anchorClick = vi.fn()
    const origCreate = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
      const el = origCreate(tag)
      if (tag === 'a') vi.spyOn(el, 'click').mockImplementation(anchorClick)
      return el
    })
  })

  // 1. Button renders
  it('renders Export PDF button', async () => {
    const wrapper = mountView()
    await flushPromises()
    const btn = wrapper.find('[data-testid="export-pdf-btn"]')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toBe('Export PDF')
  })

  // 2. Loading state
  it('shows loading state and disables button while exporting', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.exportVersionPdf).mockReturnValueOnce(new Promise(() => {}))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="export-pdf-btn"]').trigger('click')

    const btn = wrapper.find('[data-testid="export-pdf-btn"]')
    expect(btn.text()).toContain('Exporting')
    expect(btn.attributes('disabled')).toBeDefined()
    expect(btn.find('.spinner').exists()).toBe(true)
  })

  // 3. Successful download
  it('triggers browser download on successful export', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="export-pdf-btn"]').trigger('click')
    await flushPromises()

    expect(createObjectURL).toHaveBeenCalledWith(expect.any(Blob))
    expect(anchorClick).toHaveBeenCalled()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')
  })

  // 4. API failure shows error
  it('shows error message on API failure', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.exportVersionPdf).mockRejectedValueOnce({})
    vi.mocked(resumeApi.extractError).mockReturnValueOnce({
      status: 500,
      code: 'INTERNAL_ERROR',
      message: 'PDF generation failed.',
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="export-pdf-btn"]').trigger('click')
    await flushPromises()

    const errorEl = wrapper.find('[data-testid="export-error"]')
    expect(errorEl.exists()).toBe(true)
    expect(errorEl.text()).toContain('PDF generation failed.')
  })

  // 5. Export limit error shows dedicated message
  it('shows limit-reached message on PDF_EXPORT_LIMIT_EXCEEDED', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.exportVersionPdf).mockRejectedValueOnce({})
    vi.mocked(resumeApi.extractError).mockReturnValueOnce({
      status: 402,
      code: 'PDF_EXPORT_LIMIT_EXCEEDED',
      message: 'You have reached the 3 PDF export limit for this month.',
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="export-pdf-btn"]').trigger('click')
    await flushPromises()

    const errorEl = wrapper.find('[data-testid="export-error"]')
    expect(errorEl.exists()).toBe(true)
    expect(errorEl.text()).toContain("You've reached your 3 monthly PDF exports")
    expect(errorEl.text()).toContain('Upgrade to Pro')
  })

  // 6. Correct resume/version IDs are passed
  it('calls exportVersionPdf with the current resumeId and versionId', async () => {
    const { resumeApi } = await import('@/api/resume')

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="export-pdf-btn"]').trigger('click')
    await flushPromises()

    expect(resumeApi.exportVersionPdf).toHaveBeenCalledWith('r1', 'v1')
  })

  // 7. Accessibility
  it('has aria-label and sets aria-busy while exporting', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.exportVersionPdf).mockReturnValueOnce(new Promise(() => {}))

    const wrapper = mountView()
    await flushPromises()

    const btn = wrapper.find('[data-testid="export-pdf-btn"]')
    expect(btn.attributes('aria-label')).toBe('Export current version as PDF')

    await btn.trigger('click')
    expect(btn.attributes('aria-busy')).toBe('true')
  })
})

// ── Branch error ──────────────────────────────────────────────────────────────

describe('ResumeEditorView — New Version error', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('shows branch error in sidebar (not load error) when createVersion fails', async () => {
    const { resumeApi } = await import('@/api/resume')
    vi.mocked(resumeApi.createVersion).mockRejectedValueOnce({})
    vi.mocked(resumeApi.extractError).mockReturnValueOnce({ status: 500, code: 'ERROR', message: 'Branch failed.' })

    const wrapper = mountView()
    await flushPromises()

    const newVersionBtn = wrapper.findAll('button').find((b) => b.text() === 'New Version')
    await newVersionBtn?.trigger('click')
    await flushPromises()

    // Error should appear in sidebar version-box, not as the main load error
    expect(wrapper.find('.export-error').text()).toContain('Branch failed.')
    // Main content should still be visible (not replaced by load error)
    expect(wrapper.find('#ver-title').exists()).toBe(true)
  })
})
