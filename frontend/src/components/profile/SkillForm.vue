<template>
  <form class="form" @submit.prevent="handleSubmit" novalidate>
    <div class="form-row">
      <div class="field">
        <label for="skill-name">Skill *</label>
        <input
          id="skill-name"
          v-model="form.name"
          type="text"
          placeholder="TypeScript"
          :class="{ error: errors.name }"
          maxlength="100"
        />
        <span v-if="errors.name" class="field-error">{{ errors.name }}</span>
      </div>
      <div class="field">
        <label for="skill-category">Category</label>
        <input id="skill-category" v-model="form.category" type="text" placeholder="Programming Languages" maxlength="100" />
      </div>
    </div>

    <div class="form-row">
      <div class="field">
        <label for="skill-proficiency">Proficiency</label>
        <select id="skill-proficiency" v-model="form.proficiency">
          <option value="">— Select —</option>
          <option v-for="p in proficiencyLevels" :key="p.value" :value="p.value">{{ p.label }}</option>
        </select>
      </div>
      <div class="field">
        <label for="skill-order">Display Order</label>
        <input id="skill-order" v-model.number="form.displayOrder" type="number" min="0" style="width: 80px" />
      </div>
    </div>

    <div v-if="apiError" class="api-error">{{ apiError }}</div>

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
import type { Skill, SkillPayload, ProficiencyLevel } from '@/api/profile'

const props = defineProps<{
  initial?: Skill | null
  saving?: boolean
  apiError?: string | null
  submitLabel?: string
}>()

const emit = defineEmits<{
  submit: [payload: SkillPayload]
  cancel: []
}>()

const proficiencyLevels: { value: ProficiencyLevel; label: string }[] = [
  { value: 'BEGINNER', label: 'Beginner' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
  { value: 'EXPERT', label: 'Expert' },
]

function makeForm() {
  return {
    name: props.initial?.name ?? '',
    category: props.initial?.category ?? '',
    proficiency: (props.initial?.proficiency ?? '') as ProficiencyLevel | '',
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
  if (!form.name.trim()) errors.name = 'Skill name is required.'
  return Object.keys(errors).length === 0
}

function handleSubmit() {
  if (!validate()) return
  const payload: SkillPayload = {
    name: form.name.trim(),
    category: form.category.trim() || undefined,
    proficiency: form.proficiency || null,
    displayOrder: form.displayOrder,
  }
  emit('submit', payload)
}
</script>
