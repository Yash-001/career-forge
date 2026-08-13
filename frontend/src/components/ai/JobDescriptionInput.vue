<template>
  <div class="jd-input">
    <div class="field">
      <label for="job-description">Job Description</label>
      <textarea
        id="job-description"
        v-model="localValue"
        :disabled="disabled"
        :class="{ error: showError }"
        rows="8"
        placeholder="Paste the job description here…"
        maxlength="10000"
        aria-describedby="jd-char-count jd-error"
        @input="showError = false"
      />
      <div class="jd-meta">
        <span
          v-if="showError"
          id="jd-error"
          class="field-error"
          role="alert"
        >Job description must not be blank.</span>
        <span
          id="jd-char-count"
          class="char-count"
          :class="{ 'char-count--near': localValue.length > 9000 }"
          aria-live="polite"
        >{{ localValue.length }} / 10,000</span>
      </div>
    </div>

    <div class="jd-actions">
      <button
        class="btn btn-primary"
        :disabled="disabled || analyzingLoading"
        @click="handleAnalyze"
      >
        <span v-if="analyzingLoading" class="spinner" aria-hidden="true" />
        {{ analyzingLoading ? 'Analyzing…' : 'Analyze' }}
      </button>
      <button
        class="btn btn-ghost"
        :disabled="disabled || tailoringLoading || !canTailor"
        @click="handleTailor"
      >
        <span v-if="tailoringLoading" class="spinner" aria-hidden="true" />
        {{ tailoringLoading ? 'Tailoring…' : 'Tailor Resume' }}
      </button>
      <button
        v-if="localValue || hasResults"
        class="btn btn-ghost btn-sm"
        :disabled="disabled"
        @click="$emit('clear')"
      >
        Clear
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: string
  analyzingLoading?: boolean
  tailoringLoading?: boolean
  disabled?: boolean
  canTailor?: boolean
  hasResults?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  analyze: []
  tailor: []
  clear: []
}>()

const localValue = ref(props.modelValue)
const showError = ref(false)

watch(() => props.modelValue, (v) => { localValue.value = v })
watch(localValue, (v) => emit('update:modelValue', v))

function handleAnalyze() {
  if (!localValue.value.trim()) {
    showError.value = true
    return
  }
  emit('analyze')
}

function handleTailor() {
  if (!localValue.value.trim()) {
    showError.value = true
    return
  }
  emit('tailor')
}
</script>

<style scoped>
.jd-input {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.jd-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 1.25rem;
}

.char-count {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-left: auto;
}

.char-count--near {
  color: var(--color-danger);
}

.jd-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

@media (max-width: 480px) {
  .jd-actions {
    flex-direction: column;
  }

  .jd-actions .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
