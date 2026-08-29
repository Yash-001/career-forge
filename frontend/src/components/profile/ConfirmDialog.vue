<template>
  <div v-if="open" class="dialog-overlay" role="dialog" aria-modal="true" :aria-labelledby="'dialog-title-' + title" :aria-describedby="'dialog-msg-' + title">
    <div class="dialog-box">
      <h3 class="dialog-title" :id="'dialog-title-' + title">{{ title }}</h3>
      <p class="dialog-message" :id="'dialog-msg-' + title">{{ message }}</p>
      <div v-if="error" class="dialog-error" role="alert">{{ error }}</div>
      <div class="dialog-actions">
        <button class="btn btn-ghost" @click="$emit('cancel')" :disabled="loading">Cancel</button>
        <button class="btn btn-danger" @click="$emit('confirm')" :disabled="loading">
          <span v-if="loading" class="spinner" aria-hidden="true" />
          {{ loading ? 'Deleting…' : 'Delete' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
  message: string
  loading?: boolean
  error?: string | null
}>()
defineEmits<{ confirm: []; cancel: [] }>()
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
