<template>
  <div class="card">
    <div class="card-header">
      <h3 class="card-title">Skills</h3>
      <button v-if="!showAddForm" class="btn btn-primary btn-sm" @click="showAddForm = true">+ Add</button>
    </div>

    <div v-if="showAddForm" class="inline-form">
      <ResumeSkillForm
        :saving="saving"
        :api-error="formError"
        submit-label="Add Skill"
        @submit="handleAdd"
        @cancel="closeAdd"
      />
    </div>

    <div v-if="items.length === 0 && !showAddForm" class="empty-state">No skills added yet.</div>

    <ul v-else class="item-list">
      <li v-for="skill in items" :key="skill.id" class="item-row">
        <template v-if="editingId === skill.id">
          <ResumeSkillForm
            :initial="skill"
            :saving="saving"
            :api-error="formError"
            submit-label="Save"
            @submit="(p) => handleUpdate(skill.id, p)"
            @cancel="closeEdit"
          />
        </template>
        <template v-else>
          <div class="item-info">
            <span class="item-primary">{{ skill.name }}</span>
            <span class="item-secondary">
              {{ [skill.category, skill.proficiency].filter(Boolean).join(' · ') }}
            </span>
          </div>
          <div class="item-actions">
            <button class="btn btn-ghost btn-sm" @click="startEdit(skill.id)">Edit</button>
            <button class="btn btn-danger btn-sm" :disabled="deletingId === skill.id" @click="handleDelete(skill.id)">
              <span v-if="deletingId === skill.id" class="spinner" aria-hidden="true" />
              {{ deletingId === skill.id ? '' : 'Delete' }}
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
import type { ResumeSkill, ResumeSkillPayload, ResumeVersion } from '@/api/resume'
import ResumeSkillForm from './ResumeSkillForm.vue'

const props = defineProps<{
  resumeId: string
  versionId: string
  items: ResumeSkill[]
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

async function handleAdd(payload: ResumeSkillPayload) {
  saving.value = true
  formError.value = null
  try {
    payload.displayOrder = props.items.length
    await resumeApi.addSkill(props.resumeId, props.versionId, payload)
    const updated = await resumeApi.getVersion(props.resumeId, props.versionId)
    emit('updated', updated)
    closeAdd()
  } catch (err) {
    formError.value = resumeApi.extractError(err).message
  } finally {
    saving.value = false
  }
}

async function handleUpdate(skillId: string, payload: ResumeSkillPayload) {
  saving.value = true
  formError.value = null
  try {
    await resumeApi.updateSkill(props.resumeId, props.versionId, skillId, payload)
    const updated = await resumeApi.getVersion(props.resumeId, props.versionId)
    emit('updated', updated)
    closeEdit()
  } catch (err) {
    formError.value = resumeApi.extractError(err).message
  } finally {
    saving.value = false
  }
}

async function handleDelete(skillId: string) {
  deletingId.value = skillId
  deleteError.value = null
  try {
    await resumeApi.deleteSkill(props.resumeId, props.versionId, skillId)
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
