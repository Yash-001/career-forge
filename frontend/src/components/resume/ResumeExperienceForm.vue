<template>
  <form class="form" @submit.prevent="handleSubmit" novalidate>
    <div class="form-row">
      <div class="field">
        <label for="rexp-company">Company *</label>
        <input
          id="rexp-company"
          v-model="form.companyName"
          type="text"
          placeholder="Acme Corp"
          :class="{ error: errors.companyName }"
          maxlength="255"
        />
        <span v-if="errors.companyName" class="field-error">{{ errors.companyName }}</span>
      </div>
      <div class="field">
        <label for="rexp-title">Job Title *</label>
        <input
          id="rexp-title"
          v-model="form.jobTitle"
          type="text"
          placeholder="Software Engineer"
          :class="{ error: errors.jobTitle }"
          maxlength="255"
        />
        <span v-if="errors.jobTitle" class="field-error">{{ errors.jobTitle }}</span>
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="rexp-location">Location</label>
        <input id="rexp-location" v-model="form.location" type="text" placeholder="San Francisco, CA" maxlength="255" />
      </div>
      <div class="field">
        <label for="rexp-type">Employment Type</label>
        <select id="rexp-type" v-model="form.employmentType">
          <option value="">— Select —</option>
          <option v-for="t in employmentTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="rexp-start">Start Date *</label>
        <input
          id="rexp-start"
          v-model="form.startDate"
          type="date"
          :class="{ error: errors.startDate }"
        />
        <span v-if="errors.startDate" class="field-error">{{ errors.startDate }}</span>
      </div>
      <div class="field">
        <label for="rexp-end">End Date</label>
        <input
          id="rexp-end"
          v-model="form.endDate"
          type="date"
          :disabled="form.currentlyWorking"
          :class="{ error: errors.endDate }"
        />
        <span v-if="errors.endDate" class="field-error">{{ errors.endDate }}</span>
      </div>
    </div>

    <div class="field field-checkbox">
      <label>
        <input type="checkbox" v-model="form.currentlyWorking" @change="onCurrentToggle" />
        I currently work here
      </label>
    </div>

    <div class="field">
      <label for="rexp-desc">Description</label>
      <textarea id="rexp-desc" v-model="form.description" rows="4" placeholder="Describe your responsibilities and achievements…" />
    </div>

    <div v-if="apiError" class="api-error" role="alert">{{ apiError }}</div>

    <div class="form-actions">
      <button type="button" class="btn btn-ghost" @click="$emit('cancel')" :disabled="saving">Cancel</button>
      <button type="submit" class="btn btn-primary" :disabled="saving">
        <span v-if="saving" class="spinner" aria-hidden="true" />
        {{ saving ? 'Saving…' : submitLabel }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { ResumeExperience, ResumeExperiencePayload } from '@/api/resume'
import type { EmploymentType } from '@/api/profile'

const props = defineProps<{
  initial?: ResumeExperience | null
  saving?: boolean
  apiError?: string | null
  submitLabel?: string
}>()

const emit = defineEmits<{
  submit: [payload: ResumeExperiencePayload]
  cancel: []
}>()

const employmentTypes: { value: EmploymentType; label: string }[] = [
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'FREELANCE', label: 'Freelance' },
]

function makeForm() {
  return {
    companyName: props.initial?.companyName ?? '',
    jobTitle: props.initial?.jobTitle ?? '',
    location: props.initial?.location ?? '',
    employmentType: (props.initial?.employmentType ?? '') as EmploymentType | '',
    startDate: props.initial?.startDate ?? '',
    endDate: props.initial?.endDate ?? '',
    currentlyWorking: props.initial?.currentlyWorking ?? false,
    description: props.initial?.description ?? '',
  }
}

const form = reactive(makeForm())
const errors = reactive<Record<string, string>>({})

watch(() => props.initial, () => Object.assign(form, makeForm()))

function onCurrentToggle() {
  if (form.currentlyWorking) form.endDate = ''
}

function validate(): boolean {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!form.companyName.trim()) errors.companyName = 'Company name is required.'
  if (!form.jobTitle.trim()) errors.jobTitle = 'Job title is required.'
  if (!form.startDate) errors.startDate = 'Start date is required.'
  if (!form.currentlyWorking && form.endDate && form.endDate < form.startDate) {
    errors.endDate = 'End date must be after start date.'
  }
  return Object.keys(errors).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('submit', {
    companyName: form.companyName.trim(),
    jobTitle: form.jobTitle.trim(),
    location: form.location.trim() || null,
    employmentType: form.employmentType || null,
    startDate: form.startDate,
    endDate: form.currentlyWorking ? null : form.endDate || null,
    currentlyWorking: form.currentlyWorking,
    description: form.description.trim() || null,
    displayOrder: props.initial?.displayOrder ?? 0,
  })
}
</script>
