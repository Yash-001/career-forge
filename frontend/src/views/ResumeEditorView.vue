<template>
  <div class="editor-layout">
    <!-- Sidebar -->
    <aside class="editor-sidebar" aria-label="Resume navigation">
      <div class="sidebar-header">
        <RouterLink to="/resumes" class="back-link">← Resumes</RouterLink>
        <h2 class="resume-name">{{ resumeName }}</h2>
      </div>

      <div class="version-box">
        <div class="version-box-header">
          <span class="card-title">Version {{ currentVersion?.versionNumber }}</span>
          <span v-if="currentVersion?.isLatest" class="badge">Latest</span>
        </div>
        <p v-if="currentVersion?.title" class="version-title-text">{{ currentVersion.title }}</p>
        <RouterLink :to="`/resumes/${resumeId}/versions`" class="btn btn-ghost btn-sm version-history-link">
          Version History
        </RouterLink>
        <button class="btn btn-primary btn-sm" :disabled="branching" @click="createNewVersion">
          <span v-if="branching" class="spinner" aria-hidden="true" />
          {{ branching ? 'Branching…' : 'New Version' }}
        </button>
        <div v-if="branchError" class="export-error" role="alert">{{ branchError }}</div>
        <button
          class="btn btn-ghost btn-sm"
          :disabled="exporting || !currentVersion"
          :aria-busy="exporting"
          aria-label="Export current version as PDF"
          data-testid="export-pdf-btn"
          @click="exportPdf"
        >
          <span v-if="exporting" class="spinner" aria-hidden="true" />
          {{ exporting ? 'Exporting…' : 'Export PDF' }}
        </button>
        <div v-if="exportError" class="export-error" role="alert" data-testid="export-error">
          <template v-if="exportLimitReached">
            You've reached your 3 monthly PDF exports.
            <RouterLink to="/billing" class="export-limit-hint">Upgrade to Pro for unlimited exports.</RouterLink>
          </template>
          <template v-else>{{ exportError }}</template>
        </div>
      </div>

      <nav class="section-nav" aria-label="Resume sections">
        <button
          v-for="s in sections"
          :key="s.key"
          class="nav-item"
          :class="{ active: activeSection === s.key }"
          :aria-current="activeSection === s.key ? 'true' : undefined"
          @click="activeSection = s.key"
        >
          {{ s.label }}
        </button>
        <div class="nav-divider" />
        <button
          class="nav-item nav-item--ai"
          :class="{ active: activeSection === 'ai' }"
          :aria-current="activeSection === 'ai' ? 'true' : undefined"
          @click="activeSection = 'ai'"
        >
          AI Tailoring
        </button>
      </nav>
    </aside>

    <!-- Main content -->
    <main class="editor-main" aria-label="Resume editor">
      <div v-if="loadError" class="api-error" role="alert">
        {{ loadError }}
        <button class="btn btn-ghost btn-sm" style="margin-left:0.75rem" type="button" @click="load">Retry</button>
      </div>

      <div v-else-if="loading" class="skeleton-stack">
        <div class="skeleton" style="height: 2rem; width: 40%;" />
        <div class="skeleton" style="height: 1rem; width: 60%;" />
        <div class="skeleton" style="height: 8rem;" />
      </div>

      <template v-else-if="currentVersion">
        <!-- Summary -->
        <section v-if="activeSection === 'summary'" class="editor-section">
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Version Details</h3>
            </div>

            <div class="field">
              <label for="ver-title">Version Title</label>
              <input
                id="ver-title"
                v-model="metaForm.title"
                type="text"
                placeholder="e.g. Google SWE Application"
                maxlength="255"
              />
            </div>

            <div class="field" style="margin-top: 1rem;">
              <label for="ver-summary">Professional Summary</label>
              <textarea
                id="ver-summary"
                v-model="metaForm.professionalSummary"
                rows="6"
                placeholder="A brief overview of your professional background and goals…"
              />
            </div>

            <div v-if="metaError" class="api-error" role="alert" style="margin-top: 0.75rem;">{{ metaError }}</div>
            <div v-if="metaSaved" class="save-success" role="status" style="margin-top: 0.75rem;">Saved!</div>

            <div class="form-actions" style="margin-top: 1rem;">
              <button class="btn btn-primary" :disabled="metaSaving" @click="saveMeta">
                <span v-if="metaSaving" class="spinner" aria-hidden="true" />
                {{ metaSaving ? 'Saving…' : 'Save' }}
              </button>
            </div>
          </div>
        </section>

        <!-- Experience -->
        <section v-else-if="activeSection === 'experience'" class="editor-section">
          <ExperienceSection
            :resume-id="resumeId"
            :version-id="currentVersion.id"
            :items="currentVersion.experiences"
            @updated="onVersionUpdated"
          />
        </section>

        <!-- Education -->
        <section v-else-if="activeSection === 'education'" class="editor-section">
          <EducationSection
            :resume-id="resumeId"
            :version-id="currentVersion.id"
            :items="currentVersion.educations"
            @updated="onVersionUpdated"
          />
        </section>

        <!-- Skills -->
        <section v-else-if="activeSection === 'skills'" class="editor-section">
          <SkillsSection
            :resume-id="resumeId"
            :version-id="currentVersion.id"
            :items="currentVersion.skills"
            @updated="onVersionUpdated"
          />
        </section>

        <!-- AI Tailoring -->
        <section v-else-if="activeSection === 'ai'" class="editor-section">
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">AI Tailoring</h3>
            </div>
            <p class="ai-intro">Paste a job description to analyze how well this resume matches the role and generate tailored bullet suggestions.</p>

            <div v-if="aiError" class="api-error" role="alert" style="margin-bottom: 1rem;">{{ aiError }}</div>

            <JobDescriptionInput
              v-model="aiStore.jobDescription"
              :analyzing-loading="aiStore.analyzingLoading"
              :tailoring-loading="aiStore.tailoringLoading"
              :can-tailor="!!aiStore.analysisResult"
              :has-results="!!aiStore.analysisResult || !!aiStore.tailoringResult"
              @analyze="handleAnalyze"
              @tailor="handleTailor"
              @clear="aiStore.clearResults()"
            />
          </div>

          <!-- Analysis loading -->
          <div v-if="aiStore.analyzingLoading" class="ai-status" role="status" aria-live="polite">
            <span class="spinner" aria-hidden="true" />
            Analyzing job description…
          </div>

          <!-- Tailoring loading -->
          <div v-else-if="aiStore.tailoringLoading" class="ai-status" role="status" aria-live="polite">
            <span class="spinner" aria-hidden="true" />
            Tailoring resume…
          </div>

          <!-- Analysis result -->
          <div v-else-if="aiStore.analysisResult" class="card">
            <JobAnalysisResult :result="aiStore.analysisResult" />
          </div>

          <!-- Tailoring result -->
          <div v-if="aiStore.tailoringResult" class="card">
            <TailoringSuggestions
              :suggestions="aiStore.tailoringResult.suggestions"
              :loading="aiStore.acceptingLoading"
              @apply="handleAcceptTailoring"
            />
          </div>

          <!-- Accept success -->
          <div v-if="acceptSuccess" class="save-success" role="status" aria-live="polite">
            New tailored version created! Navigating…
          </div>
          <div
            v-if="!aiStore.analyzingLoading && !aiStore.tailoringLoading && !aiStore.analysisResult && !aiStore.tailoringResult && !aiError"
            class="empty-state"
          >
            Paste a job description above to analyze how well this resume matches the role.
          </div>

          <!-- Empty state: analysis done, no tailoring yet -->
          <div
            v-if="aiStore.analysisResult && !aiStore.tailoringResult && !aiStore.tailoringLoading"
            class="ai-tailor-hint"
          >
            Analysis complete. Click <strong>Tailor Resume</strong> to generate bullet suggestions.
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { resumeApi } from '@/api/resume'
import type { ResumeVersion } from '@/api/resume'
import type { AcceptedSuggestion } from '@/api/ai'
import ExperienceSection from '@/components/resume/ExperienceSection.vue'
import EducationSection from '@/components/resume/EducationSection.vue'
import SkillsSection from '@/components/resume/SkillsSection.vue'
import JobDescriptionInput from '@/components/ai/JobDescriptionInput.vue'
import JobAnalysisResult from '@/components/ai/JobAnalysisResult.vue'
import TailoringSuggestions from '@/components/ai/TailoringSuggestions.vue'
import { useAiStore } from '@/stores/ai'

const route = useRoute()
const router = useRouter()

const resumeId = route.params.resumeId as string
const versionIdParam = route.query.version as string | undefined

const resumeName = ref('')
const currentVersion = ref<ResumeVersion | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)
const branching = ref(false)
const branchError = ref<string | null>(null)
const exporting = ref(false)
const exportError = ref<string | null>(null)
const exportLimitReached = ref(false)

const activeSection = ref<'summary' | 'experience' | 'education' | 'skills' | 'ai'>('summary')

const sections = [
  { key: 'summary' as const, label: 'Summary' },
  { key: 'experience' as const, label: 'Experience' },
  { key: 'education' as const, label: 'Education' },
  { key: 'skills' as const, label: 'Skills' },
]

const aiStore = useAiStore()
const aiError = computed(() => aiStore.error)

const acceptSuccess = ref(false)

const metaForm = reactive({ title: '', professionalSummary: '' })
const metaSaving = ref(false)
const metaError = ref<string | null>(null)
const metaSaved = ref(false)

function syncMetaForm(v: ResumeVersion) {
  metaForm.title = v.title ?? ''
  metaForm.professionalSummary = v.professionalSummary ?? ''
}

watch(currentVersion, (v) => { if (v) syncMetaForm(v) })

async function load() {
  loading.value = true
  loadError.value = null
  try {
    const resume = await resumeApi.getResume(resumeId)
    resumeName.value = resume.name
    const targetId = versionIdParam ?? resume.latestVersion.id
    const version = await resumeApi.getVersion(resumeId, targetId)
    currentVersion.value = version
  } catch (err) {
    loadError.value = resumeApi.extractError(err).message
  } finally {
    loading.value = false
  }
}

async function saveMeta() {
  if (!currentVersion.value) return
  metaSaving.value = true
  metaError.value = null
  metaSaved.value = false
  try {
    const updated = await resumeApi.updateVersionMeta(resumeId, currentVersion.value.id, {
      title: metaForm.title.trim() || null,
      professionalSummary: metaForm.professionalSummary.trim() || null,
    })
    currentVersion.value = { ...currentVersion.value, ...updated }
    metaSaved.value = true
    setTimeout(() => { metaSaved.value = false }, 3000)
  } catch (err) {
    metaError.value = resumeApi.extractError(err).message
  } finally {
    metaSaving.value = false
  }
}

async function createNewVersion() {
  branching.value = true
  branchError.value = null
  try {
    const newVersion = await resumeApi.createVersion(resumeId)
    currentVersion.value = newVersion
    activeSection.value = 'summary'
  } catch (err) {
    branchError.value = resumeApi.extractError(err).message
  } finally {
    branching.value = false
  }
}

function onVersionUpdated(updated: ResumeVersion) {
  currentVersion.value = updated
}

async function handleAcceptTailoring(accepted: AcceptedSuggestion[]) {
  if (!currentVersion.value) return
  const newVersion = await aiStore.acceptTailoring(resumeId, currentVersion.value.id, accepted)
  if (newVersion) {
    acceptSuccess.value = true
    setTimeout(async () => {
      acceptSuccess.value = false
      currentVersion.value = newVersion
      aiStore.clearResults()
      await router.push({ query: { version: newVersion.id } })
    }, 1500)
  }
}

async function handleAnalyze() {
  if (!currentVersion.value) return
  await aiStore.analyzeResume(resumeId, currentVersion.value.id)
}

async function handleTailor() {
  if (!currentVersion.value) return
  await aiStore.tailorResume(resumeId, currentVersion.value.id)
}

async function exportPdf() {
  if (!currentVersion.value) return
  exporting.value = true
  exportError.value = null
  exportLimitReached.value = false
  try {
    const buffer = await resumeApi.exportVersionPdf(resumeId, currentVersion.value.id)
    const blob = new Blob([buffer], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const safeName = (resumeName.value + (currentVersion.value.title ? ' - ' + currentVersion.value.title : ''))
      .replace(/[^a-zA-Z0-9 \-_]/g, '')
      .trim() || 'resume'
    a.download = safeName + '.pdf'
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    const apiErr = resumeApi.extractError(err)
    if (apiErr.code === 'PDF_EXPORT_LIMIT_EXCEEDED') {
      exportLimitReached.value = true
      exportError.value = apiErr.message
    } else {
      exportError.value = apiErr.message
    }
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.editor-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: calc(100vh - 64px);
}

.editor-sidebar {
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  padding: 1.5rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.back-link {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  text-decoration: none;
}

.back-link:hover {
  color: var(--color-primary);
}

.resume-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  word-break: break-word;
}

.version-box {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.875rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

.version-box-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.version-title-text {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  word-break: break-word;
}

.version-history-link {
  text-align: center;
  text-decoration: none;
}

.export-error {
  font-size: 0.75rem;
  color: var(--color-error-text);
  background: var(--color-error-bg);
  border: 1px solid #fecaca;
  border-radius: var(--radius);
  padding: 0.4rem 0.6rem;
  line-height: 1.4;
}

.export-limit-hint {
  display: block;
  margin-top: 0.2rem;
  color: var(--color-text-muted);
}

.section-nav {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  text-align: left;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.nav-item:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.nav-item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.nav-item.active {
  background: #ede9fe;
  color: var(--color-primary);
}

.editor-main {
  padding: 2rem;
  max-width: 860px;
}

.editor-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.skeleton-stack {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.nav-divider {
  height: 1px;
  background: var(--color-border);
  margin: 0.25rem 0;
}

.nav-item--ai {
  color: var(--color-primary);
}

.nav-item--ai:hover {
  background: #ede9fe;
  color: var(--color-primary);
}

.nav-item--ai.active {
  background: #ede9fe;
  color: var(--color-primary);
}

.ai-intro {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-bottom: 1rem;
  line-height: 1.5;
}

.ai-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text-muted);
  padding: 0.75rem 0;
}

.ai-tailor-hint {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  padding: 0.5rem 0;
}

@media (max-width: 768px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .editor-sidebar {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }
}
</style>
