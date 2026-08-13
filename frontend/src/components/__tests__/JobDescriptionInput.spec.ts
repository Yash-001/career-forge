import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import JobDescriptionInput from '@/components/ai/JobDescriptionInput.vue'

function mountInput(props = {}) {
  return mount(JobDescriptionInput, {
    props: {
      modelValue: '',
      analyzingLoading: false,
      tailoringLoading: false,
      disabled: false,
      canTailor: false,
      hasResults: false,
      ...props,
    },
  })
}

describe('JobDescriptionInput', () => {
  it('renders a textarea', () => {
    const wrapper = mountInput()
    expect(wrapper.find('textarea').exists()).toBe(true)
  })

  it('renders an accessible label', () => {
    const wrapper = mountInput()
    expect(wrapper.find('label[for="job-description"]').exists()).toBe(true)
  })

  it('renders character count', () => {
    const wrapper = mountInput({ modelValue: 'hello' })
    expect(wrapper.text()).toContain('5 / 10,000')
  })

  it('shows error when Analyze clicked with blank input', async () => {
    const wrapper = mountInput({ modelValue: '   ' })
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    expect(wrapper.text()).toContain('Job description must not be blank')
  })

  it('emits analyze when Analyze clicked with valid input', async () => {
    const wrapper = mountInput({ modelValue: 'Java Spring Boot backend engineer' })
    await wrapper.findAll('button').find((b) => b.text() === 'Analyze')?.trigger('click')
    expect(wrapper.emitted('analyze')).toBeTruthy()
  })

  it('disables textarea and buttons while loading', () => {
    const wrapper = mountInput({ analyzingLoading: true, disabled: true })
    expect(wrapper.find('textarea').attributes('disabled')).toBeDefined()
    wrapper.findAll('button').forEach((b) => {
      expect(b.attributes('disabled')).toBeDefined()
    })
  })

  it('shows Analyzing… label while analyzingLoading', () => {
    const wrapper = mountInput({ analyzingLoading: true })
    expect(wrapper.text()).toContain('Analyzing…')
  })

  it('shows Tailoring… label while tailoringLoading', () => {
    const wrapper = mountInput({ tailoringLoading: true })
    expect(wrapper.text()).toContain('Tailoring…')
  })

  it('emits tailor when Tailor Resume clicked with valid input and canTailor', async () => {
    const wrapper = mountInput({ modelValue: 'Java Spring Boot', canTailor: true })
    await wrapper.findAll('button').find((b) => b.text() === 'Tailor Resume')?.trigger('click')
    expect(wrapper.emitted('tailor')).toBeTruthy()
  })

  it('emits clear when Clear clicked', async () => {
    const wrapper = mountInput({ modelValue: 'some text', hasResults: true })
    await wrapper.findAll('button').find((b) => b.text() === 'Clear')?.trigger('click')
    expect(wrapper.emitted('clear')).toBeTruthy()
  })

  it('emits update:modelValue on textarea input', async () => {
    const wrapper = mountInput()
    await wrapper.find('textarea').setValue('new text')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })
})
