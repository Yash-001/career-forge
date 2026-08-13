import { defineStore } from 'pinia'
import { ref } from 'vue'
import { aiApi, type JobAnalysisResponse, type TailoringResponse, type AcceptedSuggestion } from '@/api/ai'
import type { ResumeVersion } from '@/api/resume'

export const useAiStore = defineStore('ai', () => {
  const jobDescription = ref('')
  const analysisResult = ref<JobAnalysisResponse | null>(null)
  const tailoringResult = ref<TailoringResponse | null>(null)
  const analyzingLoading = ref(false)
  const tailoringLoading = ref(false)
  const acceptingLoading = ref(false)
  const acceptedVersion = ref<ResumeVersion | null>(null)
  const error = ref<string | null>(null)

  async function analyzeResume(resumeId: string, versionId: string): Promise<void> {
    analyzingLoading.value = true
    error.value = null
    // Clear stale results when starting a new analysis
    analysisResult.value = null
    tailoringResult.value = null
    try {
      analysisResult.value = await aiApi.analyzeResume(resumeId, versionId, jobDescription.value)
    } catch (err) {
      error.value = aiApi.extractError(err).message
    } finally {
      analyzingLoading.value = false
    }
  }

  async function tailorResume(resumeId: string, versionId: string): Promise<void> {
    tailoringLoading.value = true
    error.value = null
    tailoringResult.value = null
    try {
      tailoringResult.value = await aiApi.tailorResume(resumeId, versionId, jobDescription.value)
    } catch (err) {
      error.value = aiApi.extractError(err).message
    } finally {
      tailoringLoading.value = false
    }
  }

  async function acceptTailoring(
    resumeId: string,
    versionId: string,
    acceptedSuggestions: AcceptedSuggestion[],
  ): Promise<ResumeVersion | null> {
    acceptingLoading.value = true
    error.value = null
    acceptedVersion.value = null
    try {
      acceptedVersion.value = await aiApi.acceptTailoring(resumeId, versionId, { acceptedSuggestions })
      return acceptedVersion.value
    } catch (err) {
      error.value = aiApi.extractError(err).message
      return null
    } finally {
      acceptingLoading.value = false
    }
  }

  function clearResults(): void {
    jobDescription.value = ''
    analysisResult.value = null
    tailoringResult.value = null
    acceptedVersion.value = null
    error.value = null
  }

  return {
    jobDescription,
    analysisResult,
    tailoringResult,
    analyzingLoading,
    tailoringLoading,
    acceptingLoading,
    acceptedVersion,
    error,
    analyzeResume,
    tailorResume,
    acceptTailoring,
    clearResults,
  }
})
