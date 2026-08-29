import apiClient from './client'
import { extractApiError } from './errors'
export type { ApiError } from './errors'

// ── Response types ────────────────────────────────────────────────────────

export interface JobAnalysisResponse {
  detectedRole: string | null
  keywords: string[]
  technologies: string[]
  responsibilities: string[]
  matchedResumeSkills: string[]
  missingSkills: string[]
  providerName: string
}

export interface BulletSuggestion {
  experienceId: string
  originalText: string
  suggestedText: string
  matchedKeywords: string[]
  rationale: string
}

export interface TailoringResponse {
  suggestions: BulletSuggestion[]
  detectedKeywords: string[]
  providerName: string
}

export interface AcceptedSuggestion {
  experienceId: string
  suggestedText: string
}

export interface AcceptTailoringPayload {
  acceptedSuggestions: AcceptedSuggestion[]
}

// ── API ───────────────────────────────────────────────────────────────────

export const aiApi = {
  async analyzeResume(
    resumeId: string,
    versionId: string,
    jobDescription: string,
  ): Promise<JobAnalysisResponse> {
    const r = await apiClient.post<JobAnalysisResponse>(
      `/ai/resumes/${resumeId}/versions/${versionId}/analyze`,
      { jobDescription },
    )
    return r.data
  },

  async tailorResume(
    resumeId: string,
    versionId: string,
    jobDescription: string,
  ): Promise<TailoringResponse> {
    const r = await apiClient.post<TailoringResponse>(
      `/ai/resumes/${resumeId}/versions/${versionId}/tailor`,
      { jobDescription },
    )
    return r.data
  },

  async acceptTailoring(
    resumeId: string,
    versionId: string,
    payload: AcceptTailoringPayload,
  ): Promise<import('./resume').ResumeVersion> {
    const r = await apiClient.post<import('./resume').ResumeVersion>(
      `/ai/resumes/${resumeId}/versions/${versionId}/accept-tailoring`,
      payload,
    )
    return r.data
  },

  extractError: extractApiError,
}
