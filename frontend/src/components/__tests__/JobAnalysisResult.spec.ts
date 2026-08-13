import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import JobAnalysisResult from '@/components/ai/JobAnalysisResult.vue'
import type { JobAnalysisResponse } from '@/api/ai'

const fullResult: JobAnalysisResponse = {
  detectedRole: 'Backend Engineer',
  keywords: ['java', 'agile'],
  technologies: ['java', 'spring boot', 'docker'],
  responsibilities: ['develop', 'deploy'],
  matchedResumeSkills: ['java', 'docker'],
  missingSkills: ['spring boot'],
  providerName: 'Demo AI (rule-based)',
}

const emptyResult: JobAnalysisResponse = {
  detectedRole: null,
  keywords: [],
  technologies: [],
  responsibilities: [],
  matchedResumeSkills: [],
  missingSkills: [],
  providerName: 'Demo AI (rule-based)',
}

function mountResult(result: JobAnalysisResponse) {
  return mount(JobAnalysisResult, { props: { result } })
}

describe('JobAnalysisResult', () => {
  it('renders detected role', () => {
    const wrapper = mountResult(fullResult)
    expect(wrapper.text()).toContain('Backend Engineer')
  })

  it('renders technologies as badges', () => {
    const wrapper = mountResult(fullResult)
    expect(wrapper.text()).toContain('spring boot')
    expect(wrapper.text()).toContain('docker')
  })

  it('renders keywords', () => {
    const wrapper = mountResult(fullResult)
    expect(wrapper.text()).toContain('agile')
  })

  it('renders matched resume skills', () => {
    const wrapper = mountResult(fullResult)
    expect(wrapper.text()).toContain('java')
  })

  it('renders missing skills', () => {
    const wrapper = mountResult(fullResult)
    expect(wrapper.text()).toContain('spring boot')
  })

  it('shows Demo AI badge', () => {
    const wrapper = mountResult(fullResult)
    expect(wrapper.text()).toContain('Demo AI')
  })

  it('shows empty state for technologies when none detected', () => {
    const wrapper = mountResult(emptyResult)
    expect(wrapper.text()).toContain('None detected')
  })

  it('shows empty state for matched skills when none', () => {
    const wrapper = mountResult(emptyResult)
    expect(wrapper.text()).toContain('No resume skills matched')
  })

  it('shows positive message when no missing skills', () => {
    const wrapper = mountResult(emptyResult)
    expect(wrapper.text()).toContain('None — good coverage')
  })

  it('does not render detected role row when null', () => {
    const wrapper = mountResult(emptyResult)
    expect(wrapper.text()).not.toContain('Detected Role')
  })
})
