<template>
  <form class="form" @submit.prevent="handleSubmit" novalidate>
    <div class="form-row">
      <div class="field">
        <label for="edu-institution">Institution <span aria-hidden="true">*</span></label>
        <input
          id="edu-institution"
          v-model="form.institutionName"
          type="text"
          placeholder="MIT"
          :class="{ error: errors.institutionName }"
          :aria-invalid="!!errors.institutionName || undefined"
          :aria-describedby="errors.institutionName ? 'err-edu-institution' : undefined"
          aria-required="true"
          maxlength="255"
        />
        <span v-if="errors.institutionName" id="err-edu-institution" class="field-error" role="alert">{{ errors.institutionName }}</span>
      </div>
      <div class="field">
        <label for="edu-degree">Degree</label>
        <input id="edu-degree" v-model="form.degree" type="text" placeholder="Bachelor of Science" maxlength="255" />
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="edu-field">Field of Study</label>
        <input id="edu-field" v-model="form.fieldOfStudy" type="text" placeholder="Computer Science" maxlength="255" />
      </div>
      <div class="field">
        <label for="edu-location">Location</label>
        <input id="edu-location" v-model="form.location" type="text" placeholder="Cambridge, MA" maxlength="255" />
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="edu-start">Start Date</label>
        <input id="edu-start" v-model="form.startDate" type="date" />
      </div>
      <div class="field">
        <label for="edu-end">End Date</label>
        <input
          id="edu-end"
          v-model="form.endDate"
          type="date"
          :class="{ error: errors.endDate }"
          :aria-invalid="!!errors.endDate || undefined"
          :aria-describedby="errors.endDate ? 'err-edu-end' : undefined"
        />
        <span v-if="errors.endDate" id="err-edu-end" class="field-error" role="alert">{{ errors.endDate }}</span>
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="edu-grade">Grade / GPA</label>
        <input id="edu-grade" v-model="form.grade" type="text" placeholder="3.8 / 4.0" maxlength="50" />
      </div>
      <div class="field">
        <label for="edu-order">Display Order</label>
        <input id="edu-order" v-model.number="form.displayOrder" type="number" min="0" style="width: 80px" />
      </div>
    </div>

    <div class="field">
      <label for="edu-desc">Description</label>
      <textarea id="edu-desc" v-model="form.description" rows="3" placeholder="Relevant coursework, activities, honours…" />
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
import type { Education, EducationPayload } from '@/api/profile'

const props = defineProps<{
  initial?: Education | null
  saving?: boolean
  apiError?: string | null
  submitLabel?: string
}>()

const emit = defineEmits<{
  submit: [payload: EducationPayload]
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
    displayOrder: props.initial?.displayOrder ?? 0,
  }
}

const form = reactive(makeForm())
const errors = reactive<Record<string, string>>({})

watch(
  () => props.initial,
  () => Object.assign(form, makeForm()),
)

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
  const payload: EducationPayload = {
    institutionName: form.institutionName.trim(),
    degree: form.degree.trim() || undefined,
    fieldOfStudy: form.fieldOfStudy.trim() || undefined,
    location: form.location.trim() || undefined,
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    grade: form.grade.trim() || undefined,
    description: form.description.trim() || undefined,
    displayOrder: form.displayOrder,
  }
  emit('submit', payload)
}
</script>
