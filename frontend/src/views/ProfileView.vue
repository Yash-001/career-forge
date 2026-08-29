<template>
  <div class="profile-page">
    <header class="profile-header">
      <h1 class="profile-heading">My Profile</h1>
      <p class="profile-subheading">Keep your master profile up to date. It powers every resume you create.</p>
    </header>

    <!-- Loading skeleton -->
    <div v-if="store.loading" class="skeleton-wrap" aria-busy="true" aria-label="Loading profile">
      <div class="skeleton skeleton-block" />
      <div class="skeleton skeleton-block" />
      <div class="skeleton skeleton-block" />
    </div>

    <!-- Load error -->
    <div v-else-if="store.error" class="api-error" role="alert">
      {{ store.error }}
      <button class="btn btn-ghost btn-sm" style="margin-left:0.75rem" type="button" @click="store.loadAll()">Retry</button>
    </div>

    <template v-else>
      <!-- ── Personal Info ─────────────────────────────────────────────── -->
      <section class="card section-card" aria-labelledby="section-personal">
        <div class="card-header">
          <h2 class="card-title" id="section-personal">Personal Information</h2>
        </div>
        <form class="form" @submit.prevent="savePersonal" novalidate>
          <div class="form-row">
            <div class="field">
              <label for="p-title">Professional Title</label>
              <input id="p-title" v-model="personal.professionalTitle" type="text" placeholder="Senior Software Engineer" maxlength="255" />
            </div>
            <div class="field">
              <label for="p-phone">Phone</label>
              <input id="p-phone" v-model="personal.phone" type="tel" placeholder="+1 555 000 0000" maxlength="50" />
            </div>
          </div>
          <div class="field">
            <label for="p-location">Location</label>
            <input id="p-location" v-model="personal.location" type="text" placeholder="San Francisco, CA" maxlength="255" />
          </div>
          <div v-if="personalError" class="api-error" role="alert">{{ personalError }}</div>
          <div v-if="personalSaved" class="save-success" role="status">Personal information saved.</div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="personalSaving">
              <span v-if="personalSaving" class="spinner" aria-hidden="true" />
              {{ personalSaving ? 'Saving…' : 'Save' }}
            </button>
          </div>
        </form>
      </section>

      <!-- ── Professional Summary ─────────────────────────────────────── -->
      <section class="card section-card" aria-labelledby="section-summary">
        <div class="card-header">
          <h2 class="card-title" id="section-summary">Professional Summary</h2>
        </div>
        <form class="form" @submit.prevent="saveSummary" novalidate>
          <div class="field">
            <label for="p-summary">Summary</label>
            <textarea id="p-summary" v-model="summary.professionalSummary" rows="5" placeholder="A brief overview of your professional background and goals…" />
          </div>
          <div v-if="summaryError" class="api-error" role="alert">{{ summaryError }}</div>
          <div v-if="summarySaved" class="save-success" role="status">Summary saved.</div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="summarySaving">
              <span v-if="summarySaving" class="spinner" aria-hidden="true" />
              {{ summarySaving ? 'Saving…' : 'Save' }}
            </button>
          </div>
        </form>
      </section>

      <!-- ── Online Profiles ──────────────────────────────────────────── -->
      <section class="card section-card" aria-labelledby="section-online">
        <div class="card-header">
          <h2 class="card-title" id="section-online">Online Profiles</h2>
        </div>
        <form class="form" @submit.prevent="saveOnline" novalidate>
          <div class="field">
            <label for="p-linkedin">LinkedIn URL</label>
            <input id="p-linkedin" v-model="online.linkedinUrl" type="url" placeholder="https://linkedin.com/in/yourname" :class="{ error: onlineErrors.linkedinUrl }" />
            <span v-if="onlineErrors.linkedinUrl" class="field-error">{{ onlineErrors.linkedinUrl }}</span>
          </div>
          <div class="field">
            <label for="p-github">GitHub URL</label>
            <input id="p-github" v-model="online.githubUrl" type="url" placeholder="https://github.com/yourname" :class="{ error: onlineErrors.githubUrl }" />
            <span v-if="onlineErrors.githubUrl" class="field-error">{{ onlineErrors.githubUrl }}</span>
          </div>
          <div class="field">
            <label for="p-portfolio">Portfolio URL</label>
            <input id="p-portfolio" v-model="online.portfolioUrl" type="url" placeholder="https://yoursite.com" :class="{ error: onlineErrors.portfolioUrl }" />
            <span v-if="onlineErrors.portfolioUrl" class="field-error">{{ onlineErrors.portfolioUrl }}</span>
          </div>
          <div v-if="onlineError" class="api-error" role="alert">{{ onlineError }}</div>
          <div v-if="onlineSaved" class="save-success" role="status">Online profiles saved.</div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="onlineSaving">
              <span v-if="onlineSaving" class="spinner" aria-hidden="true" />
              {{ onlineSaving ? 'Saving…' : 'Save' }}
            </button>
          </div>
        </form>
      </section>

      <!-- ── Work Experience ──────────────────────────────────────────── -->
      <section class="card section-card" aria-labelledby="section-experience">
        <div class="card-header">
          <h2 class="card-title" id="section-experience">Work Experience</h2>
          <button class="btn btn-primary btn-sm" @click="openAddExperience" type="button">+ Add</button>
        </div>

        <div v-if="showExpForm" class="inline-form-wrap">
          <ExperienceForm
            :initial="editingExp"
            :saving="expSaving"
            :api-error="expFormError"
            :submit-label="editingExp ? 'Update' : 'Add Experience'"
            @submit="handleExpSubmit"
            @cancel="closeExpForm"
          />
        </div>

        <div v-if="store.experiences.length === 0 && !showExpForm" class="empty-state">
          No work experience added yet.
        </div>

        <ul v-else class="item-list" aria-label="Work experience entries">
          <li v-for="exp in store.experiences" :key="exp.id" class="item-card">
            <div class="item-main">
              <div class="item-title">{{ exp.jobTitle }} <span class="item-at">at</span> {{ exp.companyName }}</div>
              <div class="item-meta">
                <span v-if="exp.employmentType" class="badge badge-neutral">{{ formatEmploymentType(exp.employmentType) }}</span>
                <span class="item-dates">{{ formatDate(exp.startDate) }} — {{ exp.currentlyWorking ? 'Present' : formatDate(exp.endDate) }}</span>
                <span v-if="exp.location" class="item-location">· {{ exp.location }}</span>
              </div>
              <p v-if="exp.description" class="item-description">{{ exp.description }}</p>
            </div>
            <div class="item-actions">
              <button class="btn btn-ghost btn-sm" @click="openEditExperience(exp)" type="button" :aria-label="`Edit ${exp.jobTitle} at ${exp.companyName}`">Edit</button>
              <button class="btn btn-ghost btn-sm btn-delete" @click="confirmDeleteExp(exp)" type="button" :aria-label="`Delete ${exp.jobTitle} at ${exp.companyName}`">Delete</button>
            </div>
          </li>
        </ul>
      </section>

      <!-- ── Education ────────────────────────────────────────────────── -->
      <section class="card section-card" aria-labelledby="section-education">
        <div class="card-header">
          <h2 class="card-title" id="section-education">Education</h2>
          <button class="btn btn-primary btn-sm" @click="openAddEducation" type="button">+ Add</button>
        </div>

        <div v-if="showEduForm" class="inline-form-wrap">
          <EducationForm
            :initial="editingEdu"
            :saving="eduSaving"
            :api-error="eduFormError"
            :submit-label="editingEdu ? 'Update' : 'Add Education'"
            @submit="handleEduSubmit"
            @cancel="closeEduForm"
          />
        </div>

        <div v-if="store.educations.length === 0 && !showEduForm" class="empty-state">
          No education added yet.
        </div>

        <ul v-else class="item-list" aria-label="Education entries">
          <li v-for="edu in store.educations" :key="edu.id" class="item-card">
            <div class="item-main">
              <div class="item-title">{{ edu.institutionName }}</div>
              <div class="item-meta">
                <span v-if="edu.degree">{{ edu.degree }}<span v-if="edu.fieldOfStudy"> · {{ edu.fieldOfStudy }}</span></span>
                <span v-if="edu.startDate || edu.endDate" class="item-dates">{{ formatDate(edu.startDate) }} — {{ formatDate(edu.endDate) }}</span>
                <span v-if="edu.grade" class="badge badge-neutral">{{ edu.grade }}</span>
              </div>
              <p v-if="edu.description" class="item-description">{{ edu.description }}</p>
            </div>
            <div class="item-actions">
              <button class="btn btn-ghost btn-sm" @click="openEditEducation(edu)" type="button" :aria-label="`Edit ${edu.institutionName}`">Edit</button>
              <button class="btn btn-ghost btn-sm btn-delete" @click="confirmDeleteEdu(edu)" type="button" :aria-label="`Delete ${edu.institutionName}`">Delete</button>
            </div>
          </li>
        </ul>
      </section>

      <!-- ── Skills ───────────────────────────────────────────────────── -->
      <section class="card section-card" aria-labelledby="section-skills">
        <div class="card-header">
          <h2 class="card-title" id="section-skills">Skills</h2>
          <button class="btn btn-primary btn-sm" @click="openAddSkill" type="button">+ Add</button>
        </div>

        <div v-if="showSkillForm" class="inline-form-wrap">
          <SkillForm
            :initial="editingSkill"
            :saving="skillSaving"
            :api-error="skillFormError"
            :submit-label="editingSkill ? 'Update' : 'Add Skill'"
            @submit="handleSkillSubmit"
            @cancel="closeSkillForm"
          />
        </div>

        <div v-if="store.skills.length === 0 && !showSkillForm" class="empty-state">
          No skills added yet.
        </div>

        <ul v-else class="skill-list" aria-label="Skills">
          <li v-for="skill in store.skills" :key="skill.id" class="skill-item">
            <span class="skill-name">{{ skill.name }}</span>
            <span v-if="skill.proficiency" class="badge">{{ formatProficiency(skill.proficiency) }}</span>
            <span v-if="skill.category" class="badge badge-neutral">{{ skill.category }}</span>
            <div class="skill-actions">
              <button class="btn btn-ghost btn-sm" @click="openEditSkill(skill)" type="button" :aria-label="`Edit ${skill.name}`">Edit</button>
              <button class="btn btn-ghost btn-sm btn-delete" @click="confirmDeleteSkill(skill)" type="button" :aria-label="`Delete ${skill.name}`">Delete</button>
            </div>
          </li>
        </ul>
      </section>
    </template>

    <!-- Delete confirmation dialogs -->
    <ConfirmDialog
      :open="!!deleteTarget"
      :title="deleteTitle"
      :message="deleteMessage"
      :loading="deleteSaving"
      :error="deleteError"
      @confirm="executeDelete"
      @cancel="cancelDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useProfileStore } from '@/stores/profile'
import { profileApi } from '@/api/profile'
import type { WorkExperience, Education, Skill, WorkExperiencePayload, EducationPayload, SkillPayload } from '@/api/profile'
import ExperienceForm from '@/components/profile/ExperienceForm.vue'
import EducationForm from '@/components/profile/EducationForm.vue'
import SkillForm from '@/components/profile/SkillForm.vue'
import ConfirmDialog from '@/components/profile/ConfirmDialog.vue'

const store = useProfileStore()

onMounted(() => store.loadAll())

// ── Personal info ──────────────────────────────────────────────────────────

const personal = reactive({
  professionalTitle: '',
  phone: '',
  location: '',
})
const personalSaving = ref(false)
const personalError = ref<string | null>(null)
const personalSaved = ref(false)

onMounted(() => {
  if (store.profile) syncPersonal()
})

function syncPersonal() {
  personal.professionalTitle = store.profile?.professionalTitle ?? ''
  personal.phone = store.profile?.phone ?? ''
  personal.location = store.profile?.location ?? ''
}

async function savePersonal() {
  personalSaving.value = true
  personalError.value = null
  personalSaved.value = false
  try {
    await store.saveProfile({
      professionalTitle: personal.professionalTitle || undefined,
      phone: personal.phone || undefined,
      location: personal.location || undefined,
      professionalSummary: store.profile?.professionalSummary ?? undefined,
      linkedinUrl: store.profile?.linkedinUrl ?? undefined,
      githubUrl: store.profile?.githubUrl ?? undefined,
      portfolioUrl: store.profile?.portfolioUrl ?? undefined,
    })
    personalSaved.value = true
    setTimeout(() => { personalSaved.value = false }, 3000)
  } catch (err) {
    personalError.value = profileApi.extractError(err).message
  } finally {
    personalSaving.value = false
  }
}

// ── Summary ────────────────────────────────────────────────────────────────

const summary = reactive({ professionalSummary: '' })
const summarySaving = ref(false)
const summaryError = ref<string | null>(null)
const summarySaved = ref(false)

onMounted(() => {
  summary.professionalSummary = store.profile?.professionalSummary ?? ''
})

async function saveSummary() {
  summarySaving.value = true
  summaryError.value = null
  summarySaved.value = false
  try {
    await store.saveProfile({
      professionalTitle: store.profile?.professionalTitle ?? undefined,
      phone: store.profile?.phone ?? undefined,
      location: store.profile?.location ?? undefined,
      professionalSummary: summary.professionalSummary || undefined,
      linkedinUrl: store.profile?.linkedinUrl ?? undefined,
      githubUrl: store.profile?.githubUrl ?? undefined,
      portfolioUrl: store.profile?.portfolioUrl ?? undefined,
    })
    summarySaved.value = true
    setTimeout(() => { summarySaved.value = false }, 3000)
  } catch (err) {
    summaryError.value = profileApi.extractError(err).message
  } finally {
    summarySaving.value = false
  }
}

// ── Online profiles ────────────────────────────────────────────────────────

const online = reactive({ linkedinUrl: '', githubUrl: '', portfolioUrl: '' })
const onlineErrors = reactive<Record<string, string>>({})
const onlineSaving = ref(false)
const onlineError = ref<string | null>(null)
const onlineSaved = ref(false)

onMounted(() => {
  online.linkedinUrl = store.profile?.linkedinUrl ?? ''
  online.githubUrl = store.profile?.githubUrl ?? ''
  online.portfolioUrl = store.profile?.portfolioUrl ?? ''
})

function validateUrl(val: string): boolean {
  if (!val) return true
  try { new URL(val); return true } catch { return false }
}

async function saveOnline() {
  Object.keys(onlineErrors).forEach((k) => delete onlineErrors[k])
  if (!validateUrl(online.linkedinUrl)) onlineErrors.linkedinUrl = 'Must be a valid URL.'
  if (!validateUrl(online.githubUrl)) onlineErrors.githubUrl = 'Must be a valid URL.'
  if (!validateUrl(online.portfolioUrl)) onlineErrors.portfolioUrl = 'Must be a valid URL.'
  if (Object.keys(onlineErrors).length > 0) return

  onlineSaving.value = true
  onlineError.value = null
  onlineSaved.value = false
  try {
    await store.saveProfile({
      professionalTitle: store.profile?.professionalTitle ?? undefined,
      phone: store.profile?.phone ?? undefined,
      location: store.profile?.location ?? undefined,
      professionalSummary: store.profile?.professionalSummary ?? undefined,
      linkedinUrl: online.linkedinUrl || undefined,
      githubUrl: online.githubUrl || undefined,
      portfolioUrl: online.portfolioUrl || undefined,
    })
    onlineSaved.value = true
    setTimeout(() => { onlineSaved.value = false }, 3000)
  } catch (err) {
    const e = profileApi.extractError(err)
    if (e.fieldErrors) Object.assign(onlineErrors, e.fieldErrors)
    else onlineError.value = e.message
  } finally {
    onlineSaving.value = false
  }
}

// ── Work Experience ────────────────────────────────────────────────────────

const showExpForm = ref(false)
const editingExp = ref<WorkExperience | null>(null)
const expSaving = ref(false)
const expFormError = ref<string | null>(null)

function openAddExperience() {
  editingExp.value = null
  expFormError.value = null
  showExpForm.value = true
}

function openEditExperience(exp: WorkExperience) {
  editingExp.value = exp
  expFormError.value = null
  showExpForm.value = true
}

function closeExpForm() {
  showExpForm.value = false
  editingExp.value = null
  expFormError.value = null
}

async function handleExpSubmit(payload: WorkExperiencePayload) {
  expSaving.value = true
  expFormError.value = null
  try {
    if (editingExp.value) {
      await store.updateExperience(editingExp.value.id, payload)
    } else {
      await store.addExperience(payload)
    }
    closeExpForm()
  } catch (err) {
    expFormError.value = profileApi.extractError(err).message
  } finally {
    expSaving.value = false
  }
}

// ── Education ──────────────────────────────────────────────────────────────

const showEduForm = ref(false)
const editingEdu = ref<Education | null>(null)
const eduSaving = ref(false)
const eduFormError = ref<string | null>(null)

function openAddEducation() {
  editingEdu.value = null
  eduFormError.value = null
  showEduForm.value = true
}

function openEditEducation(edu: Education) {
  editingEdu.value = edu
  eduFormError.value = null
  showEduForm.value = true
}

function closeEduForm() {
  showEduForm.value = false
  editingEdu.value = null
  eduFormError.value = null
}

async function handleEduSubmit(payload: EducationPayload) {
  eduSaving.value = true
  eduFormError.value = null
  try {
    if (editingEdu.value) {
      await store.updateEducation(editingEdu.value.id, payload)
    } else {
      await store.addEducation(payload)
    }
    closeEduForm()
  } catch (err) {
    eduFormError.value = profileApi.extractError(err).message
  } finally {
    eduSaving.value = false
  }
}

// ── Skills ─────────────────────────────────────────────────────────────────

const showSkillForm = ref(false)
const editingSkill = ref<Skill | null>(null)
const skillSaving = ref(false)
const skillFormError = ref<string | null>(null)

function openAddSkill() {
  editingSkill.value = null
  skillFormError.value = null
  showSkillForm.value = true
}

function openEditSkill(skill: Skill) {
  editingSkill.value = skill
  skillFormError.value = null
  showSkillForm.value = true
}

function closeSkillForm() {
  showSkillForm.value = false
  editingSkill.value = null
  skillFormError.value = null
}

async function handleSkillSubmit(payload: SkillPayload) {
  skillSaving.value = true
  skillFormError.value = null
  try {
    if (editingSkill.value) {
      await store.updateSkill(editingSkill.value.id, payload)
    } else {
      await store.addSkill(payload)
    }
    closeSkillForm()
  } catch (err) {
    skillFormError.value = profileApi.extractError(err).message
  } finally {
    skillSaving.value = false
  }
}

// ── Delete confirmation ────────────────────────────────────────────────────

type DeleteTarget =
  | { type: 'experience'; item: WorkExperience }
  | { type: 'education'; item: Education }
  | { type: 'skill'; item: Skill }

const deleteTarget = ref<DeleteTarget | null>(null)
const deleteSaving = ref(false)
const deleteError = ref<string | null>(null)

const deleteTitle = ref('')
const deleteMessage = ref('')

function confirmDeleteExp(exp: WorkExperience) {
  deleteTarget.value = { type: 'experience', item: exp }
  deleteTitle.value = 'Delete Work Experience'
  deleteMessage.value = `Remove "${exp.jobTitle} at ${exp.companyName}"? This cannot be undone.`
  deleteError.value = null
}

function confirmDeleteEdu(edu: Education) {
  deleteTarget.value = { type: 'education', item: edu }
  deleteTitle.value = 'Delete Education'
  deleteMessage.value = `Remove "${edu.institutionName}"? This cannot be undone.`
  deleteError.value = null
}

function confirmDeleteSkill(skill: Skill) {
  deleteTarget.value = { type: 'skill', item: skill }
  deleteTitle.value = 'Delete Skill'
  deleteMessage.value = `Remove "${skill.name}"? This cannot be undone.`
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
    const t = deleteTarget.value
    if (t.type === 'experience') await store.removeExperience(t.item.id)
    else if (t.type === 'education') await store.removeEducation(t.item.id)
    else await store.removeSkill(t.item.id)
    deleteTarget.value = null
  } catch (err) {
    deleteError.value = profileApi.extractError(err).message
  } finally {
    deleteSaving.value = false
  }
}

// ── Formatters ─────────────────────────────────────────────────────────────

function formatDate(d: string | null | undefined): string {
  if (!d) return '—'
  const [y, m] = d.split('-')
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
  return `${months[parseInt(m ?? '1') - 1]} ${y}`
}

function formatEmploymentType(t: string): string {
  return t.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}

function formatProficiency(p: string): string {
  return p.charAt(0) + p.slice(1).toLowerCase()
}
</script>

<style scoped>
.profile-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profile-header {
  margin-bottom: 0.5rem;
}

.profile-heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.profile-subheading {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
}

.section-card {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.skeleton-block {
  height: 140px;
  border-radius: var(--radius-lg);
}

.inline-form-wrap {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 1.25rem;
  margin-bottom: 1rem;
  background: var(--color-bg);
}

/* Item list (experience / education) */
.item-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.item-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--color-text);
}

.item-at {
  font-weight: 400;
  color: var(--color-text-muted);
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  margin-top: 0.25rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.item-dates {
  font-size: 0.8125rem;
}

.item-location {
  font-size: 0.8125rem;
}

.item-description {
  margin-top: 0.4rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  white-space: pre-wrap;
}

.item-actions {
  display: flex;
  gap: 0.25rem;
  flex-shrink: 0;
}

.btn-delete {
  color: var(--color-danger);
}

.btn-delete:hover:not(:disabled) {
  color: var(--color-danger-hover);
  border-color: var(--color-danger);
}

/* Skills */
.skill-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.skill-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.skill-name {
  font-weight: 500;
  font-size: 0.875rem;
  flex: 1;
  min-width: 80px;
}

.skill-actions {
  display: flex;
  gap: 0.25rem;
  margin-left: auto;
}

@media (max-width: 480px) {
  .item-card {
    flex-direction: column;
  }
  .item-actions {
    align-self: flex-end;
  }
}
</style>
