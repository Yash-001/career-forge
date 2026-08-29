<template>
  <div
    v-if="open"
    class="dialog-overlay"
    role="dialog"
    aria-modal="true"
    :aria-labelledby="titleId"
    :aria-describedby="msgId"
    @keydown.esc="$emit('cancel')"
  >
    <div class="dialog-box" ref="boxRef" tabindex="-1">
      <h3 class="dialog-title" :id="titleId">{{ title }}</h3>
      <p class="dialog-message" :id="msgId">{{ message }}</p>
      <div v-if="error" class="dialog-error" role="alert">{{ error }}</div>
      <div class="dialog-actions">
        <button class="btn btn-ghost" ref="cancelRef" @click="$emit('cancel')" :disabled="loading">Cancel</button>
        <button class="btn btn-danger" @click="$emit('confirm')" :disabled="loading">
          <span v-if="loading" class="spinner" aria-hidden="true" />
          {{ loading ? 'Deleting…' : 'Delete' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = defineProps<{
  open: boolean
  title: string
  message: string
  loading?: boolean
  error?: string | null
}>()
defineEmits<{ confirm: []; cancel: [] }>()

const boxRef = ref<HTMLElement | null>(null)
const cancelRef = ref<HTMLElement | null>(null)

// Stable IDs derived from a slug of the title (spaces → dashes, lowercase)
const slug = props.title.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '')
const titleId = `dialog-title-${slug}`
const msgId = `dialog-msg-${slug}`

watch(
  () => props.open,
  async (isOpen) => {
    if (isOpen) {
      await nextTick()
      cancelRef.value?.focus()
    }
  },
)
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.dialog-box {
  background: #fff;
  border-radius: 8px;
  padding: 1.5rem;
  width: min(420px, 90vw);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.16);
  outline: none;
}
.dialog-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #111827;
}
.dialog-message {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 1.25rem;
}
.dialog-error {
  padding: 0.5rem 0.75rem;
  background: var(--color-error-bg);
  color: var(--color-error-text);
  border: 1px solid #fecaca;
  border-radius: var(--radius);
  font-size: 0.8125rem;
  margin-bottom: 1rem;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
