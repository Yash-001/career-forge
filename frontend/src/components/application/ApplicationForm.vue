<template>
  <div class="dialog-overlay" role="dialog" aria-modal="true" :aria-labelledby="titleId" @keydown.esc="$emit('cancel')">
    <div class="dialog-box" tabindex="-1" ref="boxRef">
      <h3 class="dialog-title" :id="titleId">{{ isEdit ? 'Edit Application' : 'Add Application' }}</h3>

      <form class="form" @submit.prevent="handleSubmit" novalidate>
        <div class="form-row">
          <div class="field">
            <label for="app-company">Company <span aria-hidden="true">*</span></label>
            <input
              id="app-company"
              v-model="form.companyName"
              type="text"
              maxlength="255"
              :class="{ error: errors.companyName }"
              aria-required="true"
              :aria-describedby="errors.companyName ? 'err-company' : undefined"
            />
            <span v-if="errors.companyName" id="err-company" class="field-error" role="alert">{{ errors.companyName }}</span>
          </div>

          <div class="field">
            <label for="app-role">Role <span aria-hidden="true">*</span></label>
            <input
              id="app-role"
              v-model="form.jobTitle"
              type="text"
              maxlength="255"
              :class="{ error: errors.jobTitle }"
              aria-required="true"
              :aria-describedby="errors.jobTitle ? 'err-role' : undefined"
            />
            <span v-if="errors.jobTitle" id="err-role" class="field-error" role="alert">{{ errors.jobTitle }}</span>
          </div>
        </div>

        <div class="form-row">
          <div class="field">
            <label for="app-date">Application Date <span aria-hidden="true">*</span></label>
            <input
              id="app-date"
              v-model="form.applicationDate"
              type="date"
              :class="{ error: errors.applicationDate }"
              aria-required="true"
              :aria-describedby="errors.applicationDate ? 'err-date' : undefined"
            />
            <span v-if="errors.applicationDate" id="err-date" class="field-error" role="alert">{{ errors.applicationDate }}</span>
          </div>

          <div class="field">
            <label for="app-status">Status <span aria-hidden="true">*</span></label>
            <select id="app-status" v-model="form.status" aria-required="true">
              <option value="APPLIED">Applied</option>
              <option value="INTERVIEW">Interview</option>
              <option value="OFFER">Offer</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
        </div>

        <div class="field">
          <label for="app-url">Job URL</label>
          <input
            id="app-url"
            v-model="form.jobUrl"
            type="url"
            maxlength="2048"
            placeholder="https://..."
            :class="{ error: errors.jobUrl }"
            :aria-describedby="errors.jobUrl ? 'err-url' : undefined"
          />
          <span v-if="errors.jobUrl" id="err-url" class="field-error" role="alert">{{ errors.jobUrl }}</span>
        </div>

        <div class="field">
          <label for="app-version">Resume Version</label>
          <select id="app-version" v-model="form.resumeVersionId">
            <option value="">— None —</option>
            <option v-for="v in versions" :key="v.id" :value="v.id">
              {{ v.resumeName }} — v{{ v.versionNumber }}{{ v.title ? ` (${v.title})` : '' }}
            </option>
          </select>
          <span v-if="versionsLoading" class="field-hint">Loading versions…</span>
        </div>

        <div v-if="submitError" class="api-error" role="alert">{{ submitError }}</div>

        <div class="form-actions">
          <button type="button" class="btn btn-ghost" @click="$emit('cancel')" :disabled="saving">Cancel</button>
          <button type="submit" class="btn btn-primary" :disabled="saving">
            <span v-if="saving" class="spinner" aria-hidden="true" />
            {{ saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Add Application' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { resumeApi } from '@/api/resume'
import type { ApplicationResponse, ApplicationStatus } from '@/api/application'

interface VersionOption {
  id: string
  resumeName: string
  versionNumber: number
  title: string | null
}

const props = defineProps<{
  initial?: ApplicationResponse | null
}>()

const emit = defineEmits<{
  cancel: []
  saved: [data: {
    companyName: string
    jobTitle: string
    applicationDate: string
    jobUrl: string | null
    status: ApplicationStatus
    resumeVersionId: string | null
  }]
}>()

const isEdit = !!props.initial
const titleId = isEdit ? 'edit-app-dialog' : 'add-app-dialog'

const boxRef = ref<HTMLElement | null>(null)

const form = reactive({
  companyName: props.initial?.companyName ?? '',
  jobTitle: props.initial?.jobTitle ?? '',
  applicationDate: props.initial?.applicationDate ?? new Date().toISOString().slice(0, 10),
  jobUrl: props.initial?.jobUrl ?? '',
  status: (props.initial?.status ?? 'APPLIED') as ApplicationStatus,
  resumeVersionId: props.initial?.resumeVersionId ?? '',
})

const errors = reactive<Record<string, string>>({})
const saving = ref(false)
const submitError = ref<string | null>(null)

const versions = ref<VersionOption[]>([])
const versionsLoading = ref(false)

onMounted(async () => {
  await nextTick()
  boxRef.value?.focus()

  versionsLoading.value = true
  try {
    const resumes = await resumeApi.listResumes()
    const all: VersionOption[] = []
    for (const r of resumes) {
      const versionList = await resumeApi.listVersions(r.id)
      for (const v of versionList) {
        all.push({ id: v.id, resumeName: r.name, versionNumber: v.versionNumber, title: v.title })
      }
    }
    versions.value = all
  } catch {
    // non-critical — version selector just stays empty
  } finally {
    versionsLoading.value = false
  }
})

function validate(): boolean {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!form.companyName.trim()) errors.companyName = 'Company is required.'
  if (!form.jobTitle.trim()) errors.jobTitle = 'Role is required.'
  if (!form.applicationDate) errors.applicationDate = 'Application date is required.'
  if (form.jobUrl && !/^https?:\/\/.+/.test(form.jobUrl)) errors.jobUrl = 'Must be a valid URL starting with http:// or https://'
  return Object.keys(errors).length === 0
}

function handleSubmit() {
  if (!validate()) return
  submitError.value = null
  emit('saved', {
    companyName: form.companyName.trim(),
    jobTitle: form.jobTitle.trim(),
    applicationDate: form.applicationDate,
    jobUrl: form.jobUrl.trim() || null,
    status: form.status,
    resumeVersionId: form.resumeVersionId || null,
  })
}

defineExpose({ setSaving: (v: boolean) => { saving.value = v }, setError: (msg: string | null) => { submitError.value = msg } })
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
  padding: 1rem;
}

.dialog-box {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 1.75rem;
  width: min(560px, 100%);
  box-shadow: var(--shadow-md);
  max-height: 90vh;
  overflow-y: auto;
}

.dialog-title {
  font-size: 1.0625rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 1.25rem;
}

.field-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
</style>
