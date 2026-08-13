<template>
  <form class="form" @submit.prevent="handleSubmit" novalidate>
    <div class="form-row">
      <div class="field">
        <label for="redu-institution">Institution *</label>
        <input
          id="redu-institution"
          v-model="form.institutionName"
          type="text"
          placeholder="MIT"
          :class="{ error: errors.institutionName }"
          maxlength="255"
        />
        <span v-if="errors.institutionName" class="field-error">{{ errors.institutionName }}</span>
      </div>
      <div class="field">
        <label for="redu-degree">Degree</label>
        <input id="redu-degree" v-model="form.degree" type="text" placeholder="Bachelor of Science" maxlength="255" />
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="redu-field">Field of Study</label>
        <input id="redu-field" v-model="form.fieldOfStudy" type="text" placeholder="Computer Science" maxlength="255" />
      </div>
      <div class="field">
        <label for="redu-location">Location</label>
        <input id="redu-location" v-model="form.location" type="text" placeholder="Cambridge, MA" maxlength="255" />
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="redu-start">Start Date</label>
        <input id="redu-start" v-model="form.startDate" type="date" />
      </div>
      <div class="field">
        <label for="redu-end">End Date</label>
        <input
          id="redu-end"
          v-model="form.endDate"
          type="date"
          :class="{ error: errors.endDate }"
        />
        <span v-if="errors.endDate" class="field-error">{{ errors.endDate }}</span>
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="redu-grade">Grade / GPA</label>
        <input id="redu-grade" v-model="form.grade" type="text" placeholder="3.8 / 4.0" maxlength="50" />
      </div>
    </div>

    <div class="field">
      <label for="redu-desc">Description</label>
      <textarea id="redu-desc" v-model="form.description" rows="3" placeholder="Relevant coursework, activities, honours…" />
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
import type { ResumeEducation, ResumeEducationPayload } from '@/api/resume'

const props = defineProps<{
  initial?: ResumeEducation | null
  saving?: boolean
  apiError?: string | null
  submitLabel?: string
}>()

const emit = defineEmits<{
  submit: [payload: ResumeEducationPayload]
  cancel: []
}>()

function makeForm() {
  return {
    institutionName: props.initial?.institutionName ?? '',
    degree: props.initial?.degree ?? '',
    fieldOfStudy: props.initial?.fieldOfStudy ?? '',
    location: props.initial?.location ?? '',
    startDate: props.initial?.startDate ?? '',
    endDate: props.initial?.endDate ?? '',
    grade: props.initial?.grade ?? '',
    description: props.initial?.description ?? '',
  }
}

const form = reactive(makeForm())
const errors = reactive<Record<string, string>>({})

watch(() => props.initial, () => Object.assign(form, makeForm()))

function validate(): boolean {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!form.institutionName.trim()) errors.institutionName = 'Institution name is required.'
  if (form.startDate && form.endDate && form.endDate < form.startDate) {
    errors.endDate = 'End date must be after start date.'
  }
  return Object.keys(errors).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('submit', {
    institutionName: form.institutionName.trim(),
    degree: form.degree.trim() || null,
    fieldOfStudy: form.fieldOfStudy.trim() || null,
    location: form.location.trim() || null,
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    grade: form.grade.trim() || null,
    description: form.description.trim() || null,
    displayOrder: props.initial?.displayOrder ?? 0,
  })
}
</script>
