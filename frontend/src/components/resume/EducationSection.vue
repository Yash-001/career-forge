<template>
  <div class="card">
    <div class="card-header">
      <h3 class="card-title">Education</h3>
      <button v-if="!showAddForm" class="btn btn-primary btn-sm" @click="showAddForm = true">+ Add</button>
    </div>

    <div v-if="showAddForm" class="inline-form">
      <ResumeEducationForm
        :saving="saving"
        :api-error="formError"
        submit-label="Add Education"
        @submit="handleAdd"
        @cancel="closeAdd"
      />
    </div>

    <div v-if="items.length === 0 && !showAddForm" class="empty-state">No education entries yet.</div>

    <ul v-else class="item-list">
      <li v-for="edu in items" :key="edu.id" class="item-row">
        <template v-if="editingId === edu.id">
          <ResumeEducationForm
            :initial="edu"
            :saving="saving"
            :api-error="formError"
            submit-label="Save"
            @submit="(p) => handleUpdate(edu.id, p)"
            @cancel="closeEdit"
          />
        </template>
        <template v-else>
          <div class="item-info">
            <span class="item-primary">{{ edu.degree ? `${edu.degree} — ` : '' }}{{ edu.institutionName }}</span>
            <span class="item-secondary">{{ edu.fieldOfStudy ?? '' }}{{ edu.startDate ? ` · ${edu.startDate}` : '' }}{{ edu.endDate ? ` – ${edu.endDate}` : '' }}</span>
          </div>
          <div class="item-actions">
            <button class="btn btn-ghost btn-sm" @click="startEdit(edu.id)">Edit</button>
            <button class="btn btn-danger btn-sm" :disabled="deletingId === edu.id" @click="handleDelete(edu.id)">
              <span v-if="deletingId === edu.id" class="spinner" aria-hidden="true" />
              {{ deletingId === edu.id ? '' : 'Delete' }}
            </button>
          </div>
        </template>
      </li>
    </ul>

    <div v-if="deleteError" class="api-error" role="alert" style="margin-top: 0.75rem;">{{ deleteError }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { resumeApi } from '@/api/resume'
import type { ResumeEducation, ResumeEducationPayload, ResumeVersion } from '@/api/resume'
import ResumeEducationForm from './ResumeEducationForm.vue'

const props = defineProps<{
  resumeId: string
  versionId: string
  items: ResumeEducation[]
}>()

const emit = defineEmits<{ updated: [version: ResumeVersion] }>()

const showAddForm = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)
const deletingId = ref<string | null>(null)
const deleteError = ref<string | null>(null)

function closeAdd() { showAddForm.value = false; formError.value = null }
function closeEdit() { editingId.value = null; formError.value = null }
function startEdit(id: string) { editingId.value = id; formError.value = null }

async function handleAdd(payload: ResumeEducationPayload) {
  saving.value = true
  formError.value = null
  try {
    payload.displayOrder = props.items.length
    await resumeApi.addEducation(props.resumeId, props.versionId, payload)
    const updated = await resumeApi.getVersion(props.resumeId, props.versionId)
    emit('updated', updated)
    closeAdd()
  } catch (err) {
    formError.value = resumeApi.extractError(err).message
  } finally {
    saving.value = false
  }
}

async function handleUpdate(eduId: string, payload: ResumeEducationPayload) {
  saving.value = true
  formError.value = null
  try {
    await resumeApi.updateEducation(props.resumeId, props.versionId, eduId, payload)
    const updated = await resumeApi.getVersion(props.resumeId, props.versionId)
    emit('updated', updated)
    closeEdit()
  } catch (err) {
    formError.value = resumeApi.extractError(err).message
  } finally {
    saving.value = false
  }
}

async function handleDelete(eduId: string) {
  deletingId.value = eduId
  deleteError.value = null
  try {
    await resumeApi.deleteEducation(props.resumeId, props.versionId, eduId)
    const updated = await resumeApi.getVersion(props.resumeId, props.versionId)
    emit('updated', updated)
  } catch (err) {
    deleteError.value = resumeApi.extractError(err).message
  } finally {
    deletingId.value = null
  }
}
</script>

<style scoped>
.inline-form {
  padding: 1rem 0;
  border-top: 1px solid var(--color-border);
}

.item-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.item-row {
  padding: 0.875rem 0;
  border-top: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.item-row:has(form) {
  flex-direction: column;
  align-items: stretch;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.item-primary {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
}

.item-secondary {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.item-actions {
  display: flex;
  gap: 0.375rem;
  flex-shrink: 0;
}
</style>
