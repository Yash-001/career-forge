import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TailoringSuggestions from '@/components/ai/TailoringSuggestions.vue'
import type { BulletSuggestion, AcceptedSuggestion } from '@/api/ai'

const suggestion: BulletSuggestion = {
  experienceId: 'exp-1',
  originalText: 'Developed REST APIs using Spring Boot.',
  suggestedText: 'Developed REST APIs using Spring Boot. Demonstrates proficiency in java, spring boot.',
  matchedKeywords: ['java', 'spring boot'],
  rationale: 'Matched keywords from job description: java, spring boot.',
}

function mountSuggestions(suggestions: BulletSuggestion[], loading = false) {
  return mount(TailoringSuggestions, { props: { suggestions, loading } })
}

describe('TailoringSuggestions', () => {
  it('renders original bullet text', () => {
    const wrapper = mountSuggestions([suggestion])
    expect(wrapper.text()).toContain('Developed REST APIs using Spring Boot.')
  })

  it('renders tailored suggestion text', () => {
    const wrapper = mountSuggestions([suggestion])
    expect(wrapper.text()).toContain('Demonstrates proficiency in java, spring boot.')
  })

  it('renders matched keywords as badges', () => {
    const wrapper = mountSuggestions([suggestion])
    expect(wrapper.text()).toContain('java')
    expect(wrapper.text()).toContain('spring boot')
  })

  it('renders rationale', () => {
    const wrapper = mountSuggestions([suggestion])
    expect(wrapper.text()).toContain('Matched keywords from job description')
  })

  it('renders section labels', () => {
    const wrapper = mountSuggestions([suggestion])
    expect(wrapper.text()).toContain('Original')
    expect(wrapper.text()).toContain('Tailored Suggestion')
    expect(wrapper.text()).toContain('Rationale')
  })

  it('shows Demo AI badge', () => {
    const wrapper = mountSuggestions([suggestion])
    expect(wrapper.text()).toContain('Demo AI')
  })

  it('shows empty state when no suggestions', () => {
    const wrapper = mountSuggestions([])
    expect(wrapper.text()).toContain('No tailoring suggestions were generated for this resume.')
  })

  it('renders multiple suggestions', () => {
    const second: BulletSuggestion = {
      experienceId: 'exp-2',
      originalText: 'Built CI/CD pipelines.',
      suggestedText: 'Built CI/CD pipelines. Aligned with job requirements.',
      matchedKeywords: [],
      rationale: 'No direct keyword overlap found.',
    }
    const wrapper = mountSuggestions([suggestion, second])
    expect(wrapper.findAll('.suggestion-item').length).toBe(2)
  })

  it('renders Accept and Reject buttons for each suggestion', () => {
    const wrapper = mountSuggestions([suggestion])
    const buttons = wrapper.findAll('button')
    const texts = buttons.map((b) => b.text())
    expect(texts).toContain('Accept')
    expect(texts).toContain('Reject')
  })

  it('toggles accepted state on Accept click', async () => {
    const wrapper = mountSuggestions([suggestion])
    const acceptBtn = wrapper.findAll('button').find((b) => b.text() === 'Accept')
    await acceptBtn?.trigger('click')
    expect(wrapper.find('.suggestion-item--accepted').exists()).toBe(true)
    expect(wrapper.findAll('button').find((b) => b.text() === '✓ Accepted')).toBeTruthy()
  })

  it('un-toggles accepted state on second Accept click', async () => {
    const wrapper = mountSuggestions([suggestion])
    const acceptBtn = () => wrapper.findAll('button').find((b) => b.text().includes('Accept') || b.text().includes('Accepted'))!
    await acceptBtn().trigger('click')
    await acceptBtn().trigger('click')
    expect(wrapper.find('.suggestion-item--accepted').exists()).toBe(false)
  })

  it('apply button is disabled when nothing accepted', () => {
    const wrapper = mountSuggestions([suggestion])
    const applyBtn = wrapper.find('[data-testid="apply-btn"]')
    expect(applyBtn.attributes('disabled')).toBeDefined()
  })

  it('apply button is enabled after accepting a suggestion', async () => {
    const wrapper = mountSuggestions([suggestion])
    await wrapper.findAll('button').find((b) => b.text() === 'Accept')?.trigger('click')
    const applyBtn = wrapper.find('[data-testid="apply-btn"]')
    expect(applyBtn.attributes('disabled')).toBeUndefined()
  })

  it('emits apply event with accepted suggestions on apply click', async () => {
    const wrapper = mountSuggestions([suggestion])
    await wrapper.findAll('button').find((b) => b.text() === 'Accept')?.trigger('click')
    await wrapper.find('[data-testid="apply-btn"]').trigger('click')
    const emitted = wrapper.emitted('apply') as [AcceptedSuggestion[]][]
    expect(emitted).toHaveLength(1)
    expect(emitted[0]![0]).toEqual([{ experienceId: 'exp-1', suggestedText: suggestion.suggestedText }])
  })

  it('reject marks suggestion as rejected', async () => {
    const wrapper = mountSuggestions([suggestion])
    await wrapper.findAll('button').find((b) => b.text() === 'Reject')?.trigger('click')
    expect(wrapper.find('.suggestion-item--rejected').exists()).toBe(true)
  })

  it('accepting a rejected suggestion removes rejected state', async () => {
    const wrapper = mountSuggestions([suggestion])
    await wrapper.findAll('button').find((b) => b.text() === 'Reject')?.trigger('click')
    await wrapper.findAll('button').find((b) => b.text() === 'Accept')?.trigger('click')
    expect(wrapper.find('.suggestion-item--rejected').exists()).toBe(false)
    expect(wrapper.find('.suggestion-item--accepted').exists()).toBe(true)
  })
})
