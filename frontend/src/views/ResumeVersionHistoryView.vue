<template>
  <div class="page">
    <div class="page-header">
      <RouterLink :to="`/resumes/${resumeId}`" class="back-link">← Back to Editor</RouterLink>
      <h1 class="page-title">Version History</h1>
      <p v-if="resumeName" class="page-subtitle">{{ resumeName }}</p>
    </div>

    <div v-if="loadError" class="api-error" role="alert">{{ loadError }}</div>

    <div v-else-if="loading" class="skeleton-stack">
      <div v-for="n in 3" :key="n" class="skeleton" style="height: 5rem;" />
    </div>

    <ul v-else-if="versions.length" class="version-list">
      <li v-for="v in versions" :key="v.id" class="version-card card">
        <div class="version-card-main">
          <div class="version-card-info">
            <div class="version-card-top">
              <span class="version-number">Version {{ v.versionNumber }}</span>
              <span v-if="v.isLatest" class="badge">Latest</span>
            </div>
            <p v-if="v.title" class="version-card-title">{{ v.title }}</p>
            <p class="version-card-date">Created {{ formatDate(v.createdAt) }}</p>
          </div>
          <div class="version-card-actions">
            <RouterLink
              :to="`/resumes/${resumeId}?version=${v.id}`"
              class="btn btn-primary btn-sm"
            >
              Open
            </RouterLink>
          </div>
        </div>
      </li>
    </ul>

    <div v-else class="empty-state">No versions found.</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { resumeApi } from '@/api/resume'
import type { ResumeVersionSummary } from '@/api/resume'

const route = useRoute()
const resumeId = route.params.resumeId as string

const resumeName = ref('')
const versions = ref<ResumeVersionSummary[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

async function load() {
  loading.value = true
  loadError.value = null
  try {
    const [resume, versionList] = await Promise.all([
      resumeApi.getResume(resumeId),
      resumeApi.listVersions(resumeId),
    ])
    resumeName.value = resume.name
    versions.value = versionList.slice().sort((a, b) => b.versionNumber - a.versionNumber)
  } catch (err) {
    loadError.value = resumeApi.extractError(err).message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  max-width: 680px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.back-link {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  text-decoration: none;
  align-self: flex-start;
}

.back-link:hover {
  color: var(--color-primary);
}

.page-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
}

.page-subtitle {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.skeleton-stack {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.version-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.version-card {
  padding: 1.25rem 1.5rem;
}

.version-card-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.version-card-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.version-card-top {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.version-number {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
}

.version-card-title {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.version-card-date {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.version-card-actions {
  flex-shrink: 0;
}
</style>
