<template>
  <div class="page">
    <header class="page-header">
      <RouterLink to="/resumes" class="back-link" aria-label="Back to My Resumes">← My Resumes</RouterLink>
      <h1 class="page-heading">Create Resume</h1>
    </header>

    <div class="create-card card">
      <!-- Step indicator -->
      <div class="steps" aria-label="Creation steps">
        <div class="step" :class="{ active: step === 1, done: step > 1 }" :aria-current="step === 1 ? 'step' : undefined">
          <span class="step-num" aria-hidden="true">1</span>
          <span class="step-label">Name your resume</span>
        </div>
        <div class="step-divider" aria-hidden="true" />
        <div class="step" :class="{ active: step === 2 }" :aria-current="step === 2 ? 'step' : undefined">
          <span class="step-num" aria-hidden="true">2</span>
          <span class="step-label">Snapshot profile</span>
        </div>
      </div>

      <!-- Step 1: Name -->
      <div v-if="step === 1" class="step-body">
        <h2 class="step-heading">What would you like to call this resume?</h2>
        <p class="step-hint">You can rename it later. Use something descriptive like "Software Engineer — Google" or "General Application".</p>
        <form class="form" @submit.prevent="goToStep2" novalidate>
          <div class="field">
            <label for="resume-name">Resume Name *</label>
            <input
              id="resume-name"
              v-model="name"
              type="text"
              placeholder="e.g. Software Engineer — Google"
              :class="{ error: nameError }"
              maxlength="255"
              autofocus
            />
            <span v-if="nameError" class="field-error">{{ nameError }}</span>
          </div>
          <div class="form-actions">
            <RouterLink to="/resumes" class="btn btn-ghost">Cancel</RouterLink>
            <button type="submit" class="btn btn-primary">Continue →</button>
          </div>
        </form>
      </div>

      <!-- Step 2: Confirm snapshot -->
      <div v-if="step === 2" class="step-body">
        <h2 class="step-heading">Create a snapshot from your Master Profile</h2>

        <div class="snapshot-notice">
          <div class="snapshot-notice-icon" aria-hidden="true">📸</div>
          <div>
            <p class="snapshot-notice-title">This creates a snapshot of your Master Profile.</p>
            <p class="snapshot-notice-body">
              Your current experience, education, and skills will be copied into this resume.
              Future changes to your Master Profile will <strong>not</strong> automatically update this resume —
              keeping your submitted resumes stable.
            </p>
          </div>
        </div>

        <div class="resume-name-preview">
          Creating: <strong>{{ name }}</strong>
        </div>

        <div v-if="createError" class="api-error" role="alert">{{ createError }}</div>

        <div class="form-actions">
          <button class="btn btn-ghost" @click="step = 1" :disabled="creating">← Back</button>
          <button class="btn btn-primary" @click="createResume" :disabled="creating">
            <span v-if="creating" class="spinner" aria-hidden="true" />
            {{ creating ? 'Creating…' : 'Create Resume' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { resumeApi } from '@/api/resume'

const router = useRouter()

const step = ref(1)
const name = ref('')
const nameError = ref<string | null>(null)
const creating = ref(false)
const createError = ref<string | null>(null)

function goToStep2() {
  nameError.value = null
  if (!name.value.trim()) {
    nameError.value = 'Resume name is required.'
    return
  }
  step.value = 2
}

async function createResume() {
  creating.value = true
  createError.value = null
  try {
    const resume = await resumeApi.createResume({ name: name.value.trim() })
    router.push(`/resumes/${resume.id}`)
  } catch (err) {
    createError.value = resumeApi.extractError(err).message
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 640px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.back-link {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  text-decoration: none;
}

.back-link:hover {
  color: var(--color-primary);
}

.page-heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.create-card {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Steps */
.steps {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.step {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  opacity: 0.4;
}

.step.active,
.step.done {
  opacity: 1;
}

.step-num {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: var(--color-border);
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step.active .step-num {
  background: var(--color-primary);
  color: #fff;
}

.step.done .step-num {
  background: #10b981;
  color: #fff;
}

.step-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
}

.step-divider {
  flex: 1;
  height: 1px;
  background: var(--color-border);
  min-width: 2rem;
}

/* Step body */
.step-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.step-heading {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text);
}

.step-hint {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-top: -0.75rem;
}

/* Snapshot notice */
.snapshot-notice {
  display: flex;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: var(--radius);
}

.snapshot-notice-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.snapshot-notice-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 0.35rem;
}

.snapshot-notice-body {
  font-size: 0.875rem;
  color: #1e3a8a;
  line-height: 1.6;
}

.resume-name-preview {
  font-size: 0.9375rem;
  color: var(--color-text-muted);
  padding: 0.75rem 1rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}
</style>
