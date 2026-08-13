import { defineStore } from 'pinia'
import { ref } from 'vue'
import { resumeApi, type ResumeSummary } from '@/api/resume'

export const useResumeStore = defineStore('resume', () => {
  const resumes = ref<ResumeSummary[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function loadResumes() {
    loading.value = true
    error.value = null
    try {
      resumes.value = await resumeApi.listResumes()
    } catch (err) {
      error.value = resumeApi.extractError(err).message
    } finally {
      loading.value = false
    }
  }

  async function removeResume(resumeId: string): Promise<void> {
    await resumeApi.deleteResume(resumeId)
    resumes.value = resumes.value.filter((r) => r.id !== resumeId)
  }

  async function renameResume(resumeId: string, name: string): Promise<void> {
    await resumeApi.updateResume(resumeId, { name })
    const idx = resumes.value.findIndex((r) => r.id === resumeId)
    if (idx !== -1) resumes.value[idx] = { ...resumes.value[idx], name } as ResumeSummary
  }

  return { resumes, loading, error, loadResumes, removeResume, renameResume }
})
