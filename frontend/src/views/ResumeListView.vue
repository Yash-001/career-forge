<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-heading">My Resumes</h1>
        <p class="page-subheading">Create and manage your resume versions.</p>
      </div>
      <RouterLink to="/resumes/new" class="btn btn-primary">+ New Resume</RouterLink>
    </header>

    <!-- Loading -->
    <div v-if="store.loading" class="skeleton-wrap" aria-busy="true" aria-label="Loading resumes">
      <div class="skeleton skeleton-card" />
      <div class="skeleton skeleton-card" />
      <div class="skeleton skeleton-card" />
    </div>

    <!-- Error -->
    <div v-else-if="store.error" class="api-error" role="alert">
      {{ store.error }}
      <button class="btn btn-ghost btn-sm" style="margin-left:0.75rem" type="button" @click="store.loadResumes()">Retry</button>
    </div>

    <!-- Empty state -->
    <div v-else-if="store.resumes.length === 0" class="resume-empty">
      <div class="resume-empty-icon" aria-hidden="true">📄</div>
      <h2 class="resume-empty-title">No resumes yet</h2>
      <p class="resume-empty-body">
        Create your first resume from your Master Profile. It takes a snapshot of your experience,
        education, and skills — you can then edit it independently.
      </p>
      <RouterLink to="/resumes/new" class="btn btn-primary">Create your first resume</RouterLink>
    </div>

    <!-- Resume cards -->
    <ul v-else class="resume-grid" aria-label="Your resumes">
      <li v-for="resume in store.resumes" :key="resume.id" class="resume-card">
        <div class="resume-card-body">
          <div class="resume-card-name">
            <template v-if="renamingId === resume.id">
              <input
                class="rename-input"
                v-model="renameValue"
                @keydown.enter.prevent="submitRename(resume.id)"
                @keydown.escape="cancelRename"
                :aria-label="`Rename ${resume.name}`"
                maxlength="255"
                ref="renameInputRef"
              />
              <div v-if="renameError" class="field-error" role="alert">{{ renameError }}</div>
            </template>
            <span v-else>{{ resume.name }}</span>
          </div>
          <div class="resume-card-meta">
            <span class="badge">v{{ resume.latestVersionNumber }}</span>
            <span class="resume-card-date">Updated {{ formatDate(resume.updatedAt) }}</span>
            <span class="resume-card-date">Created {{ formatDate(resume.createdAt) }}</span>
          </div>
        </div>
        <div class="resume-card-actions">
          <template v-if="renamingId === resume.id">
            <button class="btn btn-primary btn-sm" @click="submitRename(resume.id)" :disabled="renameSaving">
              <span v-if="renameSaving" class="spinner" aria-hidden="true" />
              {{ renameSaving ? 'Saving…' : 'Save' }}
            </button>
            <button class="btn btn-ghost btn-sm" @click="cancelRename" :disabled="renameSaving">Cancel</button>
          </template>
          <template v-else>
            <RouterLink :to="`/resumes/${resume.id}`" class="btn btn-primary btn-sm">Open</RouterLink>
            <button class="btn btn-ghost btn-sm" @click="startRename(resume)" type="button" :aria-label="`Rename ${resume.name}`">Rename</button>
            <RouterLink :to="`/resumes/${resume.id}/versions`" class="btn btn-ghost btn-sm">Versions</RouterLink>
            <button class="btn btn-ghost btn-sm btn-delete" @click="confirmDelete(resume)" type="button" :aria-label="`Delete ${resume.name}`">Delete</button>
          </template>
        </div>
      </li>
    </ul>

    <!-- Delete confirmation -->
    <ConfirmDialog
      :open="!!deleteTarget"
      title="Delete Resume"
      :message="deleteTarget ? `Delete &quot;${deleteTarget.name}&quot;? All versions and content will be permanently removed.` : ''"
      :loading="deleteSaving"
      :error="deleteError"
      @confirm="executeDelete"
      @cancel="cancelDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import { useResumeStore } from '@/stores/resume'
import { resumeApi, type ResumeSummary } from '@/api/resume'
import ConfirmDialog from '@/components/profile/ConfirmDialog.vue'

const store = useResumeStore()
onMounted(() => store.loadResumes())

// ── Rename ─────────────────────────────────────────────────────────────────
const renamingId = ref<string | null>(null)
const renameValue = ref('')
const renameError = ref<string | null>(null)
const renameSaving = ref(false)
const renameInputRef = ref<HTMLInputElement | null>(null)

function startRename(resume: ResumeSummary) {
  renamingId.value = resume.id
  renameValue.value = resume.name
  renameError.value = null
  nextTick(() => renameInputRef.value?.focus())
}

function cancelRename() {
  renamingId.value = null
  renameError.value = null
}

async function submitRename(resumeId: string) {
  const name = renameValue.value.trim()
  if (!name) { renameError.value = 'Name cannot be blank.'; return }
  renameSaving.value = true
  renameError.value = null
  try {
    await store.renameResume(resumeId, name)
    renamingId.value = null
  } catch (err) {
    renameError.value = resumeApi.extractError(err).message
  } finally {
    renameSaving.value = false
  }
}

// ── Delete ─────────────────────────────────────────────────────────────────
const deleteTarget = ref<ResumeSummary | null>(null)
const deleteSaving = ref(false)
const deleteError = ref<string | null>(null)

function confirmDelete(resume: ResumeSummary) {
  deleteTarget.value = resume
  deleteError.value = null
}

function cancelDelete() {
  deleteTarget.value = null
  deleteError.value = null
}

async function executeDelete() {
  if (!deleteTarget.value) return
  deleteSaving.value = true
  deleteError.value = null
  try {
    await store.removeResume(deleteTarget.value.id)
    deleteTarget.value = null
  } catch (err) {
    deleteError.value = resumeApi.extractError(err).message
  } finally {
    deleteSaving.value = false
  }
}

// ── Formatters ─────────────────────────────────────────────────────────────
function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.page {
  max-width: 860px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.page-subheading {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
}

.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.skeleton-card {
  height: 100px;
  border-radius: var(--radius-lg);
}

/* Empty state */
.resume-empty {
  text-align: center;
  padding: 4rem 2rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.resume-empty-icon {
  font-size: 2.5rem;
}

.resume-empty-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text);
}

.resume-empty-body {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  max-width: 420px;
  line-height: 1.6;
}

/* Resume grid */
.resume-grid {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.resume-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.15s;
}

.resume-card:hover {
  border-color: #c7d2fe;
}

.resume-card-body {
  flex: 1;
  min-width: 0;
}

.resume-card-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.35rem;
}

.rename-input {
  padding: 0.3rem 0.6rem;
  border: 1px solid var(--color-border-focus);
  border-radius: var(--radius);
  font-size: 1rem;
  font-weight: 600;
  font-family: inherit;
  color: var(--color-text);
  width: 100%;
  max-width: 320px;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.rename-input:focus {
  outline: none;
}

.resume-card-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.resume-card-date {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.resume-card-actions {
  display: flex;
  gap: 0.375rem;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.btn-delete {
  color: var(--color-danger);
}

.btn-delete:hover:not(:disabled) {
  color: var(--color-danger-hover);
  border-color: var(--color-danger);
}

@media (max-width: 600px) {
  .resume-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .resume-card-actions {
    justify-content: flex-start;
  }
}
</style>
