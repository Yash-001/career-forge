<template>
  <form class="form" @submit.prevent="handleSubmit" novalidate>
    <div class="form-row">
      <div class="field">
        <label for="exp-company">Company <span aria-hidden="true">*</span></label>
        <input
          id="exp-company"
          v-model="form.companyName"
          type="text"
          placeholder="Acme Corp"
          :class="{ error: errors.companyName }"
          :aria-invalid="!!errors.companyName || undefined"
          :aria-describedby="errors.companyName ? 'err-exp-company' : undefined"
          aria-required="true"
          maxlength="255"
        />
        <span v-if="errors.companyName" id="err-exp-company" class="field-error" role="alert">{{ errors.companyName }}</span>
      </div>
      <div class="field">
        <label for="exp-title">Job Title <span aria-hidden="true">*</span></label>
        <input
          id="exp-title"
          v-model="form.jobTitle"
          type="text"
          placeholder="Software Engineer"
          :class="{ error: errors.jobTitle }"
          :aria-invalid="!!errors.jobTitle || undefined"
          :aria-describedby="errors.jobTitle ? 'err-exp-title' : undefined"
          aria-required="true"
          maxlength="255"
        />
        <span v-if="errors.jobTitle" id="err-exp-title" class="field-error" role="alert">{{ errors.jobTitle }}</span>
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="exp-location">Location</label>
        <input id="exp-location" v-model="form.location" type="text" placeholder="San Francisco, CA" maxlength="255" />
      </div>
      <div class="field">
        <label for="exp-type">Employment Type</label>
        <select id="exp-type" v-model="form.employmentType">
          <option value="">— Select —</option>
          <option v-for="t in employmentTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="exp-start">Start Date <span aria-hidden="true">*</span></label>
        <input
          id="exp-start"
          v-model="form.startDate"
          type="date"
          aria-required="true"
          :aria-invalid="!!errors.startDate || undefined"
          :aria-describedby="errors.startDate ? 'err-exp-start' : undefined"
          :class="{ error: errors.startDate }"
        />
        <span v-if="errors.startDate" id="err-exp-start" class="field-error" role="alert">{{ errors.startDate }}</span>
      </div>
      <div class="field">
        <label for="exp-end">End Date</label>
        <input
          id="exp-end"
          v-model="form.endDate"
          type="date"
          :disabled="form.currentlyWorking"
          :aria-invalid="!!errors.endDate || undefined"
          :aria-describedby="errors.endDate ? 'err-exp-end' : undefined"
          :class="{ error: errors.endDate }"
        />
        <span v-if="errors.endDate" id="err-exp-end" class="field-error" role="alert">{{ errors.endDate }}</span>
      </div>
    </div>

    <div class="field field-checkbox">
      <label>
        <input type="checkbox" v-model="form.currentlyWorking" @change="onCurrentToggle" />
        I currently work here
      </label>
    </div>

    <div class="field">
      <label for="exp-desc">Description</label>
      <textarea id="exp-desc" v-model="form.description" rows="4" placeholder="Describe your responsibilities and achievements…" />
    </div>

    <div class="field">
      <label for="exp-order">Display Order</label>
      <input id="exp-order" v-model.number="form.displayOrder" type="number" min="0" style="width: 80px" />
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
import type { WorkExperience, WorkExperiencePayload, EmploymentType } from '@/api/profile'

const props = defineProps<{
  initial?: WorkExperience | null
  saving?: boolean
  apiError?: string | null
  submitLabel?: string
}>()

const emit = defineEmits<{
  submit: [payload: WorkExperiencePayload]
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
    displayOrder: props.initial?.displayOrder ?? 0,
  }
}

const form = reactive(makeForm())
const errors = reactive<Record<string, string>>({})

watch(
  () => props.initial,
  () => Object.assign(form, makeForm()),
)

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
  const payload: WorkExperiencePayload = {
    companyName: form.companyName.trim(),
    jobTitle: form.jobTitle.trim(),
    location: form.location.trim() || undefined,
    employmentType: form.employmentType || null,
    startDate: form.startDate,
    endDate: form.currentlyWorking ? null : form.endDate || null,
    currentlyWorking: form.currentlyWorking,
    description: form.description.trim() || undefined,
    displayOrder: form.displayOrder,
  }
  emit('submit', payload)
}
</script>
