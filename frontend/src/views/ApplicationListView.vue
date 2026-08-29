<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-heading">Job Applications</h1>
        <p class="page-subheading">Track your job search pipeline.</p>
      </div>
      <button class="btn btn-primary" @click="openCreate" type="button">+ Add Application</button>
    </header>

    <!-- Filters -->
    <div class="filters" role="search" aria-label="Filter applications">
      <input
        v-model="searchQuery"
        type="search"
        class="filter-search"
        placeholder="Search company or role…"
        aria-label="Search by company or role"
      />
      <div class="filter-statuses" role="group" aria-label="Filter by status">
        <button
          v-for="opt in statusOptions"
          :key="opt.value"
          type="button"
          class="filter-btn"
          :class="{ active: statusFilter === opt.value }"
          @click="statusFilter = opt.value"
          :aria-pressed="statusFilter === opt.value"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="skeleton-wrap" aria-busy="true" aria-label="Loading applications">
      <div class="skeleton skeleton-card" />
      <div class="skeleton skeleton-card" />
      <div class="skeleton skeleton-card" />
    </div>

    <!-- Error -->
    <div v-else-if="store.error" class="api-error" role="alert">
      {{ store.error }}
      <button class="btn btn-ghost btn-sm" style="margin-left:0.75rem" type="button" @click="store.loadApplications()">Retry</button>
    </div>

    <!-- Empty state (no applications at all) -->
    <div v-else-if="store.applications.length === 0" class="app-empty">
      <div class="app-empty-icon" aria-hidden="true">📋</div>
      <h2 class="app-empty-title">No applications yet</h2>
      <p class="app-empty-body">Start tracking your job search by adding your first application.</p>
      <button class="btn btn-primary" @click="openCreate" type="button">Add your first application</button>
    </div>

    <!-- Empty filtered state -->
    <div v-else-if="filtered.length === 0" class="app-empty">
      <div class="app-empty-icon" aria-hidden="true">🔍</div>
      <h2 class="app-empty-title">No results</h2>
      <p class="app-empty-body">No applications match your current filters.</p>
    </div>

    <!-- Application list -->
    <ul v-else class="app-list" aria-label="Job applications">
      <li v-for="app in filtered" :key="app.id" class="app-card">
        <div class="app-card-body">
          <div class="app-card-top">
            <span class="app-company">{{ app.companyName }}</span>
            <StatusBadge :status="app.status" />
          </div>
          <div class="app-role">{{ app.jobTitle }}</div>
          <div class="app-card-meta">
            <span class="app-date">{{ formatDate(app.applicationDate) }}</span>
            <span v-if="app.resumeVersionTitle || app.resumeVersionNumber" class="badge badge-neutral">
              v{{ app.resumeVersionNumber }}{{ app.resumeVersionTitle ? ` — ${app.resumeVersionTitle}` : '' }}
            </span>
            <a
              v-if="app.jobUrl"
              :href="app.jobUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="app-url-link"
              :aria-label="`Job posting for ${app.jobTitle} at ${app.companyName} (opens in new tab)`"
            >
              View posting ↗
            </a>
          </div>
        </div>
        <div class="app-card-actions">
          <button
            class="btn btn-ghost btn-sm"
            type="button"
            @click="openEdit(app)"
            :aria-label="`Edit application for ${app.jobTitle} at ${app.companyName}`"
          >
            Edit
          </button>
          <button
            class="btn btn-ghost btn-sm btn-delete"
            type="button"
            @click="confirmDelete(app)"
            :aria-label="`Delete application for ${app.jobTitle} at ${app.companyName}`"
          >
            Delete
          </button>
        </div>
      </li>
    </ul>

    <!-- Create / Edit form -->
    <ApplicationForm
      v-if="showForm"
      :initial="editTarget"
      @cancel="closeForm"
      @saved="handleSaved"
      ref="formRef"
    />

    <!-- Delete confirmation -->
    <ConfirmDialog
      :open="!!deleteTarget"
      title="Delete Application"
      :message="deleteTarget ? `Delete the application for &quot;${deleteTarget.jobTitle}&quot; at ${deleteTarget.companyName}? This cannot be undone.` : ''"
      :loading="deleteSaving"
      :error="deleteError"
      @confirm="executeDelete"
      @cancel="cancelDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useApplicationStore } from '@/stores/application'
import { applicationApi, type ApplicationResponse, type ApplicationStatus } from '@/api/application'
import StatusBadge from '@/components/application/StatusBadge.vue'
import ApplicationForm from '@/components/application/ApplicationForm.vue'
import ConfirmDialog from '@/components/profile/ConfirmDialog.vue'

const store = useApplicationStore()
onMounted(() => store.loadApplications())

// ── Filters ────────────────────────────────────────────────────────────────
const searchQuery = ref('')
const statusFilter = ref<ApplicationStatus | 'ALL'>('ALL')

const statusOptions: { value: ApplicationStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'APPLIED', label: 'Applied' },
  { value: 'INTERVIEW', label: 'Interview' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'REJECTED', label: 'Rejected' },
]

const filtered = computed(() => {
  const q = searchQuery.value.toLowerCase()
  return store.applications.filter((a) => {
    const matchesStatus = statusFilter.value === 'ALL' || a.status === statusFilter.value
    const matchesSearch = !q || a.companyName.toLowerCase().includes(q) || a.jobTitle.toLowerCase().includes(q)
    return matchesStatus && matchesSearch
  })
})

// ── Create / Edit ──────────────────────────────────────────────────────────
const showForm = ref(false)
const editTarget = ref<ApplicationResponse | null>(null)
const formRef = ref<InstanceType<typeof ApplicationForm> | null>(null)

function openCreate() {
  editTarget.value = null
  showForm.value = true
}

function openEdit(app: ApplicationResponse) {
  editTarget.value = app
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editTarget.value = null
}

async function handleSaved(data: {
  companyName: string
  jobTitle: string
  applicationDate: string
  jobUrl: string | null
  status: ApplicationStatus
  resumeVersionId: string | null
}) {
  formRef.value?.setSaving(true)
  formRef.value?.setError(null)
  try {
    if (editTarget.value) {
      await store.editApplication(editTarget.value.id, { ...data, status: data.status })
    } else {
      await store.addApplication(data)
    }
    closeForm()
  } catch (err) {
    formRef.value?.setError(applicationApi.extractError(err).message)
  } finally {
    formRef.value?.setSaving(false)
  }
}

// ── Delete ─────────────────────────────────────────────────────────────────
const deleteTarget = ref<ApplicationResponse | null>(null)
const deleteSaving = ref(false)
const deleteError = ref<string | null>(null)

function confirmDelete(app: ApplicationResponse) {
  deleteTarget.value = app
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
    await store.removeApplication(deleteTarget.value.id)
    deleteTarget.value = null
  } catch (err) {
    deleteError.value = applicationApi.extractError(err).message
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
  max-width: 900px;
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

/* Filters */
.filters {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  align-items: center;
}

.filter-search {
  padding: 0.45rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font-size: 0.875rem;
  font-family: inherit;
  color: var(--color-text);
  background: var(--color-surface);
  width: 220px;
  transition: border-color 0.15s;
}

.filter-search:focus {
  outline: none;
  border-color: var(--color-border-focus);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.filter-statuses {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.8125rem;
  font-weight: 500;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.filter-btn:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.filter-btn.active {
  background: #ede9fe;
  color: var(--color-primary);
  border-color: #c4b5fd;
}

/* Skeleton */
.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.skeleton-card {
  height: 90px;
  border-radius: var(--radius-lg);
}

/* Empty state */
.app-empty {
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

.app-empty-icon {
  font-size: 2.5rem;
}

.app-empty-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text);
}

.app-empty-body {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  max-width: 380px;
  line-height: 1.6;
}

/* Application list */
.app-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.app-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.125rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.15s;
}

.app-card:hover {
  border-color: #c7d2fe;
}

.app-card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.app-card-top {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex-wrap: wrap;
}

.app-company {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.app-role {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.app-card-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 0.125rem;
}

.app-date {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.app-url-link {
  font-size: 0.8125rem;
  color: var(--color-primary);
  text-decoration: none;
}

.app-url-link:hover {
  text-decoration: underline;
}

.app-card-actions {
  display: flex;
  gap: 0.375rem;
  flex-shrink: 0;
}

.btn-delete {
  color: var(--color-danger);
}

.btn-delete:hover:not(:disabled) {
  color: var(--color-danger-hover);
  border-color: var(--color-danger);
}

@media (max-width: 600px) {
  .app-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .app-card-actions {
    justify-content: flex-start;
  }

  .filter-search {
    width: 100%;
  }
}
</style>
