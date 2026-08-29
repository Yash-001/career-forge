import apiClient from './client'
import { extractApiError } from './errors'
export type { ApiError } from './errors'
import type { EmploymentType, ProficiencyLevel } from './profile'

// ── Response types ────────────────────────────────────────────────────────

export interface ResumeSummary {
  id: string
  name: string
  latestVersionNumber: number
  createdAt: string
  updatedAt: string
}

export interface ResumeVersionSummary {
  id: string
  versionNumber: number
  title: string | null
  isLatest: boolean
  createdAt: string
}

export interface ResumeResponse {
  id: string
  name: string
  latestVersion: ResumeVersionSummary
  createdAt: string
  updatedAt: string
}

export interface ResumeExperience {
  id: string
  companyName: string
  jobTitle: string
  location: string | null
  employmentType: EmploymentType | null
  startDate: string
  endDate: string | null
  currentlyWorking: boolean
  description: string | null
  displayOrder: number
}

export interface ResumeEducation {
  id: string
  institutionName: string
  degree: string | null
  fieldOfStudy: string | null
  location: string | null
  startDate: string | null
  endDate: string | null
  grade: string | null
  description: string | null
  displayOrder: number
}

export interface ResumeSkill {
  id: string
  name: string
  category: string | null
  proficiency: ProficiencyLevel | null
  displayOrder: number
}

export interface ResumeVersion {
  id: string
  versionNumber: number
  title: string | null
  professionalSummary: string | null
  isLatest: boolean
  experiences: ResumeExperience[]
  educations: ResumeEducation[]
  skills: ResumeSkill[]
  createdAt: string
}

// ── Request payload types ─────────────────────────────────────────────────

export interface CreateResumePayload {
  name: string
}

export interface UpdateResumePayload {
  name: string
}

export interface UpdateVersionMetaPayload {
  title?: string | null
  professionalSummary?: string | null
}

export interface ResumeExperiencePayload {
  companyName: string
  jobTitle: string
  location?: string | null
  employmentType?: EmploymentType | null
  startDate: string
  endDate?: string | null
  currentlyWorking: boolean
  description?: string | null
  displayOrder: number
}

export interface ResumeEducationPayload {
  institutionName: string
  degree?: string | null
  fieldOfStudy?: string | null
  location?: string | null
  startDate?: string | null
  endDate?: string | null
  grade?: string | null
  description?: string | null
  displayOrder: number
}

export interface ResumeSkillPayload {
  name: string
  category?: string | null
  proficiency?: ProficiencyLevel | null
  displayOrder: number
}

// ── API ───────────────────────────────────────────────────────────────────

export const resumeApi = {
  // Resumes
  async listResumes(): Promise<ResumeSummary[]> {
    const r = await apiClient.get<ResumeSummary[]>('/resumes')
    return r.data
  },

  async createResume(payload: CreateResumePayload): Promise<ResumeResponse> {
    const r = await apiClient.post<ResumeResponse>('/resumes', payload)
    return r.data
  },

  async getResume(resumeId: string): Promise<ResumeResponse> {
    const r = await apiClient.get<ResumeResponse>(`/resumes/${resumeId}`)
    return r.data
  },

  async updateResume(resumeId: string, payload: UpdateResumePayload): Promise<ResumeResponse> {
    const r = await apiClient.put<ResumeResponse>(`/resumes/${resumeId}`, payload)
    return r.data
  },

  async deleteResume(resumeId: string): Promise<void> {
    await apiClient.delete(`/resumes/${resumeId}`)
  },

  // Versions
  async listVersions(resumeId: string): Promise<ResumeVersionSummary[]> {
    const r = await apiClient.get<ResumeVersionSummary[]>(`/resumes/${resumeId}/versions`)
    return r.data
  },

  async getVersion(resumeId: string, versionId: string): Promise<ResumeVersion> {
    const r = await apiClient.get<ResumeVersion>(`/resumes/${resumeId}/versions/${versionId}`)
    return r.data
  },

  async createVersion(resumeId: string): Promise<ResumeVersion> {
    const r = await apiClient.post<ResumeVersion>(`/resumes/${resumeId}/versions`)
    return r.data
  },

  async updateVersionMeta(resumeId: string, versionId: string, payload: UpdateVersionMetaPayload): Promise<ResumeVersion> {
    const r = await apiClient.put<ResumeVersion>(`/resumes/${resumeId}/versions/${versionId}`, payload)
    return r.data
  },

  // Experiences
  async addExperience(resumeId: string, versionId: string, payload: ResumeExperiencePayload): Promise<ResumeExperience> {
    const r = await apiClient.post<ResumeExperience>(`/resumes/${resumeId}/versions/${versionId}/experiences`, payload)
    return r.data
  },

  async updateExperience(resumeId: string, versionId: string, expId: string, payload: ResumeExperiencePayload): Promise<ResumeExperience> {
    const r = await apiClient.put<ResumeExperience>(`/resumes/${resumeId}/versions/${versionId}/experiences/${expId}`, payload)
    return r.data
  },

  async deleteExperience(resumeId: string, versionId: string, expId: string): Promise<void> {
    await apiClient.delete(`/resumes/${resumeId}/versions/${versionId}/experiences/${expId}`)
  },

  // Education
  async addEducation(resumeId: string, versionId: string, payload: ResumeEducationPayload): Promise<ResumeEducation> {
    const r = await apiClient.post<ResumeEducation>(`/resumes/${resumeId}/versions/${versionId}/education`, payload)
    return r.data
  },

  async updateEducation(resumeId: string, versionId: string, eduId: string, payload: ResumeEducationPayload): Promise<ResumeEducation> {
    const r = await apiClient.put<ResumeEducation>(`/resumes/${resumeId}/versions/${versionId}/education/${eduId}`, payload)
    return r.data
  },

  async deleteEducation(resumeId: string, versionId: string, eduId: string): Promise<void> {
    await apiClient.delete(`/resumes/${resumeId}/versions/${versionId}/education/${eduId}`)
  },

  // Skills
  async addSkill(resumeId: string, versionId: string, payload: ResumeSkillPayload): Promise<ResumeSkill> {
    const r = await apiClient.post<ResumeSkill>(`/resumes/${resumeId}/versions/${versionId}/skills`, payload)
    return r.data
  },

  async updateSkill(resumeId: string, versionId: string, skillId: string, payload: ResumeSkillPayload): Promise<ResumeSkill> {
    const r = await apiClient.put<ResumeSkill>(`/resumes/${resumeId}/versions/${versionId}/skills/${skillId}`, payload)
    return r.data
  },

  async deleteSkill(resumeId: string, versionId: string, skillId: string): Promise<void> {
    await apiClient.delete(`/resumes/${resumeId}/versions/${versionId}/skills/${skillId}`)
  },

  // PDF Export
  async exportVersionPdf(resumeId: string, versionId: string): Promise<ArrayBuffer> {
    const r = await apiClient.get<ArrayBuffer>(
      `/resumes/${resumeId}/versions/${versionId}/pdf`,
      { responseType: 'arraybuffer' },
    )
    return r.data
  },

  extractError: extractApiError,
}
