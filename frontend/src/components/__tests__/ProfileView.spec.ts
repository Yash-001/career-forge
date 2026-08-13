import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProfileView from '@/views/ProfileView.vue'
import { useProfileStore } from '@/stores/profile'

vi.mock('@/api/profile', () => ({
  // oxlint-disable vitest/require-mock-type-parameters
  profileApi: {
    getProfile: vi.fn().mockResolvedValue(null),
    upsertProfile: vi.fn().mockResolvedValue({
      id: '1', professionalTitle: 'Dev', phone: null, location: null,
      professionalSummary: null, linkedinUrl: null, githubUrl: null, portfolioUrl: null,
      createdAt: '', updatedAt: '',
    }),
    getExperiences: vi.fn().mockResolvedValue([]),
    createExperience: vi.fn().mockResolvedValue({
      id: 'exp-1', companyName: 'Acme', jobTitle: 'Engineer', location: null,
      employmentType: null, startDate: '2020-01-01', endDate: null,
      currentlyWorking: true, description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }),
    updateExperience: vi.fn().mockResolvedValue({
      id: 'exp-1', companyName: 'Acme Updated', jobTitle: 'Senior Engineer', location: null,
      employmentType: null, startDate: '2020-01-01', endDate: null,
      currentlyWorking: true, description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }),
    deleteExperience: vi.fn().mockResolvedValue(undefined),
    getEducations: vi.fn().mockResolvedValue([]),
    createEducation: vi.fn().mockResolvedValue({
      id: 'edu-1', institutionName: 'MIT', degree: null, fieldOfStudy: null,
      location: null, startDate: null, endDate: null, grade: null,
      description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }),
    updateEducation: vi.fn().mockResolvedValue({
      id: 'edu-1', institutionName: 'MIT Updated', degree: null, fieldOfStudy: null,
      location: null, startDate: null, endDate: null, grade: null,
      description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }),
    deleteEducation: vi.fn().mockResolvedValue(undefined),
    getSkills: vi.fn().mockResolvedValue([]),
    createSkill: vi.fn().mockResolvedValue({
      id: 'skill-1', name: 'TypeScript', category: null, proficiency: null,
      displayOrder: 0, createdAt: '', updatedAt: '',
    }),
    updateSkill: vi.fn().mockResolvedValue({
      id: 'skill-1', name: 'TypeScript Updated', category: null, proficiency: null,
      displayOrder: 0, createdAt: '', updatedAt: '',
    }),
    deleteSkill: vi.fn().mockResolvedValue(undefined),
    extractError: vi.fn((err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } }
      return {
        status: 500, code: 'ERROR',
        message: e?.response?.data?.message ?? 'An unexpected error occurred.',
      }
    }),
  },
}))

function mountView() {
  return mount(ProfileView, {
    global: { plugins: [createPinia()] },
  })
}

describe('ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders all section headings', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Personal Information')
    expect(wrapper.text()).toContain('Professional Summary')
    expect(wrapper.text()).toContain('Online Profiles')
    expect(wrapper.text()).toContain('Work Experience')
    expect(wrapper.text()).toContain('Education')
    expect(wrapper.text()).toContain('Skills')
  })

  it('shows loading skeleton while data is fetching', async () => {
    // Make getExperiences hang so loading stays true
    const { profileApi } = await import('@/api/profile')
    vi.mocked(profileApi.getExperiences).mockReturnValueOnce(new Promise(() => {}))
    const wrapper = mountView()
    // Before resolving, loading should be true
    await vi.waitFor(() => {
      expect(wrapper.find('[aria-busy="true"]').exists()).toBe(true)
    })
  })

  it('hides loading skeleton after data loads', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[aria-busy="true"]').exists()).toBe(false)
  })

  it('shows empty state for experience when list is empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('No work experience added yet.')
  })

  it('shows empty state for education when list is empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('No education added yet.')
  })

  it('shows empty state for skills when list is empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('No skills added yet.')
  })

  it('opens experience form when Add button is clicked', async () => {
    const wrapper = mountView()
    await flushPromises()
    const addBtns = wrapper.findAll('button').filter((b) => b.text() === '+ Add')
    await addBtns[0]?.trigger('click')
    expect(wrapper.find('#exp-company').exists()).toBe(true)
  })

  it('blocks experience form submission when company name is blank', async () => {
    const { profileApi } = await import('@/api/profile')
    const wrapper = mountView()
    await flushPromises()

    const addBtns = wrapper.findAll('button').filter((b) => b.text() === '+ Add')
    await addBtns[0]?.trigger('click')

    // Trigger submit on the form element directly (happy-dom doesn't propagate button click to form submit)
    await wrapper.find('.inline-form-wrap form').trigger('submit')
    await flushPromises()

    expect(profileApi.createExperience).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Company name is required.')
  })

  it('calls addExperience when experience form is submitted with valid data', async () => {
    const { profileApi } = await import('@/api/profile')
    const wrapper = mountView()
    await flushPromises()

    const store = useProfileStore()
    const addBtns = wrapper.findAll('button').filter((b) => b.text() === '+ Add')
    await addBtns[0]?.trigger('click')

    await wrapper.find('#exp-company').setValue('Acme Corp')
    await wrapper.find('#exp-title').setValue('Engineer')
    await wrapper.find('#exp-start').setValue('2020-01-01')

    await wrapper.find('.inline-form-wrap form').trigger('submit')
    await flushPromises()

    expect(profileApi.createExperience).toHaveBeenCalledWith(
      expect.objectContaining({ companyName: 'Acme Corp', jobTitle: 'Engineer' }),
    )
    expect(store.experiences).toHaveLength(1)
  })

  it('pre-populates experience form for editing', async () => {
    const wrapper = mountView()
    await flushPromises()

    const store = useProfileStore()
    store.experiences = [{
      id: 'exp-1', companyName: 'Acme', jobTitle: 'Engineer', location: null,
      employmentType: null, startDate: '2020-01-01', endDate: null,
      currentlyWorking: true, description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }]
    await flushPromises()

    const editBtn = wrapper.findAll('button').find((b) => b.text() === 'Edit')
    await editBtn?.trigger('click')
    await flushPromises()

    const companyInput = wrapper.find('#exp-company')
    expect((companyInput.element as HTMLInputElement).value).toBe('Acme')
  })

  it('calls updateExperience when editing an existing experience', async () => {
    const { profileApi } = await import('@/api/profile')
    const wrapper = mountView()
    await flushPromises()

    const store = useProfileStore()
    store.experiences = [{
      id: 'exp-1', companyName: 'Acme', jobTitle: 'Engineer', location: null,
      employmentType: null, startDate: '2020-01-01', endDate: null,
      currentlyWorking: true, description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }]
    await flushPromises()

    const editBtn = wrapper.findAll('button').find((b) => b.text() === 'Edit')
    await editBtn?.trigger('click')
    await flushPromises()

    await wrapper.find('#exp-company').setValue('Acme Updated')

    await wrapper.find('.inline-form-wrap form').trigger('submit')
    await flushPromises()

    expect(profileApi.updateExperience).toHaveBeenCalledWith(
      'exp-1',
      expect.objectContaining({ companyName: 'Acme Updated' }),
    )
  })

  it('shows delete confirmation dialog when Delete is clicked', async () => {
    const wrapper = mountView()
    await flushPromises()

    const store = useProfileStore()
    store.experiences = [{
      id: 'exp-1', companyName: 'Acme', jobTitle: 'Engineer', location: null,
      employmentType: null, startDate: '2020-01-01', endDate: null,
      currentlyWorking: true, description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }]
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    await deleteBtn?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Delete Work Experience')
  })

  it('calls deleteExperience after confirming deletion', async () => {
    const { profileApi } = await import('@/api/profile')
    const wrapper = mountView()
    await flushPromises()

    const store = useProfileStore()
    store.experiences = [{
      id: 'exp-1', companyName: 'Acme', jobTitle: 'Engineer', location: null,
      employmentType: null, startDate: '2020-01-01', endDate: null,
      currentlyWorking: true, description: null, displayOrder: 0, createdAt: '', updatedAt: '',
    }]
    await flushPromises()

    // Click Delete on the item
    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === 'Delete')
    await deleteBtn?.trigger('click')
    await flushPromises()

    // The dialog is now open — find the btn-danger button (confirm delete)
    const confirmBtn = wrapper.find('.btn-danger')
    await confirmBtn.trigger('click')
    await flushPromises()

    expect(profileApi.deleteExperience).toHaveBeenCalledWith('exp-1')
  })

  it('calls addEducation when education form is submitted with valid data', async () => {
    const { profileApi } = await import('@/api/profile')
    const wrapper = mountView()
    await flushPromises()

    const store = useProfileStore()
    const addBtns = wrapper.findAll('button').filter((b) => b.text() === '+ Add')
    await addBtns[1]?.trigger('click') // Education is second + Add

    await wrapper.find('#edu-institution').setValue('MIT')

    await wrapper.find('.inline-form-wrap form').trigger('submit')
    await flushPromises()

    expect(profileApi.createEducation).toHaveBeenCalledWith(
      expect.objectContaining({ institutionName: 'MIT' }),
    )
    expect(store.educations).toHaveLength(1)
  })

  it('blocks skill submission when name is blank', async () => {
    const { profileApi } = await import('@/api/profile')
    const wrapper = mountView()
    await flushPromises()

    const addBtns = wrapper.findAll('button').filter((b) => b.text() === '+ Add')
    await addBtns[2]?.trigger('click') // Skills is third + Add

    await wrapper.find('.inline-form-wrap form').trigger('submit')
    await flushPromises()

    expect(profileApi.createSkill).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Skill name is required.')
  })

  it('calls addSkill when skill form is submitted with valid name', async () => {
    const { profileApi } = await import('@/api/profile')
    const wrapper = mountView()
    await flushPromises()

    const store = useProfileStore()
    const addBtns = wrapper.findAll('button').filter((b) => b.text() === '+ Add')
    await addBtns[2]?.trigger('click')

    await wrapper.find('#skill-name').setValue('TypeScript')

    await wrapper.find('.inline-form-wrap form').trigger('submit')
    await flushPromises()

    expect(profileApi.createSkill).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'TypeScript' }),
    )
    expect(store.skills).toHaveLength(1)
  })

  it('displays API error message when experience creation fails', async () => {
    const { profileApi } = await import('@/api/profile')
    vi.mocked(profileApi.createExperience).mockRejectedValueOnce({
      response: { status: 400, data: { code: 'VALIDATION_ERROR', message: 'Server validation failed.' } },
    })

    const wrapper = mountView()
    await flushPromises()

    const addBtns = wrapper.findAll('button').filter((b) => b.text() === '+ Add')
    await addBtns[0]?.trigger('click')

    await wrapper.find('#exp-company').setValue('Acme')
    await wrapper.find('#exp-title').setValue('Engineer')
    await wrapper.find('#exp-start').setValue('2020-01-01')

    await wrapper.find('.inline-form-wrap form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Server validation failed.')
  })
})
